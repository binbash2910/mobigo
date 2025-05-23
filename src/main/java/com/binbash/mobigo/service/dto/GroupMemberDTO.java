package com.binbash.mobigo.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.binbash.mobigo.domain.GroupMember} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GroupMemberDTO implements Serializable {

    private Long id;

    private GroupDTO group;

    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GroupDTO getGroup() {
        return group;
    }

    public void setGroup(GroupDTO group) {
        this.group = group;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupMemberDTO)) {
            return false;
        }

        GroupMemberDTO groupMemberDTO = (GroupMemberDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, groupMemberDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GroupMemberDTO{" +
            "id=" + getId() +
            ", group=" + getGroup() +
            ", user=" + getUser() +
            "}";
    }
}
