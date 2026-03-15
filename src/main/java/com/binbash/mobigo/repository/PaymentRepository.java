package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.domain.enumeration.PaymentStatusEnum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
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

    Optional<Payment> findByExternalReference(String externalReference);

    Optional<Payment> findByCampayTransactionId(String campayTransactionId);

    List<Payment> findByStatut(PaymentStatusEnum statut);

    @Query("SELECT COUNT(p) FROM Payment p")
    long countAll();

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Payment p WHERE p.statut = 'COLLECTE_REUSSIE' OR p.statut = 'REUSSI'")
    double sumCollectedAmount();

    @Query("SELECT COALESCE(SUM(p.commissionPlateforme), 0) FROM Payment p WHERE p.statut = 'REUSSI'")
    double sumCommissionPlateforme();

    @Query("SELECT COALESCE(SUM(p.fraisCampay), 0) FROM Payment p WHERE p.statut = 'REUSSI'")
    double sumFraisCampay();

    @Query("SELECT COALESCE(SUM(p.revenuNetPlateforme), 0) FROM Payment p WHERE p.statut = 'REUSSI'")
    double sumRevenuNetPlateforme();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.statut = :statut")
    long countByStatutValue(@Param("statut") PaymentStatusEnum statut);
}
