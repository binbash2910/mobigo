package com.binbash.mobigo.service.mapper;

import static com.binbash.mobigo.domain.GroupAsserts.*;
import static com.binbash.mobigo.domain.GroupTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupMapperTest {

    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupMapper = new GroupMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGroupSample1();
        var actual = groupMapper.toEntity(groupMapper.toDto(expected));
        assertGroupAllPropertiesEquals(expected, actual);
    }
}
