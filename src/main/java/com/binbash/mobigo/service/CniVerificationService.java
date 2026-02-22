package com.binbash.mobigo.service;

import com.binbash.mobigo.config.ApplicationProperties;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.service.dto.CniVerificationDTO;
import com.binbash.mobigo.service.dto.MrzData;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(applicationProperties.getTesseract().getDataPath());
            tesseract.setLanguage(applicationProperties.getTesseract().getLanguage());
            tesseract.setPageSegMode(3); // Fully automatic segmentation
            tesseract.setVariable("tessedit_char_whitelist", ""); // Full charset

            // Flexible date patterns: DD.MM.YYYY, DD/MM/YYYY, DD-MM-YYYY, DD MM YYYY
            // Also handle OCR artifacts like extra spaces around separators
            Pattern datePattern = Pattern.compile("(\\d{2})\\s*[./\\-,;:\\s]\\s*(\\d{2})\\s*[./\\-,;:\\s]\\s*(\\d{4})");
            LocalDate now = LocalDate.now();

            // Collect ALL dates from ALL images first, then pick best DOB/expiry
            List<LocalDate> allDates = new ArrayList<>();
            String combinedText = "";

            for (String path : imagePaths) {
                if (path == null) continue;
                BufferedImage img;
                if (path.toLowerCase().endsWith(".pdf")) {
                    img = convertPdfToImage(path);
                } else {
                    img = ImageIO.read(Path.of(path).toFile());
                }
                if (img == null) continue;

                // Downscale to limit memory usage, flush original if a new image was created
                BufferedImage limited = limitSize(img, MAX_OCR_WIDTH);
                if (limited != img) {
                    img.flush();
                    img = limited;
                }

                // Try raw + preprocessed + sharpened variants for maximum date extraction
                BufferedImage preprocessed = preprocessForMrz(img);
                BufferedImage sharpened = sharpen(img);
                BufferedImage[] variants = { img, preprocessed, sharpened };
                String[] variantNames = { "raw", "preprocessed", "sharpened" };

                for (int v = 0; v < variants.length; v++) {
                    String text = tesseract.doOCR(variants[v]);
                    String preview = text.length() > 500 ? text.substring(0, 500) + "..." : text;
                    LOG.info("Visual text [{}] from {} ({} chars):\n{}", variantNames[v], path, text.length(), preview);
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
                            LOG.info("Found date in visual text [{}]: {} (raw: '{}')", variantNames[v], d, matcher.group());
                        } catch (Exception e) {
                            // skip invalid date
                        }
                    }
                }
                // Flush all variant images to release native memory before processing next
                sharpened.flush();
                preprocessed.flush();
                img.flush();
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

        // Check if any word in MRZ matches any word in profile
        String[] mrzParts = normalizedMrz.split("\\s+");
        String[] profileParts = normalizedProfile.split("\\s+");
        for (String mp : mrzParts) {
            for (String pp : profileParts) {
                if (!mp.isEmpty() && !pp.isEmpty() && mp.equals(pp)) {
                    return true;
                }
            }
        }

        return false;
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
}
