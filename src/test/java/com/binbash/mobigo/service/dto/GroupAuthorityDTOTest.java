package com.binbash.mobigo.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GroupAuthorityDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GroupAuthorityDTO.class);
        GroupAuthorityDTO groupAuthorityDTO1 = new GroupAuthorityDTO();
        groupAuthorityDTO1.setId(1L);
        GroupAuthorityDTO groupAuthorityDTO2 = new GroupAuthorityDTO();
        assertThat(groupAuthorityDTO1).isNotEqualTo(groupAuthorityDTO2);
        groupAuthorityDTO2.setId(groupAuthorityDTO1.getId());
        assertThat(groupAuthorityDTO1).isEqualTo(groupAuthorityDTO2);
        groupAuthorityDTO2.setId(2L);
        assertThat(groupAuthorityDTO1).isNotEqualTo(groupAuthorityDTO2);
        groupAuthorityDTO1.setId(null);
        assertThat(groupAuthorityDTO1).isNotEqualTo(groupAuthorityDTO2);
    }
}
