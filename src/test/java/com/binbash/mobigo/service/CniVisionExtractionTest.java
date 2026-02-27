package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.service.dto.MrzData;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AI Vision extraction in {@link CniVerificationService}.
 * Tests JSON parsing, merge logic, and incompleteness detection.
 * No real API calls — only tests the parsing of Claude's response.
 */
class CniVisionExtractionTest {

    private CniVerificationService service;

    @BeforeEach
    void setUp() {
        // parseVisionJson(), isIncomplete(), mergeVisualData() are pure logic — no dependencies needed
        service = new CniVerificationService(null, null, null, null);
    }

    // ==================== parseVisionJson ====================

    @Test
    void shouldParseValidJsonResponse() {
        String json =
            "{\"nom\":\"ETONGO\",\"prenom\":\"PATIAN\",\"dateNaissance\":\"16.04.1999\"," +
            "\"sexe\":\"M\",\"dateExpiration\":\"09.05.2029\",\"documentNumber\":\"AA10340702\"}";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("AI_VISION");
        assertThat(result.getDocumentType()).isEqualTo("CNI");
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getNom()).isEqualTo("ETONGO");
        assertThat(result.getPrenom()).isEqualTo("PATIAN");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
        assertThat(result.getSexe()).isEqualTo("M");
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2029, 5, 9));
        assertThat(result.getDocumentNumber()).isEqualTo("AA10340702");
    }

    @Test
    void shouldHandlePartialJsonResponse() {
        // Some fields are null — still valid if nom + dateNaissance present
        String json =
            "{\"nom\":\"MIMBE\",\"prenom\":null,\"dateNaissance\":\"29.10.1986\"," +
            "\"sexe\":\"F\",\"dateExpiration\":null,\"documentNumber\":null}";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("MIMBE");
        assertThat(result.getPrenom()).isNull();
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1986, 10, 29));
        assertThat(result.getSexe()).isEqualTo("F");
        assertThat(result.getDateExpiration()).isNull();
        assertThat(result.getDocumentNumber()).isNull();
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        String json = "This is not valid JSON at all, sorry!";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormat()).isEqualTo("AI_VISION");
    }

    @Test
    void shouldHandleEmptyJsonObject() {
        String json = "{}";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getNom()).isNull();
    }

    @Test
    void shouldHandleJsonWithCodeFences() {
        // Claude sometimes wraps JSON in markdown code fences
        String json =
            "```json\n{\"nom\":\"FOTSO\",\"prenom\":\"BLANCHE\",\"dateNaissance\":\"05.07.1992\"," +
            "\"sexe\":\"F\",\"dateExpiration\":\"01.01.2030\",\"documentNumber\":\"CMR123456\"}\n```";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getNom()).isEqualTo("FOTSO");
        assertThat(result.getPrenom()).isEqualTo("BLANCHE");
        assertThat(result.getDocumentNumber()).isEqualTo("CMR123456");
    }

    @Test
    void shouldHandleDateWithSlashSeparator() {
        String json =
            "{\"nom\":\"KAMENI\",\"prenom\":\"FRIDE\",\"dateNaissance\":\"19/10/1981\"," +
            "\"sexe\":\"F\",\"dateExpiration\":\"25/04/2035\",\"documentNumber\":\"BB999\"}";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1981, 10, 19));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2035, 4, 25));
    }

    @Test
    void shouldReturnInvalidWhenNomMissing() {
        // nom is required for validity
        String json =
            "{\"nom\":null,\"prenom\":\"PATIAN\",\"dateNaissance\":\"16.04.1999\"," +
            "\"sexe\":\"M\",\"dateExpiration\":\"09.05.2029\",\"documentNumber\":\"AA10340702\"}";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldReturnInvalidWhenDateNaissanceMissing() {
        // dateNaissance is required for validity
        String json =
            "{\"nom\":\"ETONGO\",\"prenom\":\"PATIAN\",\"dateNaissance\":null," +
            "\"sexe\":\"M\",\"dateExpiration\":\"09.05.2029\",\"documentNumber\":\"AA10340702\"}";

        MrzData result = service.parseVisionJson(json);

        assertThat(result.isValid()).isFalse();
    }

    // ==================== isIncomplete ====================

    @Test
    void shouldDetectIncompleteDataMissingPrenom() {
        MrzData data = new MrzData();
        data.setValid(true);
        data.setNom("ETONGO");
        data.setDateNaissance(LocalDate.of(1999, 4, 16));
        data.setPrenom(null); // missing
        data.setDateExpiration(LocalDate.of(2029, 5, 9));
        data.setDocumentNumber("AA10340702");

        assertThat(service.isIncomplete(data)).isTrue();
    }

    @Test
    void shouldDetectIncompleteDataMissingExpiry() {
        MrzData data = new MrzData();
        data.setValid(true);
        data.setNom("ETONGO");
        data.setPrenom("PATIAN");
        data.setDateNaissance(LocalDate.of(1999, 4, 16));
        data.setDateExpiration(null); // missing
        data.setDocumentNumber("AA10340702");

        assertThat(service.isIncomplete(data)).isTrue();
    }

    @Test
    void shouldDetectIncompleteDataMissingDocNumber() {
        MrzData data = new MrzData();
        data.setValid(true);
        data.setNom("ETONGO");
        data.setPrenom("PATIAN");
        data.setDateNaissance(LocalDate.of(1999, 4, 16));
        data.setDateExpiration(LocalDate.of(2029, 5, 9));
        data.setDocumentNumber(null); // missing

        assertThat(service.isIncomplete(data)).isTrue();
    }

    @Test
    void shouldDetectCompleteData() {
        MrzData data = new MrzData();
        data.setValid(true);
        data.setNom("ETONGO");
        data.setPrenom("PATIAN");
        data.setDateNaissance(LocalDate.of(1999, 4, 16));
        data.setDateExpiration(LocalDate.of(2029, 5, 9));
        data.setDocumentNumber("AA10340702");

        assertThat(service.isIncomplete(data)).isFalse();
    }

    @Test
    void shouldNotReportIncompleteForInvalidData() {
        MrzData data = new MrzData();
        data.setValid(false);

        assertThat(service.isIncomplete(data)).isFalse();
    }

    // ==================== mergeVisualData ====================

    @Test
    void shouldMergeIncompleteData() {
        // Target has nom + dob from Tesseract, but missing prenom, expiry, docNumber
        MrzData target = new MrzData();
        target.setValid(true);
        target.setNom("ETONGO");
        target.setDateNaissance(LocalDate.of(1999, 4, 16));

        // Source (AI vision) has all fields
        MrzData source = new MrzData();
        source.setNom("ETONGO");
        source.setPrenom("PATIAN");
        source.setDateNaissance(LocalDate.of(1999, 4, 16));
        source.setSexe("M");
        source.setDateExpiration(LocalDate.of(2029, 5, 9));
        source.setDocumentNumber("AA10340702");

        service.mergeVisualData(target, source);

        // Target fields should be preserved
        assertThat(target.getNom()).isEqualTo("ETONGO");
        assertThat(target.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
        // Gaps should be filled from source
        assertThat(target.getPrenom()).isEqualTo("PATIAN");
        assertThat(target.getSexe()).isEqualTo("M");
        assertThat(target.getDateExpiration()).isEqualTo(LocalDate.of(2029, 5, 9));
        assertThat(target.getDocumentNumber()).isEqualTo("AA10340702");
    }

    @Test
    void shouldNotOverwriteExistingFields() {
        MrzData target = new MrzData();
        target.setNom("ETONGO");
        target.setPrenom("PATIAN CORRECT");
        target.setDateNaissance(LocalDate.of(1999, 4, 16));

        MrzData source = new MrzData();
        source.setNom("WRONG");
        source.setPrenom("WRONG PRENOM");
        source.setDateNaissance(LocalDate.of(2000, 1, 1));
        source.setDocumentNumber("NEWDOC123");

        service.mergeVisualData(target, source);

        // Existing fields must NOT be overwritten
        assertThat(target.getNom()).isEqualTo("ETONGO");
        assertThat(target.getPrenom()).isEqualTo("PATIAN CORRECT");
        assertThat(target.getDateNaissance()).isEqualTo(LocalDate.of(1999, 4, 16));
        // Only null fields are filled
        assertThat(target.getDocumentNumber()).isEqualTo("NEWDOC123");
    }
}
