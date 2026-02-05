package com.binbash.mobigo.service;

import com.binbash.mobigo.service.dto.MrzData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for parsing the MRZ (Machine Readable Zone) of identity documents.
 *
 * Supports three ICAO formats:
 * - TD1 (ID cards, residence permits): 3 lines x 30 characters
 * - TD2 (older ID cards): 2 lines x 36 characters
 * - TD3 (passports): 2 lines x 44 characters
 *
 * Supported documents: Cameroon CNI, France CNI, Cameroon Passport, France Passport, France Carte de sejour.
 */
@Service
public class CniMrzParserService {

    private static final Logger LOG = LoggerFactory.getLogger(CniMrzParserService.class);

    // MRZ lines contain only uppercase letters, digits, and '<' filler (28-44 chars)
    private static final Pattern MRZ_LINE_PATTERN = Pattern.compile("^[A-Z0-9<]{28,44}$");

    // Supported ID card prefixes (CNI)
    private static final String[] ID_PREFIXES = { "IDCMR", "IDFRA" };

    // Supported residence permit prefixes (titre de sejour / carte de sejour)
    private static final String[] RESIDENCE_PREFIXES = { "IRFRA" };

    // Supported passport issuing countries (handles any subtype: P<, PO, PD, PS, etc.)
    private static final String[] PASSPORT_COUNTRIES = { "CMR", "FRA" };

    /**
     * Parse MRZ text extracted by OCR. Detects format automatically.
     */
    public MrzData parseMrz(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            LOG.warn("Empty OCR text provided for MRZ parsing");
            return invalidResult("Empty OCR text");
        }

        List<String> mrzLines = extractMrzLines(ocrText);
        LOG.debug("Extracted {} MRZ lines from OCR text", mrzLines.size());

        if (mrzLines.size() >= 3) {
            // Try TD1 format (ID cards / residence permits): 3 lines x 30 chars
            List<String> td1Lines = findTD1Lines(mrzLines);
            if (td1Lines != null) {
                LOG.debug("Detected TD1 format (ID card / residence permit)");
                return parseTD1(td1Lines);
            }
        }

        if (mrzLines.size() >= 2) {
            // Try TD3 format (passports): 2 lines x 44 chars — check BEFORE TD2
            List<String> td3Lines = findTD3Lines(mrzLines);
            if (td3Lines != null) {
                LOG.debug("Detected TD3 format (Passport)");
                return parseTD3(td3Lines);
            }

            // Try TD2 format (older ID cards): 2 lines x 36 chars
            List<String> td2Lines = findTD2Lines(mrzLines);
            if (td2Lines != null) {
                LOG.debug("Detected TD2 format (older ID card)");
                return parseTD2(td2Lines);
            }
        }

