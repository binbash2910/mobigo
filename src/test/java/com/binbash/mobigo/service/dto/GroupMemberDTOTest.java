package com.binbash.mobigo.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GroupMemberDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GroupMemberDTO.class);
        GroupMemberDTO groupMemberDTO1 = new GroupMemberDTO();
        groupMemberDTO1.setId(1L);
        GroupMemberDTO groupMemberDTO2 = new GroupMemberDTO();
        assertThat(groupMemberDTO1).isNotEqualTo(groupMemberDTO2);
        groupMemberDTO2.setId(groupMemberDTO1.getId());
        assertThat(groupMemberDTO1).isEqualTo(groupMemberDTO2);
        groupMemberDTO2.setId(2L);
        assertThat(groupMemberDTO1).isNotEqualTo(groupMemberDTO2);
        groupMemberDTO1.setId(null);
        assertThat(groupMemberDTO1).isNotEqualTo(groupMemberDTO2);
    }
}
