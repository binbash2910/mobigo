package com.binbash.mobigo.service.mapper;

import com.binbash.mobigo.domain.Authority;
import com.binbash.mobigo.domain.Group;
import com.binbash.mobigo.domain.GroupAuthority;
import com.binbash.mobigo.service.dto.AuthorityDTO;
import com.binbash.mobigo.service.dto.GroupAuthorityDTO;
import com.binbash.mobigo.service.dto.GroupDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GroupAuthority} and its DTO {@link GroupAuthorityDTO}.
 */
@Mapper(componentModel = "spring")
public interface GroupAuthorityMapper extends EntityMapper<GroupAuthorityDTO, GroupAuthority> {
    @Mapping(target = "authority", source = "authority", qualifiedByName = "authorityName")
    @Mapping(target = "group", source = "group", qualifiedByName = "groupId")
    GroupAuthorityDTO toDto(GroupAuthority s);

    @Named("authorityName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    AuthorityDTO toDtoAuthorityName(Authority authority);

    @Named("groupId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    GroupDTO toDtoGroupId(Group group);
}
