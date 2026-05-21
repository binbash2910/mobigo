package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.LedgerEntry;
import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query(
        "select coalesce(sum(e.amount), 0) from LedgerEntry e " +
        "where e.account.accountKey = :key and e.direction = :dir and e.transaction.status = :status"
    )
    BigDecimal sumByAccountDirectionAndStatus(
        @Param("key") String key,
        @Param("dir") LedgerDirection dir,
        @Param("status") LedgerTransactionStatus status
    );

    List<LedgerEntry> findByAccount_AccountKeyOrderByIdDesc(String accountKey);
}
