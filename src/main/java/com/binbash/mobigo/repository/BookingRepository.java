package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import java.util.List;
import org.springframework.data.jpa.repository.*;
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
}
