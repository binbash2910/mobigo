package com.binbash.mobigo.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import com.binbash.mobigo.config.ApplicationProperties;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.service.dto.CniVerificationDTO;
import com.binbash.mobigo.service.dto.MrzData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for verifying identity documents (CNI, passport, carte de sejour) via OCR.
 *
 * Supports Cameroon and France documents.
 * Flow: store images -> OCR (MRZ-optimized) -> parse MRZ -> compare with profile -> update entity.
 */
@Service
@Transactional
public class CniVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(CniVerificationService.class);

    // All known MRZ prefixes for the first-pass OCR detection
    private static final String[] KNOWN_MRZ_PREFIXES = { "IDCMR", "IDFRA", "IRFRA", "I<CMR", "I<FRA", "P<CMR", "P<FRA", "POCMR", "POFRA" };

    private final ApplicationProperties applicationProperties;
    private final PeopleRepository peopleRepository;
    private final CniMrzParserService mrzParserService;
    private final FileStorageService fileStorageService;

    /** Singleton Anthropic client — lazy-initialized on first Vision call, reused across requests. */
    private volatile AnthropicClient anthropicClient;

    public CniVerificationService(
        ApplicationProperties applicationProperties,
        PeopleRepository peopleRepository,
        CniMrzParserService mrzParserService,
        FileStorageService fileStorageService
    ) {
        this.applicationProperties = applicationProperties;
        this.peopleRepository = peopleRepository;
        this.mrzParserService = mrzParserService;
        this.fileStorageService = fileStorageService;
    }

    @PreDestroy
    void closeAnthropicClient() {
        if (anthropicClient != null) {
            try {
                anthropicClient.close();
            } catch (Exception e) {
                LOG.debug("Error closing Anthropic client: {}", e.getMessage());
            }
        }
    }

    private AnthropicClient getOrCreateAnthropicClient(ApplicationProperties.Anthropic config) {
        if (anthropicClient == null) {
            synchronized (this) {
                if (anthropicClient == null) {
                    anthropicClient = AnthropicOkHttpClient.builder().apiKey(config.getApiKey()).build();
                    LOG.info("Anthropic client initialized (singleton)");
                }
            }
        }
        return anthropicClient;
    }

    /**
     * Verify an identity document by processing uploaded photos.
     *
     * @param peopleId     the ID of the People entity
     * @param rectoFile    the front photo (required)
     * @param versoFile    the back photo (optional for passports)
     * @param documentType the user-selected document type (e.g. "CNI_CMR", "PASSPORT_FRA")
     */
    public CniVerificationDTO verifyCni(Long peopleId, MultipartFile rectoFile, MultipartFile versoFile, String documentType) {
        People people = peopleRepository
            .findById(peopleId)
            .orElseThrow(() -> new IllegalArgumentException("People not found with id: " + peopleId));

        // 1. Store images
        String rectoPath = storeImage(peopleId, rectoFile, "recto");
        String versoPath = null;
        if (versoFile != null && !versoFile.isEmpty()) {
            versoPath = storeImage(peopleId, versoFile, "verso");
        }

        // 2. Run OCR - order depends on document type
        boolean isPassport = documentType != null && documentType.startsWith("PASSPORT");
        String primaryPath;
        String fallbackPath;

        if (isPassport) {
            // Passport: MRZ is on the identity page (recto)
            primaryPath = rectoPath;
            fallbackPath = versoPath;
        } else {
            // CNI / Carte de sejour: MRZ is on the back (verso)
            primaryPath = versoPath != null ? versoPath : rectoPath;
            fallbackPath = versoPath != null ? rectoPath : null;
        }

        // 2b. OCR + parse: try each strategy until a valid MRZ is found
        LOG.info("Starting CNI verification for people {} — primary image: {}", peopleId, primaryPath);
        MrzData mrzData = ocrAndParse(primaryPath);

        // 3. If MRZ dates unreadable but names/doc OK, try visual text extraction
        if (!mrzData.isValid() && mrzData.getNom() != null) {
            LOG.info("MRZ dates unreadable — attempting visual text extraction for people {}", peopleId);
            supplementWithVisualText(mrzData, primaryPath, fallbackPath);
        }

        // 4. If still not valid, try full MRZ on fallback side
        if (!mrzData.isValid() && fallbackPath != null) {
            LOG.info("Trying fallback image: {}", fallbackPath);
            MrzData fallbackMrz = ocrAndParse(fallbackPath);
            if (fallbackMrz.isValid()) {
                mrzData = fallbackMrz;
            } else if (fallbackMrz.getNom() != null) {
                supplementWithVisualText(fallbackMrz, fallbackPath, primaryPath);
                if (fallbackMrz.isValid()) mrzData = fallbackMrz;
            }
        }

        // 4b. AI Vision — si MRZ a échoué ou est incomplet
        if (!mrzData.isValid() || isIncomplete(mrzData)) {
            LOG.info("MRZ insufficient — attempting AI vision extraction for people {}", peopleId);
            MrzData visionData = extractFromVision(rectoPath, versoPath);
            if (visionData.isValid()) {
                if (mrzData.isValid()) {
                    // MRZ a des champs partiels — Vision comble les lacunes
                    mergeVisualData(mrzData, visionData);
                } else {
                    mrzData = visionData;
                }
            }
        }

        // 5. Build result
        CniVerificationDTO result = new CniVerificationDTO();

        if (!mrzData.isValid()) {
            result.setVerified(false);
            result.setStatus("REJECTED");
            result.setMessage("Impossible de lire la zone MRZ du document. Assurez-vous que la photo est nette et bien eclairee.");
            updatePeopleStatus(people, "REJECTED", rectoPath, versoPath, null, documentType);
            return result;
        }

        // Override documentType if user selected RESIDENCE_PERMIT but MRZ parser detected CNI
        if (documentType != null && documentType.startsWith("RESIDENCE_PERMIT") && "CNI".equals(mrzData.getDocumentType())) {
            mrzData.setDocumentType("RESIDENCE_PERMIT");
        }

        // 6. Compare extracted data with profile
        boolean nomMatch = compareNames(mrzData.getNom(), people.getNom());
        boolean prenomMatch = compareNames(mrzData.getPrenom(), people.getPrenom());
        boolean dobMatch = mrzData.getDateNaissance() != null && mrzData.getDateNaissance().equals(people.getDateNaissance());
        boolean expired = mrzData.getDateExpiration() != null && mrzData.getDateExpiration().isBefore(LocalDate.now());

        // Populate result DTO
        result.setDocumentNumber(mrzData.getDocumentNumber());
        result.setNom(mrzData.getNom());
        result.setPrenom(mrzData.getPrenom());
        result.setDateNaissance(mrzData.getDateNaissance());
        result.setDateExpiration(mrzData.getDateExpiration());
        result.setSexe(mrzData.getSexe());
        result.setMrzFormat(mrzData.getFormat());
        result.setDocumentType(mrzData.getDocumentType());
        result.setIssuingCountry(mrzData.getIssuingCountry());
        result.setNomMatch(nomMatch);
        result.setPrenomMatch(prenomMatch);
        result.setDateNaissanceMatch(dobMatch);
        result.setDocumentExpired(expired);

        // 7. Determine verification status
        String status;
        if (expired) {
            status = "EXPIRED";
            result.setVerified(false);
            result.setMessage("Le document est expire. Veuillez utiliser un document valide.");
        } else if (nomMatch && prenomMatch && dobMatch) {
            status = "VERIFIED";
            result.setVerified(true);
            result.setMessage("Identite verifiee avec succes.");
        } else {
            status = "REJECTED";
            result.setVerified(false);
            result.setMessage("Les informations extraites ne correspondent pas a votre profil.");
        }
        result.setStatus(status);

        // 8. Update People entity
        updatePeopleStatus(people, status, rectoPath, versoPath, mrzData, documentType);

        return result;
    }

    /**
     * Store an uploaded document image to the filesystem.
     * Returns the absolute filesystem path (used for OCR processing).
     */
    private String storeImage(Long peopleId, MultipartFile file, String side) {
        try {
            return fileStorageService.storeCniImage(peopleId, file, side);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store " + side + " image: " + e.getMessage(), e);
        }
    }

    /** Maximum width for OCR processing — larger images are downscaled to save memory. */
    private static final int MAX_OCR_WIDTH = 3500;

    /**
     * Run OCR with multiple strategies and parse MRZ, returning the first valid result.
     * Each strategy's OCR output is immediately parsed — only a valid MRZ (with parseable dates)
     * is accepted. This prevents garbled full-image OCR from short-circuiting better crop results.
     *
     * Strategy order: bottom crops first (isolated MRZ zone), then sharpened crops,
     * then full image as fallback. Tracks the best partial result (names OK, dates failed)
     * for the text extraction fallback in the caller.
     */
    private MrzData ocrAndParse(String imagePath) {
        BufferedImage originalImage = null;
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(applicationProperties.getTesseract().getDataPath());
            tesseract.setLanguage(applicationProperties.getTesseract().getLanguage());

            BufferedImage rawImage;
            if (imagePath.toLowerCase().endsWith(".pdf")) {
                rawImage = convertPdfToImage(imagePath);
            } else {
                rawImage = ImageIO.read(Path.of(imagePath).toFile());
            }
            if (rawImage == null) {
                LOG.error("Could not read image at: {}", imagePath);
                return invalidMrzData();
            }

            // Downscale large images to limit memory usage (300MB heap)
            originalImage = limitSize(rawImage, MAX_OCR_WIDTH);
            if (originalImage != rawImage) {
                rawImage.flush(); // release the large original
            }
            LOG.info("OCR image size after limit: {}x{}", originalImage.getWidth(), originalImage.getHeight());

            double[] cropRatios = { 0.35, 0.25, 0.20, 0.15 };
            MrzData bestPartial = null;

            // --- Phase 1: Bottom crops with preprocessing, restricted charset (best for MRZ) ---
            for (double ratio : cropRatios) {
                MrzData result = tryOcrStrategy(
                    tesseract,
                    originalImage,
                    ratio,
                    true,
                    true,
                    String.format("bottom %d%% crop, preprocessed, restricted charset", (int) (ratio * 100))
                );
                if (result.isValid()) return result;
                if (bestPartial == null && result.getNom() != null) bestPartial = result;
            }

            // --- Phase 2: Bottom crops with preprocessing, full charset ---
            for (double ratio : cropRatios) {
                MrzData result = tryOcrStrategy(
                    tesseract,
                    originalImage,
                    ratio,
                    true,
                    false,
                    String.format("bottom %d%% crop, preprocessed, full charset", (int) (ratio * 100))
                );
                if (result.isValid()) return result;
                if (bestPartial == null && result.getNom() != null) bestPartial = result;
            }

            // --- Phase 3: Raw bottom crops (no preprocessing), restricted charset ---
            for (double ratio : new double[] { 0.35, 0.25 }) {
                MrzData result = tryOcrStrategy(
                    tesseract,
                    originalImage,
                    ratio,
                    false,
                    true,
                    String.format("bottom %d%% raw crop, restricted charset", (int) (ratio * 100))
                );
                if (result.isValid()) return result;
                if (bestPartial == null && result.getNom() != null) bestPartial = result;
            }

            // --- Phase 4: Sharpened bottom crops (sharpen the crop, not the full image) ---
            for (double ratio : new double[] { 0.35, 0.25 }) {
                BufferedImage crop = cropBottom(originalImage, ratio);
                BufferedImage sharpenedCrop = sharpen(crop);
                crop.flush();
                MrzData result = tryOcrStrategy(
                    tesseract,
                    sharpenedCrop,
                    0,
                    true,
                    true,
                    String.format("bottom %d%% sharpened crop, restricted charset", (int) (ratio * 100))
                );
                sharpenedCrop.flush();
                if (result.isValid()) return result;
                if (bestPartial == null && result.getNom() != null) bestPartial = result;
            }

            // --- Phase 5: Full image, restricted charset ---
            {
                MrzData result = tryOcrStrategy(tesseract, originalImage, 0, true, true, "full image, preprocessed, restricted charset");
                if (result.isValid()) return result;
                if (bestPartial == null && result.getNom() != null) bestPartial = result;
            }

            // --- Phase 6: Full image, full charset ---
            {
                MrzData result = tryOcrStrategy(tesseract, originalImage, 0, true, false, "full image, preprocessed, full charset");
                if (result.isValid()) return result;
                if (bestPartial == null && result.getNom() != null) bestPartial = result;
            }

            LOG.warn("No valid MRZ found after all strategies for: {}", imagePath);

            if (bestPartial != null) {
                LOG.info(
                    "Returning partial MRZ (nom={}, doc={}, dates failed) for text extraction",
                    bestPartial.getNom(),
                    bestPartial.getDocumentNumber()
                );
                return bestPartial;
            }

            return invalidMrzData();
        } catch (TesseractException | IOException e) {
            LOG.error("OCR failed for image {}: {}", imagePath, e.getMessage());
            return invalidMrzData();
        } finally {
            if (originalImage != null) {
                originalImage.flush();
            }
        }
    }

    /**
     * Try a single OCR strategy: crop (or full image), optionally preprocess, OCR, then parse MRZ.
     *
     * @param cropRatio 0 = full image, > 0 = bottom crop ratio
     * @param preprocess true = apply Otsu binarization + contrast stretching
     * @param restricted true = restricted MRZ charset (PSM 6), false = full charset (PSM 3)
     */
    private MrzData tryOcrStrategy(
        Tesseract tesseract,
        BufferedImage originalImage,
        double cropRatio,
        boolean preprocess,
        boolean restricted,
        String strategyName
    ) throws TesseractException {
        // Track intermediate images to flush after OCR
        BufferedImage cropped = null;
        BufferedImage preprocessed = null;
        BufferedImage scaled = null;

        try {
            BufferedImage image;
            if (cropRatio > 0) {
                cropped = cropBottom(originalImage, cropRatio);
                image = cropped;
            } else {
                image = originalImage;
            }

            if (preprocess) {
                preprocessed = preprocessForMrz(image);
                image = preprocessed;
            }

            if (image.getHeight() < 300) {
                scaled = scaleUp(image, 300.0 / image.getHeight());
                image = scaled;
            }

            String ocrText = ocrWithCharset(tesseract, image, restricted);
            MrzData mrzData = mrzParserService.parseMrz(ocrText);

            if (mrzData.isValid()) {
                LOG.info(
                    "SUCCESS via strategy [{}] — doc={}, nom={}, dob={}",
                    strategyName,
                    mrzData.getDocumentNumber(),
                    mrzData.getNom(),
                    mrzData.getDateNaissance()
                );
            } else {
                String preview = ocrText == null ? "null" : ocrText.replace("\n", " | ").trim();
                if (preview.length() > 120) preview = preview.substring(0, 120) + "...";
                LOG.info("FAIL via strategy [{}] — OCR: {}", strategyName, preview);
            }

            return mrzData;
        } finally {
            // Flush intermediate images to release native memory immediately
            if (scaled != null) scaled.flush();
            if (preprocessed != null) preprocessed.flush();
            if (cropped != null) cropped.flush();
        }
    }

    private MrzData invalidMrzData() {
        MrzData data = new MrzData();
        data.setValid(false);
        return data;
    }

    /**
     * Convert the first page of a PDF file to a BufferedImage for OCR processing.
     *
     * @param pdfPath absolute path to the PDF file
     * @return BufferedImage of the first page, or null if conversion fails
     */
    private BufferedImage convertPdfToImage(String pdfPath) {
        try (PDDocument document = Loader.loadPDF(Path.of(pdfPath).toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            // Render first page at 300 DPI for good OCR quality
            return renderer.renderImageWithDPI(0, 300);
        } catch (IOException e) {
            LOG.error("Failed to convert PDF to image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Run OCR on an image with either restricted MRZ charset or full charset.
     */
    private String ocrWithCharset(Tesseract tesseract, BufferedImage image, boolean restricted) throws TesseractException {
        if (restricted) {
            tesseract.setPageSegMode(6); // Single uniform block
            tesseract.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<");
        } else {
            tesseract.setPageSegMode(3); // Fully automatic segmentation
            tesseract.setVariable("tessedit_char_whitelist", "");
        }
        return tesseract.doOCR(image);
    }

    /**
     * Crop the bottom portion of an image (where MRZ is located).
     * Returns an independent copy (not a subimage view) so the parent can be GC'd separately.
     */
    private BufferedImage cropBottom(BufferedImage image, double ratio) {
        int cropHeight = (int) (image.getHeight() * ratio);
        int y = image.getHeight() - cropHeight;
        int w = image.getWidth();
        // Copy pixel data to independent BufferedImage (getSubimage shares parent's data array)
        int[] pixels = image.getRGB(0, y, w, cropHeight, null, 0, w);
        BufferedImage crop = new BufferedImage(w, cropHeight, BufferedImage.TYPE_INT_RGB);
        crop.setRGB(0, 0, w, cropHeight, pixels, 0, w);
        return crop;
    }

    /**
     * Scale up an image by the given factor for better OCR on small text.
     */
    private BufferedImage scaleUp(BufferedImage image, double factor) {
        int newWidth = (int) (image.getWidth() * factor);
        int newHeight = (int) (image.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    /**
     * Downscale an image if its width exceeds maxWidth, preserving aspect ratio.
     * Uses high-quality rendering hints to preserve MRZ text detail.
     * Returns the original image unchanged if already within limits.
     */
    private BufferedImage limitSize(BufferedImage image, int maxWidth) {
        if (image.getWidth() <= maxWidth) {
            return image;
        }
        double factor = (double) maxWidth / image.getWidth();
        int newHeight = (int) (image.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, maxWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    /**
     * Apply a 3x3 sharpening kernel to enhance edge contrast.
     * Helps Tesseract distinguish digits from patterned card backgrounds.
     */
    private BufferedImage sharpen(BufferedImage image) {
        // Ensure compatible image type for ConvolveOp
        BufferedImage compatible = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics g = compatible.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        float[] kernel = { 0, -1, 0, -1, 5, -1, 0, -1, 0 };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage result = op.filter(compatible, null);
        compatible.flush(); // release intermediate copy
        return result;
    }

    /**
     * Supplement partial MRZ data (where names/doc parsed but dates failed) with dates
     * extracted from the human-readable text visible on the card photos.
     * Looks for DD.MM.YYYY patterns, sex indicators, etc.
     */
    private void supplementWithVisualText(MrzData partial, String... imagePaths) {
        try {
            // Flexible date patterns: DD.MM.YYYY, DD/MM/YYYY, DD-MM-YYYY, DD MM YYYY
            // Also handle OCR artifacts like extra spaces around separators
            Pattern datePattern = Pattern.compile("(\\d{2})\\s*[./\\-,;:\\s]\\s*(\\d{2})\\s*[./\\-,;:\\s]\\s*(\\d{4})");
            LocalDate now = LocalDate.now();

            // Collect ALL dates from ALL images first, then pick best DOB/expiry
            List<LocalDate> allDates = new ArrayList<>();
            String combinedText = "";

            for (String path : imagePaths) {
                if (path == null) continue;
                String text = ocrAllVariants(path);
                combinedText += " " + text;

                Matcher matcher = datePattern.matcher(text);
                while (matcher.find()) {
                    try {
                        int dd = Integer.parseInt(matcher.group(1));
                        int mm = Integer.parseInt(matcher.group(2));
                        int yyyy = Integer.parseInt(matcher.group(3));
                        if (mm < 1 || mm > 12 || dd < 1 || dd > 31 || yyyy < 1900 || yyyy > 2100) continue;
                        LocalDate d = LocalDate.of(yyyy, mm, dd);
                        allDates.add(d);
                        LOG.info("Found date in visual text: {} (raw: '{}')", d, matcher.group());
                    } catch (Exception e) {
                        // skip invalid date
                    }
                }
            }

            LOG.info("Total dates found across all images: {}", allDates.size());

            // DOB = oldest date (furthest in the past, before now - 5 years)
            // Expiry = newest date (furthest in the future, after now)
            for (LocalDate d : allDates) {
                if (d.isBefore(now.minusYears(5))) {
                    if (partial.getDateNaissance() == null || d.isBefore(partial.getDateNaissance())) {
                        partial.setDateNaissance(d);
                    }
                } else if (d.isAfter(now)) {
                    if (partial.getDateExpiration() == null || d.isAfter(partial.getDateExpiration())) {
                        partial.setDateExpiration(d);
                    }
                }
            }

            if (partial.getDateNaissance() != null) LOG.info("Extracted DOB: {}", partial.getDateNaissance());
            if (partial.getDateExpiration() != null) LOG.info("Extracted expiry: {}", partial.getDateExpiration());
            if (partial.getDateExpiration() == null) LOG.warn("Could NOT extract expiry date from visual text — all dates: {}", allDates);

            // Extract sex near "SEX" label
            if (partial.getSexe() == null) {
                Pattern sexPattern = Pattern.compile("SEX[E]?[\\s/|:]*([MF])\\b", Pattern.CASE_INSENSITIVE);
                Matcher sexMatcher = sexPattern.matcher(combinedText);
                if (sexMatcher.find()) {
                    partial.setSexe(sexMatcher.group(1).toUpperCase());
                    LOG.info("Extracted sex from visual text: {}", partial.getSexe());
                }
            }

            // Re-validate: names from MRZ + dates from visual text
            partial.setValid(partial.getNom() != null && partial.getDateNaissance() != null);
            if (partial.isValid()) {
                LOG.info(
                    "Visual text extraction SUCCESS — DOB={}, expiry={}, sex={}",
                    partial.getDateNaissance(),
                    partial.getDateExpiration(),
                    partial.getSexe()
                );
            }
        } catch (TesseractException | IOException e) {
            LOG.error("Visual text extraction failed: {}", e.getMessage());
        }
    }

    /**
     * Run full-page OCR on an image using multiple variants (raw, preprocessed, sharpened)
     * and return the concatenated text from all variants.
     * Shared logic between supplementWithVisualText() and extractFromVisualText().
     */
    private String ocrAllVariants(String imagePath) throws IOException, TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(applicationProperties.getTesseract().getDataPath());
        tesseract.setLanguage(applicationProperties.getTesseract().getLanguage());
        tesseract.setPageSegMode(3); // Fully automatic segmentation
        tesseract.setVariable("tessedit_char_whitelist", ""); // Full charset

        BufferedImage img;
        if (imagePath.toLowerCase().endsWith(".pdf")) {
            img = convertPdfToImage(imagePath);
        } else {
            img = ImageIO.read(Path.of(imagePath).toFile());
        }
        if (img == null) {
            LOG.error("Could not read image at: {}", imagePath);
            return "";
        }

        // Downscale to limit memory usage, flush original if a new image was created
        BufferedImage limited = limitSize(img, MAX_OCR_WIDTH);
        if (limited != img) {
            img.flush();
            img = limited;
        }

        StringBuilder combined = new StringBuilder();

        // Try raw + preprocessed + sharpened variants for maximum text extraction
        BufferedImage preprocessed = preprocessForMrz(img);
        BufferedImage sharpened = sharpen(img);
        BufferedImage[] variants = { img, preprocessed, sharpened };
        String[] variantNames = { "raw", "preprocessed", "sharpened" };

        for (int v = 0; v < variants.length; v++) {
            String text = tesseract.doOCR(variants[v]);
            String preview = text.length() > 500 ? text.substring(0, 500) + "..." : text;
            LOG.info("Visual text [{}] from {} ({} chars):\n{}", variantNames[v], imagePath, text.length(), preview);
            combined.append(" ").append(text);
        }

        // Flush all variant images to release native memory
        sharpened.flush();
        preprocessed.flush();
        img.flush();

        return combined.toString();
    }

    /**
     * Extract identity data from visual text on card photos when MRZ is absent (old-format CNI).
     * Runs full-page OCR on both sides and uses label-based regex patterns to extract fields.
     *
     * @param rectoPath path to front image
     * @param versoPath path to back image (may be null)
     * @return MrzData with format="VISUAL", valid if at least nom + dateNaissance extracted
     */
    private MrzData extractFromVisualText(String rectoPath, String versoPath) {
        try {
            String rectoText = ocrAllVariants(rectoPath);
            String versoText = versoPath != null ? ocrAllVariants(versoPath) : "";
            return parseVisualText(rectoText, versoText);
        } catch (TesseractException | IOException e) {
            LOG.error("Visual text extraction failed: {}", e.getMessage());
            return invalidMrzData();
        }
    }

    /**
     * Parse identity fields from OCR text using multiple extraction strategies.
     *
     * Strategy 1: Label-based inline regex (e.g. "NOM / SURNAME: MIMBE")
     * Strategy 2: Line-by-line keyword search (label on one line, value on next — common on old-format CNI)
     * Strategy 3: Label-independent date extraction (find all DD.MM.YYYY, assign by heuristic)
     *
     * Package-private for unit testing.
     *
     * @param rectoText OCR text from the front of the card
     * @param versoText OCR text from the back of the card
     * @return MrzData with format="VISUAL", valid if at least nom + dateNaissance extracted
     */
    MrzData parseVisualText(String rectoText, String versoText) {
        MrzData data = new MrzData();
        data.setFormat("VISUAL");
        data.setDocumentType("CNI");
        data.setIssuingCountry("CMR");

        String bothText = rectoText + "\n" + versoText;

        // ========== PASS 1: Label-based inline regex (label + value on SAME LINE) ==========
        // Use [ \t]* (not \s*) between label and value to prevent cross-line matching.
        // Use literal space (not \s) in value group to avoid matching across newlines.
        // Negative lookbehind prevents "NOM" matching inside "PRÉNOMS".

        // --- NOM / SURNAME ---
        Pattern nomPattern = Pattern.compile(
            "(?<![A-Za-zÀ-Üà-ü])(?:NOM[ \t]*(?:[/|I][ \t]*SURNAME)?|SURNAME)[ \t]*:?[ \t]+([A-ZÀ-Ü][A-ZÀ-Ü \\-]{1,})",
            Pattern.CASE_INSENSITIVE
        );
        String nom = findFirst(nomPattern, rectoText);
        if (nom == null) nom = findFirst(nomPattern, bothText);
        nom = isPlausibleName(nom) ? cleanNameValue(nom) : null;
        if (nom != null) data.setNom(nom);

        // --- PRENOMS / GIVEN NAMES ---
        Pattern prenomPattern = Pattern.compile(
            "(?:PR[EÉ]NOMS?[ \t]*(?:[/|I][ \t]*GIVEN[ \t]*NAMES?)?|GIVEN[ \t]*NAMES?)[ \t]*:?[ \t]+([A-ZÀ-Ü][A-ZÀ-Ü \\-]{1,})",
            Pattern.CASE_INSENSITIVE
        );
        String prenom = findFirst(prenomPattern, rectoText);
        if (prenom == null) prenom = findFirst(prenomPattern, bothText);
        prenom = isPlausibleName(prenom) ? cleanNameValue(prenom) : null;
        if (prenom != null) data.setPrenom(prenom);

        // --- DATE DE NAISSANCE / DATE OF BIRTH (inline, same line) ---
        Pattern dobPattern = Pattern.compile(
            "(?:DATE[ \t]*DE[ \t]*NAISSANCE|DATE[ \t]*OF[ \t]*BIRTH|N[EÉ][E(]?[)]?[ \t]*LE)[ \t]*[:/]?[ \t]*(\\d{2}\\s*[./\\-\\s]\\s*\\d{2}\\s*[./\\-\\s]\\s*\\d{4})",
            Pattern.CASE_INSENSITIVE
        );
        String dobStr = findFirst(dobPattern, rectoText);
        if (dobStr == null) dobStr = findFirst(dobPattern, bothText);
        if (dobStr != null) {
            LocalDate dob = parseDateDMY(dobStr);
            if (dob != null) data.setDateNaissance(dob);
        }

        // --- SEXE / SEX ---
        Pattern sexPattern = Pattern.compile("(?:SEXE?)[ \t]*[:/]?[ \t]*([MF])\\b", Pattern.CASE_INSENSITIVE);
        String sexe = findFirst(sexPattern, rectoText);
        if (sexe == null) sexe = findFirst(sexPattern, bothText);
        if (sexe != null) data.setSexe(sexe.toUpperCase());

        // --- DATE D'EXPIRATION / DATE OF EXPIRY (inline, same line) ---
        Pattern expiryPattern = Pattern.compile(
            "(?:DATE[ \t]*D[''']?EXPIRATION|DATE[ \t]*OF[ \t]*EXPIRY|EXPIRE[ \t]*LE)[ \t]*[:/]?[ \t]*(\\d{2}\\s*[./\\-\\s]\\s*\\d{2}\\s*[./\\-\\s]\\s*\\d{4})",
            Pattern.CASE_INSENSITIVE
        );
        String expiryStr = findFirst(expiryPattern, versoText);
        if (expiryStr == null) expiryStr = findFirst(expiryPattern, bothText);
        if (expiryStr != null) {
            LocalDate expiry = parseDateDMY(expiryStr);
            if (expiry != null) data.setDateExpiration(expiry);
        }

        // --- IDENTIFIANT UNIQUE / NIC ---
        Pattern nicPattern = Pattern.compile(
            "(?:IDENTIFIANT[ \t]*UNIQUE|UNIQUE[ \t]*IDENTIFIER|N[°o]?[ \t]*CNI)[ \t]*[:/]?[ \t]*([A-Z0-9]{5,})",
            Pattern.CASE_INSENSITIVE
        );
        String nic = findFirst(nicPattern, versoText);
        if (nic == null) nic = findFirst(nicPattern, bothText);
        if (nic != null) data.setDocumentNumber(nic.toUpperCase());

        // ========== PASS 2: Line-by-line keyword search ==========
        // On old-format CNI, labels and values are on separate lines.
        // The OCR may garble labels but keywords like NOM, PRENOM, NAISSANCE partially survive.

        if (data.getNom() == null) {
            String lineNom = extractValueAfterKeywordLine(bothText, "NOM", "SURNAM");
            lineNom = cleanNameValue(lineNom);
            if (lineNom != null) data.setNom(lineNom);
        }

        if (data.getPrenom() == null) {
            String linePrenom = extractValueAfterKeywordLine(bothText, "PRENOM", "GIVEN");
            linePrenom = cleanNameValue(linePrenom);
            if (linePrenom != null) data.setPrenom(linePrenom);
        }

        if (data.getDateNaissance() == null) {
            String lineDob = extractDateAfterKeywordLine(bothText, "NAISSANCE", "AISSANCE", "BIRTH");
            if (lineDob != null) {
                LocalDate dob = parseDateDMY(lineDob);
                if (dob != null) data.setDateNaissance(dob);
            }
        }

        if (data.getDateExpiration() == null) {
            String lineExpiry = extractDateAfterKeywordLine(bothText, "EXPIR", "EXPI");
            if (lineExpiry != null) {
                LocalDate expiry = parseDateDMY(lineExpiry);
                if (expiry != null) data.setDateExpiration(expiry);
            }
        }

        // ========== PASS 3: Label-independent date extraction ==========
        // Find ALL DD.MM.YYYY patterns and assign by heuristic:
        // DOB = oldest date (before now - 5 years), Expiry = newest date (after now)
        if (data.getDateNaissance() == null || data.getDateExpiration() == null) {
            extractDatesByHeuristic(data, bothText);
        }

        // ========== PASS 4: Positional prenom extraction ==========
        // On old-format CNI, the layout is always: NOM → PRÉNOMS → DATE DE NAISSANCE.
        // If NOM and a date are found but PRENOM is missing, look for a name-like value
        // between the NOM value and the first date in the text.
        if (data.getPrenom() == null && data.getNom() != null) {
            String positionalPrenom = extractPrenomByPosition(bothText, data.getNom());
            positionalPrenom = cleanNameValue(positionalPrenom);
            if (positionalPrenom != null) data.setPrenom(positionalPrenom);
        }

        // Valid if at least nom + dateNaissance are extracted
        data.setValid(data.getNom() != null && data.getDateNaissance() != null);

        LOG.info(
            "parseVisualText result — valid={}, nom={}, prenom={}, dob={}, expiry={}, sex={}, nic={}",
            data.isValid(),
            data.getNom(),
            data.getPrenom(),
            data.getDateNaissance(),
            data.getDateExpiration(),
            data.getSexe(),
            data.getDocumentNumber()
        );

        return data;
    }

    /**
     * Check if a captured text looks like a real name value (not a label fragment).
     * Rejects values containing common label keywords that indicate OCR captured
     * part of the bilingual label instead of the actual value.
     */
    private boolean isPlausibleName(String text) {
        if (text == null) return false;
        String upper = text.toUpperCase().trim();
        if (upper.length() < 2) return false;
        String[] labelWords = {
            "SURNAME",
            "GIVEN",
            "NAMES",
            "BIRTH",
            "NAISSANCE",
            "DATE",
            "EXPIR",
            "IDENTITY",
            "CAMEROUN",
            "CAMEROON",
            "NATIONAL",
            "REPUBLIC",
        };
        for (String lw : labelWords) {
            if (upper.contains(lw)) return false;
        }
        return true;
    }

    /** Common French/African name particles (2 chars) that are NOT OCR noise. */
    private static final java.util.Set<String> NAME_PARTICLES = java.util.Set.of("DE", "DI", "DU", "DA", "EL", "AL", "LE", "LA", "EP");

    /**
     * Clean an extracted name value by removing trailing OCR noise fragments.
     * Keeps real name words (>= 3 chars) and known particles (DE, DI, etc.).
     * Stops when hitting short fragments (1-2 chars) after a real name word,
     * as these are typically OCR artifacts from card design elements.
     *
     * Examples: "ETONGO SR LEE" → "ETONGO", "LUCIEN YANNICK" → "LUCIEN YANNICK",
     *           "KAMENI EPSE MIMBE" → "KAMENI EPSE MIMBE", "DE LA FONTAINE" → "DE LA FONTAINE"
     */
    private String cleanNameValue(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] words = raw.toUpperCase().trim().split("\\s+");

        List<String> result = new ArrayList<>();
        boolean hadRealWord = false;

        for (String word : words) {
            boolean isReal = word.length() >= 3;
            boolean isParticle = NAME_PARTICLES.contains(word);

            if (isReal || isParticle) {
                result.add(word);
                if (isReal) hadRealWord = true;
            } else if (hadRealWord) {
                // Short non-particle word after a real name → likely OCR noise, stop
                break;
            }
            // Skip leading single-char noise (rare, but possible from OCR)
        }

        return result.isEmpty() ? null : String.join(" ", result);
    }

    /**
     * Check if a text line contains a keyword, with accent normalization and word-boundary
     * checking for short keywords (≤4 chars like "NOM") to avoid false matches
     * inside longer words (e.g. "NOM" inside "PRÉNOMS").
     */
    private boolean lineContainsKeyword(String line, String keyword) {
        String normalized = Normalizer.normalize(line, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase();
        String kw = keyword.toUpperCase();
        int idx = normalized.indexOf(kw);
        while (idx >= 0) {
            // For short keywords (≤4 chars), require that preceding char is not a letter
            if (kw.length() > 4 || idx == 0 || !Character.isLetter(normalized.charAt(idx - 1))) {
                return true;
            }
            idx = normalized.indexOf(kw, idx + 1);
        }
        return false;
    }

    /**
     * Find a line containing any of the given keywords, then return the next non-empty line
     * as a cleaned name value (uppercase letters, spaces, hyphens only).
     * Handles old-format CNI where labels and values are on separate lines.
     */
    private String extractValueAfterKeywordLine(String text, String... keywords) {
        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length - 1; i++) {
            for (String keyword : keywords) {
                if (lineContainsKeyword(lines[i], keyword)) {
                    // Found a label line — look for the next non-empty line with name-like content
                    for (int j = i + 1; j < Math.min(i + 3, lines.length); j++) {
                        String valueLine = lines[j].trim();
                        if (valueLine.isEmpty()) continue;
                        // Clean: keep only uppercase letters, accented chars, spaces, hyphens
                        String cleaned = valueLine.replaceAll("[^A-ZÀ-Üa-zà-ü \\-]", "").trim().toUpperCase();
                        // Must be a plausible name (2+ chars, not a label keyword itself)
                        if (cleaned.length() >= 2 && isPlausibleName(cleaned) && !looksLikeLabel(cleaned)) {
                            return cleaned;
                        }
                    }
                    break; // Found the keyword line but no valid value — don't keep searching
                }
            }
        }
        return null;
    }

    /**
     * Check if a text line looks like a label rather than a value.
     * Labels typically contain multiple keywords from the bilingual format.
     */
    private boolean looksLikeLabel(String text) {
        int labelWords = 0;
        String[] labelKeywords = {
            "NOM",
            "SURNAME",
            "PRENOM",
            "GIVEN",
            "NAME",
            "DATE",
            "NAISSANCE",
            "BIRTH",
            "SEXE",
            "SEX",
            "TAILLE",
            "HEIGHT",
            "LIEU",
            "PLACE",
            "PROFESSION",
            "OCCUPATION",
        };
        for (String kw : labelKeywords) {
            if (text.contains(kw)) labelWords++;
        }
        return labelWords >= 2;
    }

    /**
     * Find a line containing any of the given keywords, then extract a DD.MM.YYYY date
     * from the same line or next lines.
     */
    private String extractDateAfterKeywordLine(String text, String... keywords) {
        Pattern datePattern = Pattern.compile("(\\d{2}\\s*[./\\-]\\s*\\d{2}\\s*[./\\-]\\s*\\d{4})");
        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            for (String keyword : keywords) {
                if (lineContainsKeyword(lines[i], keyword)) {
                    // Look for date on this line or the next few lines
                    for (int j = i; j < Math.min(i + 3, lines.length); j++) {
                        Matcher m = datePattern.matcher(lines[j]);
                        if (m.find()) return m.group(1);
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Extract all DD.MM.YYYY dates from text and assign DOB/expiry by heuristic:
     * DOB = oldest date before (now - 5 years), Expiry = newest date after now.
     */
    private void extractDatesByHeuristic(MrzData data, String text) {
        Pattern datePattern = Pattern.compile("(\\d{2})\\s*[./\\-,;:\\s]\\s*(\\d{2})\\s*[./\\-,;:\\s]\\s*(\\d{4})");
        LocalDate now = LocalDate.now();
        List<LocalDate> allDates = new ArrayList<>();

        Matcher matcher = datePattern.matcher(text);
        while (matcher.find()) {
            try {
                int dd = Integer.parseInt(matcher.group(1));
                int mm = Integer.parseInt(matcher.group(2));
                int yyyy = Integer.parseInt(matcher.group(3));
                if (mm < 1 || mm > 12 || dd < 1 || dd > 31 || yyyy < 1900 || yyyy > 2100) continue;
                allDates.add(LocalDate.of(yyyy, mm, dd));
            } catch (Exception e) {
                // skip invalid date
            }
        }

        for (LocalDate d : allDates) {
            if (data.getDateNaissance() == null && d.isBefore(now.minusYears(5))) {
                data.setDateNaissance(d);
            } else if (data.getDateExpiration() == null && d.isAfter(now)) {
                data.setDateExpiration(d);
            }
        }
    }

    /**
     * Positional prenom extraction: on old-format CNI, the layout is NOM → PRÉNOMS → DOB.
     * Find the line containing the NOM value, then look for name-like lines after it
     * (before the first date). The first plausible name that differs from NOM is the prenom.
     */
    private String extractPrenomByPosition(String text, String nomValue) {
        Pattern datePattern = Pattern.compile("\\d{2}\\s*[./\\-]\\s*\\d{2}\\s*[./\\-]\\s*\\d{4}");
        String[] lines = text.split("\\n");
        boolean foundNom = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Stop at the first date line after NOM
            if (foundNom && datePattern.matcher(trimmed).find()) {
                break;
            }

            // Look for the NOM value line
            if (!foundNom && trimmed.toUpperCase().contains(nomValue.toUpperCase())) {
                foundNom = true;
                continue;
            }

            // After NOM, look for a name-like line
            if (foundNom) {
                String cleaned = trimmed.replaceAll("[^A-ZÀ-Üa-zà-ü \\-]", "").trim().toUpperCase();
                if (
                    cleaned.length() >= 2 && isPlausibleName(cleaned) && !looksLikeLabel(cleaned) && !cleaned.equals(nomValue.toUpperCase())
                ) {
                    // Apply cleanNameValue to filter OCR noise; skip if nothing survives
                    String cleanedName = cleanNameValue(cleaned);
                    if (cleanedName != null) {
                        return cleanedName;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find the first match of a pattern's group(1) in the given text.
     */
    private String findFirst(Pattern pattern, String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Parse a DD.MM.YYYY or DD/MM/YYYY date string (with possible spaces around separators).
     */
    private LocalDate parseDateDMY(String dateStr) {
        // Normalize: replace any separator (dot, slash, dash, space) with dot, then split
        String normalized = dateStr.trim().replaceAll("[./\\-\\s]+", ".");
        String[] parts = normalized.split("\\.");
        if (parts.length != 3) return null;
        try {
            int dd = Integer.parseInt(parts[0]);
            int mm = Integer.parseInt(parts[1]);
            int yyyy = Integer.parseInt(parts[2]);
            if (mm < 1 || mm > 12 || dd < 1 || dd > 31 || yyyy < 1900 || yyyy > 2100) return null;
            return LocalDate.of(yyyy, mm, dd);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pre-process image for better OCR results using contrast stretching + Otsu's binarization.
     * Converts to grayscale, enhances contrast, then applies Otsu's automatic thresholding
     * to produce a clean black-and-white image optimal for MRZ reading.
     *
     * Uses bit manipulation instead of new Color() per pixel to reduce GC pressure.
     */
    private BufferedImage preprocessForMrz(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Read all pixels in one batch (much faster than per-pixel getRGB)
        int[] pixels = original.getRGB(0, 0, width, height, null, 0, width);

        // Step 1: Convert to grayscale values using bit shifts (no Color objects)
        int[] grayValues = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            grayValues[i] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        }
        pixels = null; // release source pixels

        // Step 1.5: Contrast stretching — normalize gray range to full 0-255
        int minGray = 255, maxGray = 0;
        for (int v : grayValues) {
            if (v < minGray) minGray = v;
            if (v > maxGray) maxGray = v;
        }
        int grayRange = maxGray - minGray;
        if (grayRange > 20 && grayRange < 230) {
            for (int i = 0; i < grayValues.length; i++) {
                grayValues[i] = Math.min(255, Math.max(0, (int) (((grayValues[i] - minGray) * 255.0) / grayRange)));
            }
        }

        // Step 2: Compute Otsu's optimal threshold
        int threshold = computeOtsuThreshold(grayValues);

        // Step 3: Apply threshold to create binary image
        // Pre-compute black/white RGB values (avoids new Color() per pixel)
        int blackRgb = 0xFF000000; // opaque black
        int whiteRgb = 0xFFFFFFFF; // opaque white
        int[] binaryPixels = new int[grayValues.length];
        for (int i = 0; i < grayValues.length; i++) {
            binaryPixels[i] = grayValues[i] > threshold ? whiteRgb : blackRgb;
        }
        grayValues = null; // release

        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        binary.setRGB(0, 0, width, height, binaryPixels, 0, width);

        return binary;
    }

    /**
     * Compute optimal threshold using Otsu's method.
     * Maximizes inter-class variance between foreground (text) and background.
     */
    private int computeOtsuThreshold(int[] grayValues) {
        int[] histogram = new int[256];
        for (int value : grayValues) {
            histogram[value]++;
        }

        int total = grayValues.length;
        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        float sumB = 0;
        int wB = 0;
        float maxVariance = 0;
        int bestThreshold = 128;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;
            int wF = total - wB;
            if (wF == 0) break;

            sumB += t * histogram[t];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float variance = (float) wB * wF * (mB - mF) * (mB - mF);
            if (variance > maxVariance) {
                maxVariance = variance;
                bestThreshold = t;
            }
        }

        return bestThreshold;
    }

    /**
     * Compare two name strings with normalization (remove accents, uppercase, trim).
     * Uses fuzzy matching (Levenshtein distance) to tolerate minor OCR errors.
     */
    private boolean compareNames(String mrzName, String profileName) {
        if (mrzName == null || profileName == null) {
            return false;
        }
        String normalizedMrz = normalizeName(mrzName);
        String normalizedProfile = normalizeName(profileName);

        // Exact match
        if (normalizedMrz.equals(normalizedProfile)) {
            return true;
        }

        // MRZ may contain multiple first names, profile may have only one
        // Check if profile name is contained in MRZ name
        if (normalizedMrz.contains(normalizedProfile) || normalizedProfile.contains(normalizedMrz)) {
            return true;
        }

        // Compact comparison: remove spaces/dashes and compare full strings
        // Handles concatenated names from misread '<' separators (e.g. "CYRILLESANICET" vs "CYRILLE ANICET")
        String mrzCompact = normalizedMrz.replace(" ", "").replace("-", "");
        String profileCompact = normalizedProfile.replace(" ", "").replace("-", "");
        if (mrzCompact.length() >= 4 && profileCompact.length() >= 4 && levenshtein(mrzCompact, profileCompact) <= 2) {
            return true;
        }

        // Check if any word in MRZ matches any word in profile (exact or fuzzy)
        String[] mrzParts = normalizedMrz.split("\\s+");
        String[] profileParts = normalizedProfile.split("\\s+");
        for (String mp : mrzParts) {
            for (String pp : profileParts) {
                if (mp.isEmpty() || pp.isEmpty()) continue;
                if (mp.equals(pp)) return true;
                // Fuzzy match: tolerate up to 2 OCR errors for words >= 4 chars
                if (mp.length() >= 4 && pp.length() >= 4 && levenshtein(mp, pp) <= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Compute Levenshtein edit distance between two strings.
     */
    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    /**
     * Normalize a name: remove accents, uppercase, trim whitespace.
     */
    private String normalizeName(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", ""); // Remove diacritical marks
        return normalized.toUpperCase().trim();
    }

    /**
     * Update People entity with verification results.
     */
    private void updatePeopleStatus(
        People people,
        String status,
        String rectoPath,
        String versoPath,
        MrzData mrzData,
        String userDocumentType
    ) {
        people.setCniStatut(status);
        people.setCniPhotoRecto(rectoPath);
        people.setCniPhotoVerso(versoPath);
        people.setCniVerifieAt(Instant.now());

        if (mrzData != null && mrzData.isValid()) {
            people.setCniNumero(mrzData.getDocumentNumber());
            people.setCniDateExpiration(mrzData.getDateExpiration());
            people.setCniSexe(mrzData.getSexe());
            people.setCniNomMrz(mrzData.getNom());
            people.setCniPrenomMrz(mrzData.getPrenom());
            people.setCniDateNaissanceMrz(mrzData.getDateNaissance());

            // Store document type: prefer user selection, fallback to MRZ-detected
            if (userDocumentType != null) {
                people.setDocumentType(userDocumentType);
            } else if (mrzData.getDocumentType() != null && mrzData.getIssuingCountry() != null) {
                people.setDocumentType(mrzData.getDocumentType() + "_" + mrzData.getIssuingCountry());
            }

            // Update legacy cni field for backward compatibility
            if ("VERIFIED".equals(status) && mrzData.getDocumentNumber() != null) {
                people.setCni(mrzData.getDocumentNumber());
            }
        }

        peopleRepository.save(people);
    }

    /**
     * Check if extracted data is incomplete: valid but missing key fields
     * (prenom, dateExpiration, or documentNumber).
     * Package-private for unit testing.
     */
    boolean isIncomplete(MrzData data) {
        if (!data.isValid()) return false;
        return data.getPrenom() == null || data.getDateExpiration() == null || data.getDocumentNumber() == null;
    }

    /**
     * Merge AI vision data into existing partial data: copy non-null fields from source
     * to target only when target has a null field.
     * Package-private for unit testing.
     */
    void mergeVisualData(MrzData target, MrzData source) {
        if (target.getNom() == null && source.getNom() != null) target.setNom(source.getNom());
        if (target.getPrenom() == null && source.getPrenom() != null) target.setPrenom(source.getPrenom());
        if (target.getDateNaissance() == null && source.getDateNaissance() != null) target.setDateNaissance(source.getDateNaissance());
        if (target.getDateExpiration() == null && source.getDateExpiration() != null) target.setDateExpiration(source.getDateExpiration());
        if (target.getDocumentNumber() == null && source.getDocumentNumber() != null) target.setDocumentNumber(source.getDocumentNumber());
        if (target.getSexe() == null && source.getSexe() != null) target.setSexe(source.getSexe());
    }

    private static final String VISION_PROMPT =
        "Tu reçois 2 images d'une carte d'identité nationale (CNI) : la première est le RECTO (face avant), la seconde est le VERSO (face arrière).\n\n" +
        "FORMATS POSSIBLES :\n" +
        "- CNI camerounaise ancienne (v1) : pas de zone MRZ, texte imprimé avec étiquettes comme NOM/NAME, PRENOM/SURNAME, DATE DE NAISSANCE/DATE OF BIRTH, SEXE/SEX.\n" +
        "- CNI camerounaise récente (v2) : avec zone MRZ en bas du verso.\n" +
        "- CNI française : format carte bancaire, MRZ au verso.\n\n" +
        "OÙ TROUVER LES CHAMPS :\n" +
        "- RECTO : nom de famille, prénom(s), date de naissance, sexe, parfois numéro de document.\n" +
        "- VERSO : date d'expiration (ou date de validité), numéro de document (si pas au recto).\n\n" +
        "INSTRUCTIONS :\n" +
        "- Lis le TEXTE IMPRIMÉ visible sur les images. Ne devine pas, ne fabrique pas de données.\n" +
        "- Pour le nom et prénom, lis la valeur APRÈS l'étiquette (ex: après \"NOM/NAME :\" ou \"SURNAME :\").\n" +
        "- Les dates sont au format DD.MM.YYYY (ex: 16.04.1999).\n" +
        "- Le sexe est M ou F.\n\n" +
        "Retourne UNIQUEMENT un JSON (sans markdown, sans explication) :\n" +
        "{\"nom\":\"...\",\"prenom\":\"...\",\"dateNaissance\":\"DD.MM.YYYY\",\"sexe\":\"M/F\"," +
        "\"dateExpiration\":\"DD.MM.YYYY\",\"documentNumber\":\"...\"}\n" +
        "Mets null pour les champs non lisibles.";

    /**
     * Extract identity fields from card images using Claude Vision API.
     * Only called when Tesseract-based extraction failed or produced incomplete results.
     */
    private MrzData extractFromVision(String rectoPath, String versoPath) {
        ApplicationProperties.Anthropic config = applicationProperties.getAnthropic();
        if (!config.isEnabled() || config.getApiKey() == null || config.getApiKey().isBlank()) {
            LOG.info("AI vision extraction skipped — Anthropic API not configured");
            return invalidMrzData();
        }

        try {
            List<ContentBlockParam> contentBlocks = new ArrayList<>();

            // Add recto label + image
            contentBlocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text("RECTO (face avant de la carte) :").build()));
            ContentBlockParam rectoBlock = buildImageBlock(rectoPath);
            if (rectoBlock == null) return invalidMrzData();
            contentBlocks.add(rectoBlock);

            // Add verso label + image if available
            if (versoPath != null) {
                contentBlocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text("VERSO (face arrière de la carte) :").build()));
                ContentBlockParam versoBlock = buildImageBlock(versoPath);
                if (versoBlock != null) {
                    contentBlocks.add(versoBlock);
                }
            }

            // Add text prompt
            contentBlocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(VISION_PROMPT).build()));

            AnthropicClient client = getOrCreateAnthropicClient(config);

            MessageCreateParams params = MessageCreateParams.builder()
                .model(config.getModel())
                .maxTokens(512L)
                .addUserMessageOfBlockParams(contentBlocks)
                .build();

            Message message = client.messages().create(params);

            // Extract text from response
            StringBuilder responseText = new StringBuilder();
            message.content().stream().flatMap(block -> block.text().stream()).forEach(textBlock -> responseText.append(textBlock.text()));

            String jsonResponse = responseText.toString().trim();
            LOG.info("AI vision response: {}", jsonResponse);

            return parseVisionJson(jsonResponse);
        } catch (Exception e) {
            LOG.error("AI vision extraction failed: {}", e.getMessage());
            return invalidMrzData();
        }
    }

    /**
     * Build a base64 image content block for the Claude API from a file path.
     */
    private ContentBlockParam buildImageBlock(String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            Base64ImageSource.MediaType mediaType;
            String lower = imagePath.toLowerCase();
            if (lower.endsWith(".png")) {
                mediaType = Base64ImageSource.MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".gif")) {
                mediaType = Base64ImageSource.MediaType.IMAGE_GIF;
            } else if (lower.endsWith(".webp")) {
                mediaType = Base64ImageSource.MediaType.IMAGE_WEBP;
            } else {
                mediaType = Base64ImageSource.MediaType.IMAGE_JPEG;
            }

            return ContentBlockParam.ofImage(
                ImageBlockParam.builder().source(Base64ImageSource.builder().mediaType(mediaType).data(base64).build()).build()
            );
        } catch (IOException e) {
            LOG.error("Failed to read image for vision: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse the JSON response from Claude Vision into MrzData.
     * Package-private for unit testing.
     */
    MrzData parseVisionJson(String json) {
        MrzData data = new MrzData();
        data.setFormat("AI_VISION");
        data.setDocumentType("CNI");
        data.setIssuingCountry("CMR");

        try {
            // Strip markdown code fences if present
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(cleaned);

            if (node.has("nom") && !node.get("nom").isNull()) {
                data.setNom(node.get("nom").asText().toUpperCase().trim());
            }
            if (node.has("prenom") && !node.get("prenom").isNull()) {
                data.setPrenom(node.get("prenom").asText().toUpperCase().trim());
            }
            if (node.has("dateNaissance") && !node.get("dateNaissance").isNull()) {
                LocalDate dob = parseDateDMY(node.get("dateNaissance").asText());
                if (dob != null) data.setDateNaissance(dob);
            }
            if (node.has("sexe") && !node.get("sexe").isNull()) {
                data.setSexe(node.get("sexe").asText().toUpperCase().trim());
            }
            if (node.has("dateExpiration") && !node.get("dateExpiration").isNull()) {
                LocalDate expiry = parseDateDMY(node.get("dateExpiration").asText());
                if (expiry != null) data.setDateExpiration(expiry);
            }
            if (node.has("documentNumber") && !node.get("documentNumber").isNull()) {
                data.setDocumentNumber(node.get("documentNumber").asText().toUpperCase().trim());
            }

            data.setValid(data.getNom() != null && data.getDateNaissance() != null);

            LOG.info(
                "parseVisionJson result — valid={}, nom={}, prenom={}, dob={}, expiry={}, sex={}, doc={}",
                data.isValid(),
                data.getNom(),
                data.getPrenom(),
                data.getDateNaissance(),
                data.getDateExpiration(),
                data.getSexe(),
                data.getDocumentNumber()
            );
        } catch (Exception e) {
            LOG.error("Failed to parse AI vision JSON response: {}", e.getMessage());
            data.setValid(false);
        }

        return data;
    }
}
