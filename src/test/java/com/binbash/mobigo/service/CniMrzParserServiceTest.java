package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.service.dto.MrzData;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CniMrzParserService}.
 * Tests both old (IDCMR) and new (I<CMR) Cameroon CNI formats.
 */
class CniMrzParserServiceTest {

    private CniMrzParserService parser;

    @BeforeEach
    void setUp() {
        parser = new CniMrzParserService();
    }

    @Test
    void shouldParseOldFormatCameroonCni() {
        // Old format: IDCMR prefix, TD1
        String mrz = "IDCMR1234567891<<<<<<<<<<<<<<<\n" + "8610191M3504201CMR<<<<<<<<<<<<7\n" + "MIMBE<<LUCIEN<YANNICK<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("TD1");
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getDocumentType()).isEqualTo("CNI");
        assertThat(result.getDocumentNumber()).isEqualTo("123456789");
        assertThat(result.getNom()).isEqualTo("MIMBE");
        assertThat(result.getPrenom()).isEqualTo("LUCIEN YANNICK");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1986, 10, 19));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2035, 4, 20));
        assertThat(result.getSexe()).isEqualTo("M");
    }

    @Test
    void shouldParseNewFormatCameroonCni() {
        // New format: I<CMR prefix, NIC in optional data field
        String mrz = "I<CMR1002275191AA10340702<<<<<<\n" + "8610191M3504208CMR<<<<<<<<<<<<7\n" + "MIMBE<<LUCIEN<YANNICK<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("TD1");
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getDocumentType()).isEqualTo("CNI");
        // New format: NIC number extracted from optional data, not internal MRZ doc number
        assertThat(result.getDocumentNumber()).isEqualTo("AA10340702");
        assertThat(result.getNom()).isEqualTo("MIMBE");
        assertThat(result.getPrenom()).isEqualTo("LUCIEN YANNICK");
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1986, 10, 19));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2035, 4, 20));
        assertThat(result.getSexe()).isEqualTo("M");
    }

    @Test
    void shouldParseFranceCni() {
        // France CNI TD1 format
        String mrz = "IDFRA1234567891<<<<<<<<<<<<<<<\n" + "9001011F3001019FRA<<<<<<<<<<<<5\n" + "DUPONT<<MARIE<CLAIRE<<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("TD1");
        assertThat(result.getIssuingCountry()).isEqualTo("FRA");
        assertThat(result.getDocumentType()).isEqualTo("CNI");
        assertThat(result.getNom()).isEqualTo("DUPONT");
        assertThat(result.getPrenom()).isEqualTo("MARIE CLAIRE");
    }

    @Test
    void shouldParsePassport() {
        // Cameroon passport TD3 format
        String mrz = "P<CMRMIMBE<<LUCIEN<YANNICK<<<<<<<<<<<<<<<<<\n" + "AB12345671CMR8610191M3504201<<<<<<<<<<<<<<<0";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("TD3");
        assertThat(result.getDocumentType()).isEqualTo("PASSPORT");
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getNom()).isEqualTo("MIMBE");
        assertThat(result.getPrenom()).isEqualTo("LUCIEN YANNICK");
    }

    @Test
    void shouldHandleOcrArtifactsInMrz() {
        // OCR might produce « instead of <, or spaces between chars
        String mrz = "I«CMR1002275191AA10340702<<<<<<\n" + "8610191M3504208CMR<<<<<<<<<<<<7\n" + "MIMBE<<LUCIEN<YANNICK<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDocumentNumber()).isEqualTo("AA10340702");
    }

    @Test
    void shouldHandleMisreadAngleBracketAsK() {
        // OCR might read '<' as 'K' — flexible matching should still detect TD1
        String mrz = "IKCMR1002275191AA10340702<<<<<<\n" + "8610191M3504208CMR<<<<<<<<<<<<7\n" + "MIMBE<<LUCIEN<YANNICK<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo("TD1");
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getDocumentNumber()).isEqualTo("AA10340702");
        assertThat(result.getNom()).isEqualTo("MIMBE");
    }

    @Test
    void shouldHandleMisreadAngleBracketAsC() {
        // OCR might read '<' as 'C'
        String mrz = "ICCMR1002275191AA10340702<<<<<<\n" + "8610191M3504208CMR<<<<<<<<<<<<7\n" + "MIMBE<<LUCIEN<YANNICK<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getIssuingCountry()).isEqualTo("CMR");
        assertThat(result.getDocumentNumber()).isEqualTo("AA10340702");
    }

    @Test
    void shouldParseNewFormatWithRealOcrDateValues() {
        // Real MRZ from the test CNI card: DOB 29.10.1986, Expiry 25.04.2035
        String mrz = "I<CMR1002275191AA10340702<<<<<<\n" + "8610291M3504258CMR<<<<<<<<<<<<7\n" + "MIMBE<<LUCIEN<YANNICK<<<<<<<<<<<";

        MrzData result = parser.parseMrz(mrz);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDateNaissance()).isEqualTo(LocalDate.of(1986, 10, 29));
        assertThat(result.getDateExpiration()).isEqualTo(LocalDate.of(2035, 4, 25));
        assertThat(result.getSexe()).isEqualTo("M");
    }

    @Test
    void shouldReturnInvalidForEmptyText() {
        MrzData result = parser.parseMrz("");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldReturnInvalidForGarbageText() {
        MrzData result = parser.parseMrz("This is not an MRZ at all\nJust random text");
        assertThat(result.isValid()).isFalse();
    }
}
