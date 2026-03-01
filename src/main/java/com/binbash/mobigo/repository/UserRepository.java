package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findOneByActivationKey(String activationKey);
    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);
    Optional<User> findOneByResetKey(String resetKey);
    Optional<User> findOneByEmailIgnoreCase(String email);
    Optional<User> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @Query(
        "SELECT u FROM User u WHERE " +
        "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
        "AND (:activated IS NULL OR u.activated = :activated)"
    )
    Page<User> findAllWithFilters(@Param("search") String search, @Param("activated") Boolean activated, Pageable pageable);
}
