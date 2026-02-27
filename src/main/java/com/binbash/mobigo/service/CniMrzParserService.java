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
    // Supports both old format (IDCMR) and new format (I<CMR) prefixes
    private static final String[] ID_PREFIXES = { "IDCMR", "IDFRA", "I<CMR", "I<FRA" };

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
        LOG.info("Extracted {} MRZ-candidate lines from OCR text ({} chars)", mrzLines.size(), ocrText.length());
        for (int i = 0; i < mrzLines.size(); i++) {
            LOG.info("  MRZ line {}: [{}] (len={})", i, mrzLines.get(i), mrzLines.get(i).length());
        }

        if (mrzLines.size() >= 3) {
            // Try TD1 format (ID cards / residence permits): 3 lines x 30 chars
            List<String> td1Lines = findTD1Lines(mrzLines);
            if (td1Lines != null) {
                LOG.info("Detected TD1 format (ID card / residence permit)");
                return parseTD1(td1Lines);
            }
        }

        if (mrzLines.size() >= 2) {
            // Try TD3 format (passports): 2 lines x 44 chars — check BEFORE TD2
            List<String> td3Lines = findTD3Lines(mrzLines);
            if (td3Lines != null) {
                LOG.info("Detected TD3 format (Passport)");
                return parseTD3(td3Lines);
            }

            // Try TD2 format (older ID cards): 2 lines x 36 chars
            List<String> td2Lines = findTD2Lines(mrzLines);
            if (td2Lines != null) {
                LOG.info("Detected TD2 format (older ID card)");
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
                .replace("{", "<")
                .replace("[", "<")
                .toUpperCase();

            if (MRZ_LINE_PATTERN.matcher(cleaned).matches() && cleaned.length() >= 28) {
                mrzLines.add(cleaned);
            }
        }
        return mrzLines;
    }

    /**
     * Find 3 consecutive TD1 lines (each ~30 chars, starting with an ID or residence permit prefix).
     * Uses flexible matching: I + any char + known country code, to handle OCR misreadings of '<'.
     */
    private List<String> findTD1Lines(List<String> mrzLines) {
        for (int i = 0; i <= mrzLines.size() - 3; i++) {
            String l1 = padOrTrim(mrzLines.get(i), 30);
            String l2 = padOrTrim(mrzLines.get(i + 1), 30);
            String l3 = padOrTrim(mrzLines.get(i + 2), 30);

            if (
                (hasPrefix(l1, ID_PREFIXES) || hasPrefix(l1, RESIDENCE_PREFIXES) || looksLikeIdCardLine(l1)) &&
                l1.length() >= 28 &&
                l2.length() >= 28 &&
                l3.length() >= 28
            ) {
                // Normalize the second char to '<' if it was misread by OCR
                if (!hasPrefix(l1, ID_PREFIXES) && !hasPrefix(l1, RESIDENCE_PREFIXES) && looksLikeIdCardLine(l1)) {
                    l1 = l1.charAt(0) + "<" + l1.substring(2);
                    LOG.debug("Normalized TD1 line 1 prefix: {}", l1.substring(0, 5));
                }
                return List.of(l1, l2, l3);
            }
        }
        return null;
    }

    /**
     * Flexible detection: line starts with 'I' + any char + known country code.
     * Handles cases where OCR misreads '<' as K, C, or other characters.
     * Excludes 'IR' prefix (residence permits) which are handled separately.
     */
    private boolean looksLikeIdCardLine(String line) {
        if (line.length() < 5 || line.charAt(0) != 'I') return false;
        if (line.charAt(1) == 'R') return false; // Residence permit, handled by RESIDENCE_PREFIXES
        String country = line.substring(2, 5).replace("<", "");
        for (String c : PASSPORT_COUNTRIES) {
            if (c.equals(country)) return true;
        }
        return false;
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
            String line2 = sanitizeTd1DataLine(lines.get(1));
            String line3 = sanitizeNameOnlyLine(lines.get(2));
            data.setRawMrz(line1 + "\n" + line2 + "\n" + line3);

            // Extract document type code and issuing country from positions 0-1 and 2-4
            String docTypeCode = line1.substring(0, 2);
            String issuingCountry = line1.substring(2, 5).replace("<", "");
            data.setIssuingCountry(issuingCountry);
            data.setDocumentType(docTypeCode.startsWith("IR") ? "RESIDENCE_PERMIT" : "CNI");

            // Line 1: document number at positions 5-13
            String docNumber = line1.substring(5, 14).replace("<", "");
            data.setDocumentNumber(docNumber);

            // New format CMR cards (I< prefix): the user-visible NIC number is in optional data (pos 15-29)
            // e.g. I<CMR1002275191AA10340702<<<<<< → NIC = AA10340702
            if ("I<".equals(docTypeCode) && "CMR".equals(issuingCountry)) {
                String optionalData = line1.substring(15).replace("<", "").trim();
                if (!optionalData.isEmpty()) {
                    data.setDocumentNumber(optionalData);
                }
            }

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
            String line1 = sanitizeTd2NameLine(lines.get(0));
            String line2 = sanitizeTd2DataLine(lines.get(1));
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
            String line1 = sanitizeTd3NameLine(lines.get(0));
            String line2 = sanitizeTd3DataLine(lines.get(1));
            data.setRawMrz(line1 + "\n" + line2);
            LOG.debug("TD3 after sanitization — L1: [{}]  L2: [{}]", line1, line2);

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

    // ---- MRZ field-type sanitization (ICAO 9303 spec) ----
    // Each MRZ position has a known type: letter-only, digit-only, or mixed.
    // OCR commonly confuses visually similar characters across types.
    // These methods fix such confusions using the known field types.

    /**
     * TD3 line 1: positions 0-4 = type+country (letters), positions 5-43 = names (letters + '<' only).
     * Any digit in the name field is an OCR error.
     */
    private String sanitizeTd3NameLine(String line) {
        if (line.length() <= 5) return line;
        StringBuilder sb = new StringBuilder(line.substring(0, 5));
        for (int i = 5; i < line.length(); i++) {
            sb.append(ensureLetter(line.charAt(i)));
        }
        return restoreTrailingFillers(sb.toString(), 5);
    }

    /**
     * TD3 line 2: fix digit/letter confusions in type-specific fields.
     * Positions: 0-8 passport# (mixed), 9 check (digit), 10-12 nationality (letter),
     * 13-18 DOB (digit), 19 check (digit), 20 sex (letter), 21-26 expiry (digit),
     * 27 check (digit), 28-41 personal# (mixed), 42 check (digit), 43 overall check (digit).
     */
    private String sanitizeTd3DataLine(String line) {
        if (line.length() < 42) return line;
        char[] c = line.toCharArray();
        c[9] = ensureDigit(c[9]);
        for (int i = 10; i <= 12; i++) c[i] = ensureLetter(c[i]);
        for (int i = 13; i <= 18; i++) c[i] = ensureDigit(c[i]);
        c[19] = ensureDigit(c[19]);
        c[20] = ensureLetter(c[20]);
        for (int i = 21; i <= 26; i++) c[i] = ensureDigit(c[i]);
        if (c.length > 27) c[27] = ensureDigit(c[27]);
        if (c.length > 42) c[42] = ensureDigit(c[42]);
        if (c.length > 43) c[43] = ensureDigit(c[43]);
        return new String(c);
    }

    /**
     * TD1 line 2: DOB(6) + check(1) + sex(1) + expiry(6) + check(1) + nationality(3) + optional(11) + check(1).
     */
    private String sanitizeTd1DataLine(String line) {
        if (line.length() < 28) return line;
        char[] c = line.toCharArray();
        for (int i = 0; i <= 5; i++) c[i] = ensureDigit(c[i]);
        c[6] = ensureDigit(c[6]);
        c[7] = ensureLetter(c[7]); // sex
        for (int i = 8; i <= 13; i++) c[i] = ensureDigit(c[i]);
        c[14] = ensureDigit(c[14]);
        for (int i = 15; i <= 17; i++) c[i] = ensureLetter(c[i]); // nationality
        if (c.length > 29) c[29] = ensureDigit(c[29]);
        return new String(c);
    }

    /**
     * TD2 line 1: positions 0-4 = type+country (letters), positions 5+ = names (letters + '<').
     */
    private String sanitizeTd2NameLine(String line) {
        if (line.length() <= 5) return line;
        StringBuilder sb = new StringBuilder(line.substring(0, 5));
        for (int i = 5; i < line.length(); i++) {
            sb.append(ensureLetter(line.charAt(i)));
        }
        return restoreTrailingFillers(sb.toString(), 5);
    }

    /**
     * TD2 line 2: doc#(12) + check(1) + nationality(3) + DOB(6) + check(1) + sex(1) + expiry(6) + check(1) + filler(5) + check(1).
     */
    private String sanitizeTd2DataLine(String line) {
        if (line.length() < 30) return line;
        char[] c = line.toCharArray();
        c[12] = ensureDigit(c[12]);
        for (int i = 13; i <= 15; i++) c[i] = ensureLetter(c[i]); // nationality
        for (int i = 16; i <= 21; i++) c[i] = ensureDigit(c[i]); // DOB
        c[22] = ensureDigit(c[22]);
        c[23] = ensureLetter(c[23]); // sex
        for (int i = 24; i <= 29; i++) c[i] = ensureDigit(c[i]); // expiry
        if (c.length > 30) c[30] = ensureDigit(c[30]);
        if (c.length > 35) c[35] = ensureDigit(c[35]);
        return new String(c);
    }

    /**
     * Sanitize a name-only line (e.g. TD1 line 3): should only contain letters and '<'.
     */
    private String sanitizeNameOnlyLine(String line) {
        StringBuilder sb = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            sb.append(ensureLetter(line.charAt(i)));
        }
        return restoreTrailingFillers(sb.toString(), 0);
    }

    /**
     * Ensure a character is a letter or '<'. If it's a digit, convert using common OCR confusions.
     */
    private char ensureLetter(char c) {
        if (Character.isLetter(c) || c == '<') return c;
        return switch (c) {
            case '0' -> 'O';
            case '1' -> 'I';
            case '2' -> 'Z';
            case '3' -> 'B'; // 3 resembles B in degraded MRZ
            case '4' -> '<'; // '<' commonly misread as '4' in MRZ font
            case '5' -> 'S';
            case '6' -> 'G';
            case '7' -> '<'; // filler in name context
            case '8' -> 'B';
            case '9' -> '<'; // filler in name context
            default -> '<';
        };
    }

    /**
     * Ensure a character is a digit or '<'. If it's a letter, convert using common OCR confusions.
     */
    private char ensureDigit(char c) {
        if (Character.isDigit(c) || c == '<') return c;
        return switch (c) {
            case 'O', 'Q', 'D' -> '0';
            case 'I', 'L' -> '1';
            case 'Z' -> '2';
            case 'B' -> '8';
            case 'S' -> '5';
            case 'G' -> '6';
            case 'A' -> '4';
            case 'T' -> '7';
            default -> '0';
        };
    }

    /**
     * Restore trailing '<' fillers in MRZ name fields that were misread as letters by OCR.
     *
     * In MRZ name fields, '<' is used as filler to pad names to the required field length.
     * OCR commonly misreads '<' as L, C, S, or K due to visual similarity in the MRZ font.
     * This method detects trailing runs of such confusion characters and converts them back to '<'.
     *
     * Safety: only applies when the trailing run has >= 5 misread letters AND L makes up
     * the majority, which is virtually impossible in real names but common in misread fillers.
     */
    private String restoreTrailingFillers(String line, int nameStart) {
        if (line == null || line.length() <= nameStart) return line;

        char[] chars = line.toCharArray();

        // Scan from right: find where the trailing confusion-character run starts
        int runStart = chars.length;
        for (int i = chars.length - 1; i >= nameStart; i--) {
            char c = chars[i];
            if (c == '<' || c == 'L' || c == 'S' || c == 'C' || c == 'K') {
                runStart = i;
            } else {
                break;
            }
        }

        int runLength = chars.length - runStart;
        if (runLength < 5) return line;

        // Count misread letters (not already '<') and L occurrences in the run
        int letterCount = 0;
        int lCount = 0;
        for (int i = runStart; i < chars.length; i++) {
            if (chars[i] != '<') {
                letterCount++;
                if (chars[i] == 'L') lCount++;
            }
        }

        // Only restore if enough misread letters AND L dominates (most common '<' misread)
        if (letterCount >= 5 && lCount * 2 >= letterCount) {
            LOG.debug(
                "Restoring {} trailing fillers ({}L of {} letters, run={}) in: {}",
                letterCount,
                lCount,
                letterCount,
                runLength,
                line.substring(nameStart)
            );
            for (int i = runStart; i < chars.length; i++) {
                chars[i] = '<';
            }
            return new String(chars);
        }

        return line;
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
            LOG.info("Failed to parse MRZ date '{}': {}", dateStr, e.getMessage());
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
