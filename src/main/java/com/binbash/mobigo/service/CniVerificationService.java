package com.binbash.mobigo.service;

import com.binbash.mobigo.config.ApplicationProperties;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.service.dto.CniVerificationDTO;
import com.binbash.mobigo.service.dto.MrzData;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
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
    private static final String[] KNOWN_MRZ_PREFIXES = { "IDCMR", "IDFRA", "IRFRA", "P<CMR", "P<FRA", "POCMR", "POFRA" };

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

        String ocrText = runOcr(primaryPath);
        LOG.debug("OCR primary result for people {}: {}", peopleId, ocrText);

        // 3. Parse MRZ
        MrzData mrzData = mrzParserService.parseMrz(ocrText);

        // 4. If MRZ not found on primary side, try fallback
        if (!mrzData.isValid() && fallbackPath != null) {
            LOG.debug("MRZ not found on primary side, trying fallback for people {}", peopleId);
            String fallbackOcrText = runOcr(fallbackPath);
            mrzData = mrzParserService.parseMrz(fallbackOcrText);
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

    /**
     * Run Tesseract OCR on an image file, optimized for MRZ reading.
     * Uses a multi-strategy approach with different crop ratios and charsets.
     */
    private String runOcr(String imagePath) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(applicationProperties.getTesseract().getDataPath());
            tesseract.setLanguage(applicationProperties.getTesseract().getLanguage());

            BufferedImage originalImage = ImageIO.read(Path.of(imagePath).toFile());
            if (originalImage == null) {
                LOG.error("Could not read image at: {}", imagePath);
                return "";
            }

            // Prepare images: full + multiple bottom crops at different ratios
            BufferedImage fullImage = preprocessForMrz(originalImage);
            double[] cropRatios = { 0.25, 0.15 };

            // Strategy 1: Full image, restricted charset (PSM 6)
            String mrzText = ocrWithCharset(tesseract, fullImage, true);
            if (containsAnyPrefix(mrzText)) {
                LOG.debug("MRZ found: full image, restricted charset");
                return mrzText;
            }

            // Strategy 2-3: Bottom crops, restricted charset (PSM 6)
            for (double ratio : cropRatios) {
                BufferedImage crop = cropBottom(originalImage, ratio);
                BufferedImage processedCrop = preprocessForMrz(crop);
                // Scale up small crops for better OCR accuracy
                if (processedCrop.getHeight() < 300) {
                    processedCrop = scaleUp(processedCrop, 300.0 / processedCrop.getHeight());
                }
                mrzText = ocrWithCharset(tesseract, processedCrop, true);
                if (containsAnyPrefix(mrzText)) {
                    LOG.debug("MRZ found: bottom {}% crop, restricted charset", (int) (ratio * 100));
                    return mrzText;
                }
            }

            // Strategy 4: Full image, full charset (PSM 3)
            mrzText = ocrWithCharset(tesseract, fullImage, false);
            if (containsAnyPrefix(mrzText)) {
                LOG.debug("MRZ found: full image, full charset");
                return mrzText;
            }

            // Strategy 5-6: Bottom crops, full charset (PSM 3)
            for (double ratio : cropRatios) {
                BufferedImage crop = cropBottom(originalImage, ratio);
                BufferedImage processedCrop = preprocessForMrz(crop);
                if (processedCrop.getHeight() < 300) {
                    processedCrop = scaleUp(processedCrop, 300.0 / processedCrop.getHeight());
                }
                mrzText = ocrWithCharset(tesseract, processedCrop, false);
                if (containsAnyPrefix(mrzText)) {
                    LOG.debug("MRZ found: bottom {}% crop, full charset", (int) (ratio * 100));
                    return mrzText;
                }
            }

            LOG.warn("MRZ not found after all OCR strategies for image: {}", imagePath);
            return mrzText;
        } catch (TesseractException | IOException e) {
            LOG.error("OCR failed for image {}: {}", imagePath, e.getMessage());
            return "";
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
     */
    private BufferedImage cropBottom(BufferedImage image, double ratio) {
        int cropHeight = (int) (image.getHeight() * ratio);
        int y = image.getHeight() - cropHeight;
        return image.getSubimage(0, y, image.getWidth(), cropHeight);
    }

    /**
     * Scale up an image by the given factor for better OCR on small text.
     */
    private BufferedImage scaleUp(BufferedImage image, double factor) {
        int newWidth = (int) (image.getWidth() * factor);
        int newHeight = (int) (image.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, image.getType());
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    /**
     * Check if OCR text contains any known MRZ prefix.
     */
    private boolean containsAnyPrefix(String text) {
        for (String prefix : KNOWN_MRZ_PREFIXES) {
            if (text.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pre-process image for better OCR results using Otsu's binarization.
     * Converts to grayscale, then applies Otsu's automatic thresholding
     * to produce a clean black-and-white image optimal for MRZ reading.
     */
    private BufferedImage preprocessForMrz(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Step 1: Convert to grayscale values
        int[] grayValues = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(original.getRGB(x, y));
                grayValues[y * width + x] = (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
            }
        }

        // Step 2: Compute Otsu's optimal threshold
        int threshold = computeOtsuThreshold(grayValues);

        // Step 3: Apply threshold to create binary image
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int bw = grayValues[y * width + x] > threshold ? 255 : 0;
                int rgb = new Color(bw, bw, bw).getRGB();
                binary.setRGB(x, y, rgb);
            }
        }

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