        LOG.warn("Could not detect MRZ format from {} extracted lines", mrzLines.size());
        return invalidResult("MRZ not detected");
    }

    /**
     * Extract candidate MRZ lines from raw OCR text.
     * Filters lines matching the MRZ character set pattern.
     */
    List<String> extractMrzLines(String rawText) {
        List<String> mrzLines = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");

        for (String line : lines) {
            // Clean up common OCR artifacts
            String cleaned = line
                .trim()
                .replace(" ", "")
                .replace("«", "<")
                .replace("»", "<")
                .replace("(", "<")
                .replace(")", "<")
                .toUpperCase();

            if (MRZ_LINE_PATTERN.matcher(cleaned).matches() && cleaned.length() >= 28) {
                mrzLines.add(cleaned);
            }
        }
        return mrzLines;
    }

    /**
     * Find 3 consecutive TD1 lines (each ~30 chars, starting with an ID or residence permit prefix).
     */
    private List<String> findTD1Lines(List<String> mrzLines) {
        for (int i = 0; i <= mrzLines.size() - 3; i++) {
            String l1 = padOrTrim(mrzLines.get(i), 30);
            String l2 = padOrTrim(mrzLines.get(i + 1), 30);
            String l3 = padOrTrim(mrzLines.get(i + 2), 30);

            if (
                (hasPrefix(l1, ID_PREFIXES) || hasPrefix(l1, RESIDENCE_PREFIXES)) &&
                l1.length() >= 28 &&
                l2.length() >= 28 &&
                l3.length() >= 28
            ) {
                return List.of(l1, l2, l3);
            }
        }
        return null;
    }

    /**
     * Find 2 consecutive TD2 lines (each ~36 chars, starting with an ID or residence permit prefix).
     */
    private List<String> findTD2Lines(List<String> mrzLines) {
        for (int i = 0; i <= mrzLines.size() - 2; i++) {
            String l1 = mrzLines.get(i);
            String l2 = mrzLines.get(i + 1);

            if ((hasPrefix(l1, ID_PREFIXES) || hasPrefix(l1, RESIDENCE_PREFIXES)) && l1.length() >= 34 && l2.length() >= 34) {
                return List.of(padOrTrim(l1, 36), padOrTrim(l2, 36));
            }
        }
        return null;
    }

    /**
     * Find 2 consecutive TD3 lines (each ~44 chars, starting with a passport prefix).
     * Passport MRZ starts with 'P' + subtype char + country code (e.g. P<CMR, POCMR, PDFRA).
     */
    private List<String> findTD3Lines(List<String> mrzLines) {
        for (int i = 0; i <= mrzLines.size() - 2; i++) {
            String l1 = mrzLines.get(i);
            String l2 = mrzLines.get(i + 1);

            if (isPassportPrefix(l1) && l1.length() >= 42 && l2.length() >= 42) {
                return List.of(padOrTrim(l1, 44), padOrTrim(l2, 44));
            }
        }
        return null;
    }

    /**
     * Check if a line starts with a passport prefix: P + any char + supported country code.
     * Covers all ICAO subtypes: P< (standard), PO (ordinary), PD (diplomatic), PS (service), etc.
     */
    private boolean isPassportPrefix(String line) {
        if (line.length() < 5 || line.charAt(0) != 'P') {
            return false;
        }
        String country = line.substring(2, 5).replace("<", "");
        for (String c : PASSPORT_COUNTRIES) {
            if (c.equals(country)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse ID card - TD1 format (3 lines x 30 chars).
     *
     * Line 1: ID + country(3) + document_number(9) + check(1) + filler(15)
     * Line 2: DOB(6) + check(1) + sex(1) + expiry(6) + check(1) + nationality(3) + filler(11) + check(1)
     * Line 3: lastname + << + firstnames (separated by <)
     */
    MrzData parseTD1(List<String> lines) {
        MrzData data = new MrzData();
        data.setFormat("TD1");

        try {
            String line1 = lines.get(0);
            String line2 = lines.get(1);
            String line3 = lines.get(2);
            data.setRawMrz(line1 + "\n" + line2 + "\n" + line3);

            // Extract document type code and issuing country from positions 0-1 and 2-4
            String docTypeCode = line1.substring(0, 2);
            String issuingCountry = line1.substring(2, 5).replace("<", "");
            data.setIssuingCountry(issuingCountry);
            data.setDocumentType(docTypeCode.startsWith("IR") ? "RESIDENCE_PERMIT" : "CNI");

            // Line 1: document number at positions 5-13
            String docNumber = line1.substring(5, 14).replace("<", "");
            data.setDocumentNumber(docNumber);

            // Line 2: DOB at positions 0-5, sex at 7, expiry at 8-13
            String dobStr = line2.substring(0, 6);
            data.setDateNaissance(parseMrzDate(dobStr, true));

            String sex = line2.substring(7, 8);
            data.setSexe(sex.equals("M") || sex.equals("F") ? sex : null);

            String expiryStr = line2.substring(8, 14);
            data.setDateExpiration(parseMrzDate(expiryStr, false));

            // Line 3: names
            parseNames(line3, data);

            data.setValid(data.getNom() != null && data.getDateNaissance() != null);
        } catch (Exception e) {
            LOG.error("Error parsing TD1 MRZ: {}", e.getMessage());
            data.setValid(false);
        }

        return data;
    }

    /**
     * Parse older ID card - TD2 format (2 lines x 36 chars).
     *
     * Line 1: ID/IR + country(3) + lastname + << + firstnames
     * Line 2: doc_number(12) + check(1) + nationality(3) + DOB(6) + check(1) + sex(1) + expiry(6) + check(1) + filler(5) + check(1)
     */
    MrzData parseTD2(List<String> lines) {
        MrzData data = new MrzData();
        data.setFormat("TD2");

        try {
            String line1 = lines.get(0);
            String line2 = lines.get(1);
            data.setRawMrz(line1 + "\n" + line2);

            // Extract document type code and issuing country from positions 0-1 and 2-4
            String docTypeCode = line1.substring(0, 2);
            String issuingCountry = line1.substring(2, 5).replace("<", "");
            data.setIssuingCountry(issuingCountry);
            data.setDocumentType(docTypeCode.startsWith("IR") ? "RESIDENCE_PERMIT" : "CNI");

            // Line 1: names start after country code (position 5)
            String namesPart = line1.substring(5);
            parseNames(namesPart, data);

            // Line 2: document number at 0-11, nationality at 13-15, DOB at 16-21, sex at 23, expiry at 24-29
            String docNumber = line2.substring(0, 12).replace("<", "");
            data.setDocumentNumber(docNumber);

            String dobStr = line2.substring(16, 22);
            data.setDateNaissance(parseMrzDate(dobStr, true));

            String sex = line2.substring(23, 24);
            data.setSexe(sex.equals("M") || sex.equals("F") ? sex : null);

            String expiryStr = line2.substring(24, 30);
            data.setDateExpiration(parseMrzDate(expiryStr, false));

            data.setValid(data.getNom() != null && data.getDateNaissance() != null);
        } catch (Exception e) {
            LOG.error("Error parsing TD2 MRZ: {}", e.getMessage());
            data.setValid(false);
        }

        return data;
    }

    /**
     * Parse passport - TD3 format (2 lines x 44 chars).
     *
     * Line 1: P< + issuing_country(3) + surname + << + given_names (total 44)
     * Line 2: passport_number(9) + check(1) + nationality(3) + DOB(6) + check(1) + sex(1) + expiry(6) + check(1) + personal_number(14) + check(1) + overall_check(1)
     */
    MrzData parseTD3(List<String> lines) {
        MrzData data = new MrzData();
        data.setFormat("TD3");
        data.setDocumentType("PASSPORT");

        try {
            String line1 = lines.get(0);
            String line2 = lines.get(1);
            data.setRawMrz(line1 + "\n" + line2);

            // Line 1: issuing country at positions 2-4, names from position 5
            String issuingCountry = line1.substring(2, 5).replace("<", "");
            data.setIssuingCountry(issuingCountry);

            String namesPart = line1.substring(5);
            parseNames(namesPart, data);

            // Line 2: passport number at 0-8, nationality at 10-12, DOB at 13-18, sex at 20, expiry at 21-26
            String docNumber = line2.substring(0, 9).replace("<", "");
            data.setDocumentNumber(docNumber);

            String dobStr = line2.substring(13, 19);
            data.setDateNaissance(parseMrzDate(dobStr, true));

            String sex = line2.substring(20, 21);
            data.setSexe(sex.equals("M") || sex.equals("F") ? sex : null);

            String expiryStr = line2.substring(21, 27);
            data.setDateExpiration(parseMrzDate(expiryStr, false));

            data.setValid(data.getNom() != null && data.getDateNaissance() != null);
        } catch (Exception e) {
            LOG.error("Error parsing TD3 MRZ: {}", e.getMessage());
            data.setValid(false);
        }

        return data;
    }

    /**
     * Parse names from MRZ format: LASTNAME<<FIRSTNAME<SECONDNAME
     */
    private void parseNames(String nameField, MrzData data) {
        // Split by << to separate last name from first names
        int separatorIdx = nameField.indexOf("<<");
        if (separatorIdx > 0) {
            String lastName = nameField.substring(0, separatorIdx).replace("<", " ").trim();
            String firstNames = nameField.substring(separatorIdx + 2).replace("<", " ").trim();
            data.setNom(lastName);
            data.setPrenom(firstNames);
        } else {
            // Fallback: entire field is the last name
            data.setNom(nameField.replace("<", " ").trim());
        }
    }

    /**
     * Parse MRZ date in YYMMDD format.
     *
     * @param dateStr   6-character date string in YYMMDD format
     * @param isBirthDate true for DOB (pivot: >40 -> 19xx), false for expiry (pivot: <60 -> 20xx)
     */
    LocalDate parseMrzDate(String dateStr, boolean isBirthDate) {
        if (dateStr == null || dateStr.length() != 6 || dateStr.contains("<")) {
            return null;
        }
        try {
            int yy = Integer.parseInt(dateStr.substring(0, 2));
            int mm = Integer.parseInt(dateStr.substring(2, 4));
            int dd = Integer.parseInt(dateStr.substring(4, 6));

            int year;
            if (isBirthDate) {
                year = yy > 40 ? 1900 + yy : 2000 + yy;
            } else {
                year = yy < 60 ? 2000 + yy : 1900 + yy;
            }

            return LocalDate.of(year, mm, dd);
        } catch (Exception e) {
            LOG.debug("Failed to parse MRZ date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a line starts with any of the given prefixes.
     */
    private boolean hasPrefix(String line, String[] prefixes) {
        for (String prefix : prefixes) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pad or trim a string to the specified length using '<' as filler.
     */
    private String padOrTrim(String s, int length) {
        if (s.length() >= length) {
            return s.substring(0, length);
        }
        return s + "<".repeat(length - s.length());
    }

    private MrzData invalidResult(String reason) {
        MrzData data = new MrzData();
        data.setValid(false);
        data.setRawMrz(reason);
        return data;
    }
}
