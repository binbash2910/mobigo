package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.LedgerTransaction;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByExternalReference(String externalReference);

    List<LedgerTransaction> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from LedgerTransaction t where t.idempotencyKey = :key")
    Optional<LedgerTransaction> lockByIdempotencyKey(@Param("key") String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from LedgerTransaction t where t.externalReference = :ref")
    Optional<LedgerTransaction> lockByExternalReference(@Param("ref") String ref);
}
