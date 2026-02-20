package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Booking entity.
 */
@SuppressWarnings("unused")
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPassagerUserLogin(String login);

    List<Booking> findByTrajetId(Long rideId);

    List<Booking> findByTrajetIdAndStatut(Long rideId, BookingStatusEnum statut);

    @Query(
        "SELECT b FROM Booking b " +
        "JOIN FETCH b.passager p " +
        "LEFT JOIN FETCH p.user " +
        "JOIN FETCH b.trajet r " +
        "LEFT JOIN FETCH r.vehicule v " +
        "LEFT JOIN FETCH v.proprietaire prop " +
        "LEFT JOIN FETCH prop.user " +
        "WHERE b.id = :id"
    )
    Optional<Booking> findByIdWithRelations(@Param("id") Long id);

    @Query(
        "SELECT b FROM Booking b " +
        "JOIN FETCH b.passager p " +
        "LEFT JOIN FETCH p.user " +
        "JOIN FETCH b.trajet r " +
        "LEFT JOIN FETCH r.vehicule v " +
        "LEFT JOIN FETCH v.proprietaire prop " +
        "LEFT JOIN FETCH prop.user " +
        "WHERE r.id = :rideId"
    )
    List<Booking> findByTrajetIdWithRelations(@Param("rideId") Long rideId);

    @Query("SELECT b FROM Booking b JOIN b.trajet r WHERE r.createdBy = :login")
    List<Booking> findByTrajetCreatedBy(@Param("login") String login);
}
