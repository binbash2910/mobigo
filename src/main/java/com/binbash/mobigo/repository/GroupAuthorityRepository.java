package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.GroupAuthority;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the GroupAuthority entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GroupAuthorityRepository extends JpaRepository<GroupAuthority, Long> {}
