package com.binbash.mobigo.service.mapper;

import static com.binbash.mobigo.domain.GroupAuthorityAsserts.*;
import static com.binbash.mobigo.domain.GroupAuthorityTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupAuthorityMapperTest {

    private GroupAuthorityMapper groupAuthorityMapper;

    @BeforeEach
    void setUp() {
        groupAuthorityMapper = new GroupAuthorityMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGroupAuthoritySample1();
        var actual = groupAuthorityMapper.toEntity(groupAuthorityMapper.toDto(expected));
        assertGroupAuthorityAllPropertiesEquals(expected, actual);
    }
}
