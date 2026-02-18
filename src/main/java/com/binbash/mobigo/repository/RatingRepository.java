package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.Rating;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Rating entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    @Query("SELECT r FROM Rating r WHERE r.conducteur.user.login = :login")
    List<Rating> findByConducteurUserLogin(@Param("login") String login);

    @Query("SELECT r FROM Rating r WHERE r.passager.user.login = :login")
    List<Rating> findByPassagerUserLogin(@Param("login") String login);
}
