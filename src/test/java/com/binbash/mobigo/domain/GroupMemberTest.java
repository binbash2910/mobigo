package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.GroupMemberTestSamples.*;
import static com.binbash.mobigo.domain.GroupTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GroupMemberTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GroupMember.class);
        GroupMember groupMember1 = getGroupMemberSample1();
        GroupMember groupMember2 = new GroupMember();
        assertThat(groupMember1).isNotEqualTo(groupMember2);

        groupMember2.setId(groupMember1.getId());
        assertThat(groupMember1).isEqualTo(groupMember2);

        groupMember2 = getGroupMemberSample2();
        assertThat(groupMember1).isNotEqualTo(groupMember2);
    }

    @Test
    void groupTest() {
        GroupMember groupMember = getGroupMemberRandomSampleGenerator();
        Group groupBack = getGroupRandomSampleGenerator();

        groupMember.setGroup(groupBack);
        assertThat(groupMember.getGroup()).isEqualTo(groupBack);

        groupMember.group(null);
        assertThat(groupMember.getGroup()).isNull();
    }
}
