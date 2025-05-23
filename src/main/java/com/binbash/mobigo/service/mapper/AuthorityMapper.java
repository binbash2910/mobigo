package com.binbash.mobigo.service.mapper;

import com.binbash.mobigo.domain.Authority;
import com.binbash.mobigo.domain.Group;
import com.binbash.mobigo.service.dto.AuthorityDTO;
import com.binbash.mobigo.service.dto.GroupDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link Group} and its DTO {@link GroupDTO}.
 */
@Mapper(componentModel = "spring")
public interface AuthorityMapper extends EntityMapper<AuthorityDTO, Authority> {}
