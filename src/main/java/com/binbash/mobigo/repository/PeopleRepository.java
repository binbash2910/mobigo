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

    Optional<People> findByUserLogin(String login);

    @Query(
        value = "SELECT p FROM People p LEFT JOIN FETCH p.user " +
        "WHERE p.cniStatut IS NOT NULL " +
        "AND (:cniStatut IS NULL OR p.cniStatut = CAST(:cniStatut AS string))" +
        "AND (:search IS NULL OR LOWER(p.nom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(p.prenom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
        countQuery = "SELECT COUNT(p) FROM People p " +
        "WHERE p.cniStatut IS NOT NULL " +
        "AND (:cniStatut IS NULL OR p.cniStatut = CAST(:cniStatut AS string))" +
        "AND (:search IS NULL OR LOWER(p.nom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(p.prenom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))"
    )
    Page<People> findAllWithCniSubmitted(@Param("search") String search, @Param("cniStatut") String cniStatut, Pageable pageable);

    @Query(
        value = "SELECT p FROM People p LEFT JOIN FETCH p.user " +
        "WHERE p.cniStatut IS NOT NULL AND p.cniStatut <> 'VERIFIED' " +
        "AND (:search IS NULL OR LOWER(p.nom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(p.prenom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
        countQuery = "SELECT COUNT(p) FROM People p " +
        "WHERE p.cniStatut IS NOT NULL AND p.cniStatut <> 'VERIFIED' " +
        "AND (:search IS NULL OR LOWER(p.nom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(p.prenom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))"
    )
    Page<People> findAllNotVerified(@Param("search") String search, Pageable pageable);
}
