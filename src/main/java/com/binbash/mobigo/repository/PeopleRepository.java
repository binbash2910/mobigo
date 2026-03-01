package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the People entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PeopleRepository extends JpaRepository<People, Long> {
    Optional<People> findByUser(User user);

    @Query(
        value = "SELECT p FROM People p LEFT JOIN FETCH p.user " +
        "WHERE p.cniStatut IS NOT NULL " +
        "AND (:cniStatut IS NULL OR p.cniStatut = CAST(:cniStatut AS string))",
        countQuery = "SELECT COUNT(p) FROM People p " +
        "WHERE p.cniStatut IS NOT NULL " +
        "AND (:cniStatut IS NULL OR p.cniStatut = CAST(:cniStatut AS string))"
    )
    Page<People> findAllWithCniSubmitted(@Param("cniStatut") String cniStatut, Pageable pageable);
}
