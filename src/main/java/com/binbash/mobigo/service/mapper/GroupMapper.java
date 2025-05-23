package com.binbash.mobigo.service.mapper;

import com.binbash.mobigo.domain.Group;
import com.binbash.mobigo.service.dto.GroupDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Group} and its DTO {@link GroupDTO}.
 */
@Mapper(componentModel = "spring")
public interface GroupMapper extends EntityMapper<GroupDTO, Group> {}
