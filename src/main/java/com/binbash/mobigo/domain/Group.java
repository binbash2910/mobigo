package com.binbash.mobigo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Group.
 */
@Entity
@Table(name = "jhi_group")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "group")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Group extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "group_name")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String groupName;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "group", "user" }, allowSetters = true)
    private Set<GroupMember> groupMembers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "authority", "group" }, allowSetters = true)
    private Set<GroupAuthority> groupAuthorities = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Group id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public Group groupName(String groupName) {
        this.setGroupName(groupName);
        return this;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Set<GroupMember> getGroupMembers() {
        return this.groupMembers;
    }

    public void setGroupMembers(Set<GroupMember> groupMembers) {
        if (this.groupMembers != null) {
            this.groupMembers.forEach(i -> i.setGroup(null));
        }
        if (groupMembers != null) {
            groupMembers.forEach(i -> i.setGroup(this));
        }
        this.groupMembers = groupMembers;
    }

    public Group groupMembers(Set<GroupMember> groupMembers) {
        this.setGroupMembers(groupMembers);
        return this;
    }

    public Group addGroupMember(GroupMember groupMember) {
        this.groupMembers.add(groupMember);
        groupMember.setGroup(this);
        return this;
    }

    public Group removeGroupMember(GroupMember groupMember) {
        this.groupMembers.remove(groupMember);
        groupMember.setGroup(null);
        return this;
    }

    public Set<GroupAuthority> getGroupAuthorities() {
        return this.groupAuthorities;
    }

    public void setGroupAuthorities(Set<GroupAuthority> groupAuthorities) {
        if (this.groupAuthorities != null) {
            this.groupAuthorities.forEach(i -> i.setGroup(null));
        }
        if (groupAuthorities != null) {
            groupAuthorities.forEach(i -> i.setGroup(this));
        }
        this.groupAuthorities = groupAuthorities;
    }

    public Group groupAuthorities(Set<GroupAuthority> groupAuthorities) {
        this.setGroupAuthorities(groupAuthorities);
        return this;
    }

    public Group addGroupAuthority(GroupAuthority groupAuthority) {
        this.groupAuthorities.add(groupAuthority);
        groupAuthority.setGroup(this);
        return this;
    }

    public Group removeGroupAuthority(GroupAuthority groupAuthority) {
        this.groupAuthorities.remove(groupAuthority);
        groupAuthority.setGroup(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Group)) {
            return false;
        }
        return getId() != null && getId().equals(((Group) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Group{" +
            "id=" + getId() +
            ", groupName='" + getGroupName() + "'" +
            "}";
    }
}
