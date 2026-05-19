package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionStatus;
import com.binbash.mobigo.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WalletService {

    private static final Logger LOG = LoggerFactory.getLogger(WalletService.class);

    private final LedgerAccountRepository accountRepo;
    private final LedgerTransactionRepository txRepo;
    private final LedgerEntryRepository entryRepo;
    private final BookingRepository bookingRepo;
    private final PeopleRepository peopleRepo;
    private final CampayService campayService;
    private final AppSettingService appSettingService;

    public WalletService(
        LedgerAccountRepository accountRepo,
        LedgerTransactionRepository txRepo,
        LedgerEntryRepository entryRepo,
        BookingRepository bookingRepo,
        PeopleRepository peopleRepo,
        CampayService campayService,
        AppSettingService appSettingService
    ) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
        this.bookingRepo = bookingRepo;
        this.peopleRepo = peopleRepo;
        this.campayService = campayService;
        this.appSettingService = appSettingService;
    }

    static String accountKey(LedgerAccountType type, Long ownerPeopleId) {
        return ownerPeopleId == null ? type.name() : type.name() + ":" + ownerPeopleId;
    }

    public LedgerAccount getOrCreateAccount(LedgerAccountType type, Long ownerPeopleId) {
        String key = accountKey(type, ownerPeopleId);
        return accountRepo
            .findByAccountKey(key)
            .orElseGet(() -> {
                LOG.debug("Creating new LedgerAccount: key={}, type={}", key, type);
                LedgerAccount a = new LedgerAccount();
                a.setAccountKey(key);
                a.setAccountType(type);
                a.setOwnerPeopleId(ownerPeopleId);
                a.setBalance(BigDecimal.ZERO);
                try {
                    return accountRepo.save(a);
                } catch (DataIntegrityViolationException ex) {
                    // Concurrent first-touch lost the race; the winner's row now exists.
                    return accountRepo.findByAccountKey(key).orElseThrow(() -> ex);
                }
            });
    }

    @Transactional(readOnly = true)
    public BigDecimal availableBalance(String accountKey) {
        BigDecimal posted = accountRepo.findByAccountKey(accountKey).map(LedgerAccount::getBalance).orElse(BigDecimal.ZERO);
        // COALESCE in the query guarantees non-null; guard kept as defense-in-depth
        BigDecimal draftDebits = entryRepo.sumByAccountDirectionAndStatus(accountKey, LedgerDirection.DEBIT, LedgerTransactionStatus.DRAFT);
        return posted.subtract(draftDebits == null ? BigDecimal.ZERO : draftDebits);
    }

    static int toInt(BigDecimal b) {
        return b.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
