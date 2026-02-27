package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.service.dto.MrzData;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CniVerificationService#parseVisualText(String, String)}.
 * Tests the label-based regex extraction used as fallback for old-format CNI without MRZ.
 */
class CniVisualExtractionTest {

    private CniVerificationService service;

    @BeforeEach
    void setUp() {
        // parseVisualText() is pure text parsing — no dependencies needed
        service = new CniVerificationService(null, null, null, null);
    }

    // ==================== Pass 1: Inline label regex ====================

    @Test
    void shouldExtractNomAndPrenomFromRectoText() {
        String recto =
            "REPUBLIQUE DU CAMEROUN\n" +
            "CARTE NATIONALE D'IDENTITE\n" +
            "NOM / SURNAME: MIMBE\n" +
            "PRENOMS / GIVEN NAMES: LUCIEN YANNICK\n" +
            "DATE DE NAISSANCE / DATE OF BIRTH: 29.10.1986\n" +
            "SEXE / SEX: M\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("VISUAL");
        assertThat(result.getDocumentType()).isEqualTo("CNI");
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getNom()).isEqualTo("MIMBE");
        assertThat(result.getPrenom()).isEqualTo("LUCIEN YANNICK");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1986, 10, 29));
        assertThat(result.getSexe()).isEqualTo("M");
    }

    @Test
    void shouldExtractDatesFromVisualText() {
        String recto = "NOM: KAMENI\n" + "PRENOMS: FRIDE BLANCHE\n" + "DATE DE NAISSANCE: 19/10/1981\n";
        String verso = "DATE D'EXPIRATION: 25/04/2035\n" + "IDENTIFIANT UNIQUE: AA10340702\n";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1981, 10, 19));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2035, 4, 25));
        assertThat(result.getDocumentNumber()).isEqualTo("AA10340702");
    }

    @Test
    void shouldExtractSexAndDocumentNumber() {
        String recto = "NOM: DUPONT\n" + "DATE DE NAISSANCE: 01.01.1990\n" + "SEXE: F\n";
        String verso = "N° CNI: CMR123456789\n" + "EXPIRE LE: 31.12.2030\n";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getSexe()).isEqualTo("F");
        assertThat(result.getDocumentNumber()).isEqualTo("CMR123456789");
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2030, 12, 31));
    }

    @Test
    void shouldReturnInvalidWhenNoLabelsFound() {
        String recto = "This is just random text without any identity labels\n123 garbage data";
        String verso = "More random text here";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getNom()).isNull();
        assertThat(result.getFormat()).isEqualTo("VISUAL");
    }

    @Test
    void shouldHandleOcrArtifactsInLabels() {
        String recto = "NOM   MIMBE\n" + "PRENOM   LUCIEN  YANNICK\n" + "DATE DE NAISSANCE  29 10 1986\n" + "SEX  M\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("MIMBE");
        assertThat(result.getPrenom()).isEqualTo("LUCIEN YANNICK");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1986, 10, 29));
        assertThat(result.getSexe()).isEqualTo("M");
    }

    @Test
    void shouldFallbackToSearchingBothTextsForMissingFields() {
        String recto = "DATE DE NAISSANCE: 15.03.1975\n";
        String verso = "NOM / SURNAME: NGOUMOU\nDATE D'EXPIRATION: 01.01.2028\n";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("NGOUMOU");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1975, 3, 15));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2028, 1, 1));
    }

    @Test
    void shouldHandleDateWithDashSeparator() {
        String recto = "NOM: FOTSO\n" + "DATE DE NAISSANCE: 05-07-1992\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1992, 7, 5));
    }

    // ==================== Pass 2: Line-by-line keyword extraction ====================

    @Test
    void shouldExtractFromSeparateLines() {
        // Old-format CNI: labels on one line, values on the next
        String recto =
            "REPUBLIQUE DU CAMEROUN\n" +
            "REPUBLIC OF CAMEROON\n" +
            "NOM/SURNAME\n" +
            "\n" +
            "ETONGO\n" +
            "\n" +
            "PRENOMS/GIVEN NAMES\n" +
            "\n" +
            "PATIAN\n" +
            "\n" +
            "DATE DE NAISSANCE/DATE OF BIRTH\n" +
            "\n" +
            "16.04.1999\n" +
            "\n" +
            "SEXESEX TAILLE HEIGHT\n" +
            "M 1,75\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("ETONGO");
        assertThat(result.getPrenom()).isEqualTo("PATIAN");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
    }

    @Test
    void shouldExtractFromRealPreprocessedOcrOutput() {
        // Actual OCR output from old-format CNI (preprocessed variant)
        // Labels are garbled but keywords partially survive
        String recto =
            "NATIONAL IDENTITY-CARD\n" +
            "NATIONALE DIDENTITE\n" +
            "\n" +
            "REPUBLIQUE DU CAMEROUN\n" +
            "REPUBLIC OF CAMEROON\n" +
            "\n" +
            "[NORE EI NP KES\n" +
            "\n" +
            "ETONGO\n" +
            "\n" +
            "PRÉNOMS D MEN NAMES\n" +
            "\n" +
            "PATIAN\n" +
            "\n" +
            "DATE DE MAISSANCE-DATE OF ERT\n" +
            "\n" +
            "16.04.1999\n" +
            "\n" +
            "LIEU DE NAISSANCE PLACE OF 8:75 # A :\n" +
            "\n" +
            "MOUANKO\n" +
            "\n" +
            "SEXESEX TAILLE HEGHT\n" +
            "W 1,75\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        // Should extract via line-by-line keywords: PRENOM in label, NAISSANCE in date label
        assertThat(result.getPrenom()).isEqualTo("PATIAN");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
    }

    @Test
    void shouldExtractFromCombinedOcrVariants() {
        // Simulates concatenated output from 3 OCR variants (raw + preprocessed + sharpened)
        // as produced by ocrAllVariants()
        String recto =
            // Raw variant (garbled)
            " ETONGO\n" +
            " 16.04.1393\n" +
            // Preprocessed variant (better labels)
            " REPUBLIQUE DU CAMEROUN\n" +
            " [NORE EI NP KES\n" +
            " ETONGO\n" +
            " PRÉNOMS D MEN NAMES\n" +
            " PATIAN\n" +
            " DATE DE MAISSANCE-DATE OF ERT\n" +
            " 16.04.1999\n" +
            " SEXESEX TAILLE HEGHT\n" +
            " W 1,75\n" +
            // Sharpened variant (clean labels but some garbled values)
            " NOM/SURNAME\n" +
            " ETONGO\n" +
            " PRENOMS/GINEN NAMES\n" +
            " PATIAK\n" +
            " 16.94.1999\n" +
            " SERETSEX\n" +
            " M 1,75\n";
        String verso = " 09.05.2019 LT\n" + " 09.05.2029\n";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("ETONGO");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
        // Expiry from heuristic: 09.05.2029 is in the future
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2029, 5, 9));
    }

    // ==================== Pass 3: Heuristic date extraction ====================

    @Test
    void shouldExtractDatesByHeuristicWhenLabelsAbsent() {
        // No recognizable labels, but dates are present
        String recto = "NOM FOTSO\n" + "GARBLED TEXT\n" + "05.07.1992\n" + "SOME MORE TEXT\n";
        String verso = "GARBLED\n" + "01.01.2030\n";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("FOTSO");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1992, 7, 5));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    // ==================== Name cleaning ====================

    @Test
    void shouldCleanOcrNoiseFromExtractedNames() {
        // Simulates real production OCR where noise fragments appear after the actual name
        // e.g. "ETONGO SR LEE" where "SR LEE" is card design noise
        String recto =
            "NOM/SURNAME\n" + "\n" + "ETONGO SR LEE\n" + "\n" + "PRENOMS/GIVEN NAMES\n" + "\n" + "PATIAN\n" + "\n" + "16.04.1999\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("ETONGO");
        assertThat(result.getPrenom()).isEqualTo("PATIAN");
    }

    @Test
    void shouldPreserveCompoundNames() {
        // Real compound names should NOT be truncated
        String recto = "NOM: KAMENI EPSE MIMBE\n" + "PRENOMS: FRIDE BLANCHE\n" + "DATE DE NAISSANCE: 19.10.1981\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.getNom()).isEqualTo("KAMENI EPSE MIMBE");
        assertThat(result.getPrenom()).isEqualTo("FRIDE BLANCHE");
    }

    // ==================== Pass 4: Positional prenom extraction ====================

    @Test
    void shouldExtractPrenomByPositionWhenLabelsGarbled() {
        // Simulates old-format CNI where PRENOM label is completely unrecognizable.
        // Real OCR garbling produces short fragments like "R1 DG PK" that cleanNameValue() filters.
        // The actual prenom value appears between NOM and DOB in the standard card layout.
        String recto =
            "NOM/SURNAME\n" +
            "\n" +
            "ETONGO\n" +
            "\n" +
            "R1 DG PK MN\n" + // garbled prenom label — short OCR fragments
            "\n" +
            "PATIAN\n" +
            "\n" +
            "16.04.1999\n";
        String verso = "";

        MrzData result = service.parseVisualText(recto, verso);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("ETONGO");
        // Pass 4 should find PATIAN positionally between ETONGO and 16.04.1999
        assertThat(result.getPrenom()).isEqualTo("PATIAN");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
    }
}
