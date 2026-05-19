package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.LedgerTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByExternalReference(String externalReference);

    List<LedgerTransaction> findByBookingId(Long bookingId);
}
