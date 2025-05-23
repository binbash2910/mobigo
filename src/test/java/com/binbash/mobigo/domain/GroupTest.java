package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.GroupAuthorityTestSamples.*;
import static com.binbash.mobigo.domain.GroupMemberTestSamples.*;
import static com.binbash.mobigo.domain.GroupTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GroupTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Group.class);
        Group group1 = getGroupSample1();
        Group group2 = new Group();
        assertThat(group1).isNotEqualTo(group2);

        group2.setId(group1.getId());
        assertThat(group1).isEqualTo(group2);

        group2 = getGroupSample2();
        assertThat(group1).isNotEqualTo(group2);
    }

    @Test
    void groupMemberTest() {
        Group group = getGroupRandomSampleGenerator();
        GroupMember groupMemberBack = getGroupMemberRandomSampleGenerator();

        group.addGroupMember(groupMemberBack);
        assertThat(group.getGroupMembers()).containsOnly(groupMemberBack);
        assertThat(groupMemberBack.getGroup()).isEqualTo(group);

        group.removeGroupMember(groupMemberBack);
        assertThat(group.getGroupMembers()).doesNotContain(groupMemberBack);
        assertThat(groupMemberBack.getGroup()).isNull();

        group.groupMembers(new HashSet<>(Set.of(groupMemberBack)));
        assertThat(group.getGroupMembers()).containsOnly(groupMemberBack);
        assertThat(groupMemberBack.getGroup()).isEqualTo(group);

        group.setGroupMembers(new HashSet<>());
        assertThat(group.getGroupMembers()).doesNotContain(groupMemberBack);
        assertThat(groupMemberBack.getGroup()).isNull();
    }

    @Test
    void groupAuthorityTest() {
        Group group = getGroupRandomSampleGenerator();
        GroupAuthority groupAuthorityBack = getGroupAuthorityRandomSampleGenerator();

        group.addGroupAuthority(groupAuthorityBack);
        assertThat(group.getGroupAuthorities()).containsOnly(groupAuthorityBack);
        assertThat(groupAuthorityBack.getGroup()).isEqualTo(group);

        group.removeGroupAuthority(groupAuthorityBack);
        assertThat(group.getGroupAuthorities()).doesNotContain(groupAuthorityBack);
        assertThat(groupAuthorityBack.getGroup()).isNull();

        group.groupAuthorities(new HashSet<>(Set.of(groupAuthorityBack)));
        assertThat(group.getGroupAuthorities()).containsOnly(groupAuthorityBack);
        assertThat(groupAuthorityBack.getGroup()).isEqualTo(group);

        group.setGroupAuthorities(new HashSet<>());
        assertThat(group.getGroupAuthorities()).doesNotContain(groupAuthorityBack);
        assertThat(groupAuthorityBack.getGroup()).isNull();
    }
}
