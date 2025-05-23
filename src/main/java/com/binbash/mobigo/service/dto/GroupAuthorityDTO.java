package com.binbash.mobigo.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.binbash.mobigo.domain.GroupAuthority} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GroupAuthorityDTO implements Serializable {

    private Long id;

    private AuthorityDTO authority;

    private GroupDTO group;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthorityDTO getAuthority() {
        return authority;
    }

    public void setAuthority(AuthorityDTO authority) {
        this.authority = authority;
    }

    public GroupDTO getGroup() {
        return group;
    }

    public void setGroup(GroupDTO group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupAuthorityDTO)) {
            return false;
        }

        GroupAuthorityDTO groupAuthorityDTO = (GroupAuthorityDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, groupAuthorityDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GroupAuthorityDTO{" +
            "id=" + getId() +
            ", authority=" + getAuthority() +
            ", group=" + getGroup() +
            "}";
    }
}
