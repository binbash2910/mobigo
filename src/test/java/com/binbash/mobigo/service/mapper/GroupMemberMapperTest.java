package com.binbash.mobigo.service.mapper;

import static com.binbash.mobigo.domain.GroupMemberAsserts.*;
import static com.binbash.mobigo.domain.GroupMemberTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupMemberMapperTest {

    private GroupMemberMapper groupMemberMapper;

    @BeforeEach
    void setUp() {
        groupMemberMapper = new GroupMemberMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGroupMemberSample1();
        var actual = groupMemberMapper.toEntity(groupMemberMapper.toDto(expected));
        assertGroupMemberAllPropertiesEquals(expected, actual);
    }
}
