package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingPassagerUserLogin(String login);

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingPassagerId(Long passagerId);
}
