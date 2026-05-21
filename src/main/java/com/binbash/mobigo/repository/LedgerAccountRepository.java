package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByAccountKey(String accountKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LedgerAccount a where a.accountKey = :key")
    Optional<LedgerAccount> lockByAccountKey(@Param("key") String key);

    List<LedgerAccount> findByAccountTypeAndBalanceGreaterThanEqual(LedgerAccountType accountType, BigDecimal balance);
}
