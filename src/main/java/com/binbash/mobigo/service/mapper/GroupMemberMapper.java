package com.binbash.mobigo.service.mapper;

import com.binbash.mobigo.domain.Group;
import com.binbash.mobigo.domain.GroupMember;
import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.service.dto.GroupDTO;
import com.binbash.mobigo.service.dto.GroupMemberDTO;
import com.binbash.mobigo.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GroupMember} and its DTO {@link GroupMemberDTO}.
 */
@Mapper(componentModel = "spring")
public interface GroupMemberMapper extends EntityMapper<GroupMemberDTO, GroupMember> {
    @Mapping(target = "group", source = "group", qualifiedByName = "groupId")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    GroupMemberDTO toDto(GroupMember s);

    @Named("groupId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    GroupDTO toDtoGroupId(Group group);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
