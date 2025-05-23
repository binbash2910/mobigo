package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.GroupAuthorityTestSamples.*;
import static com.binbash.mobigo.domain.GroupTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GroupAuthorityTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GroupAuthority.class);
        GroupAuthority groupAuthority1 = getGroupAuthoritySample1();
        GroupAuthority groupAuthority2 = new GroupAuthority();
        assertThat(groupAuthority1).isNotEqualTo(groupAuthority2);

        groupAuthority2.setId(groupAuthority1.getId());
        assertThat(groupAuthority1).isEqualTo(groupAuthority2);

        groupAuthority2 = getGroupAuthoritySample2();
        assertThat(groupAuthority1).isNotEqualTo(groupAuthority2);
    }

    @Test
    void groupTest() {
        GroupAuthority groupAuthority = getGroupAuthorityRandomSampleGenerator();
        Group groupBack = getGroupRandomSampleGenerator();

        groupAuthority.setGroup(groupBack);
        assertThat(groupAuthority.getGroup()).isEqualTo(groupBack);

        groupAuthority.group(null);
        assertThat(groupAuthority.getGroup()).isNull();
    }
}
