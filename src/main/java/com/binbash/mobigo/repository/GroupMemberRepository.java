package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the GroupMember entity.
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("select groupMember from GroupMember groupMember where groupMember.user.login = ?#{authentication.name}")
    List<GroupMember> findByUserIsCurrentUser();

    default Optional<GroupMember> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<GroupMember> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<GroupMember> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select groupMember from GroupMember groupMember left join fetch groupMember.user",
        countQuery = "select count(groupMember) from GroupMember groupMember"
    )
    Page<GroupMember> findAllWithToOneRelationships(Pageable pageable);

    @Query("select groupMember from GroupMember groupMember left join fetch groupMember.user")
    List<GroupMember> findAllWithToOneRelationships();

    @Query("select groupMember from GroupMember groupMember left join fetch groupMember.user where groupMember.id =:id")
    Optional<GroupMember> findOneWithToOneRelationships(@Param("id") Long id);
}
