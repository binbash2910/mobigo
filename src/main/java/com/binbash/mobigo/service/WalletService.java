package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.LedgerEntry;
import com.binbash.mobigo.domain.LedgerTransaction;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionStatus;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionType;
import com.binbash.mobigo.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate txTemplate;

    public WalletService(
        LedgerAccountRepository accountRepo,
        LedgerTransactionRepository txRepo,
        LedgerEntryRepository entryRepo,
        BookingRepository bookingRepo,
        PeopleRepository peopleRepo,
        CampayService campayService,
        AppSettingService appSettingService,
        PlatformTransactionManager transactionManager
    ) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
        this.bookingRepo = bookingRepo;
        this.peopleRepo = peopleRepo;
        this.campayService = campayService;
        this.appSettingService = appSettingService;
        this.txTemplate = new TransactionTemplate(transactionManager);
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

    public void holdForBooking(Booking booking) {
        Long bookingId = booking.getId();
        String idem = "SETTLE-" + bookingId;
        if (txRepo.findByIdempotencyKey(idem).isPresent()) {
            LOG.debug("holdForBooking idempotent skip for booking {}", bookingId);
            return;
        }

        BigDecimal total = BigDecimal.valueOf(booking.getMontantTotal()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal commission = booking.getCommission() == null
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(booking.getCommission()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal net = total.subtract(commission);

        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Commission exceeds total for booking " + bookingId + ": total=" + total + ", commission=" + commission
            );
        }

        if (booking.getPassager() == null || booking.getPassager().getId() == null) {
            throw new IllegalStateException("No passenger resolvable for booking " + bookingId);
        }
        Long passengerId = booking.getPassager().getId();
        People driver = booking.getTrajet() != null && booking.getTrajet().getVehicule() != null
            ? booking.getTrajet().getVehicule().getProprietaire()
            : null;
        if (driver == null) {
            throw new IllegalStateException("No driver resolvable for booking " + bookingId);
        }

        String passKey = accountKey(LedgerAccountType.PASSENGER, passengerId);
        BigDecimal available = availableBalance(passKey);
        if (available.compareTo(total) < 0) {
            throw new InsufficientWalletBalanceException(total.subtract(available));
        }

        LedgerAccount passenger = getOrCreateAccount(LedgerAccountType.PASSENGER, passengerId);
        LedgerAccount escrow = getOrCreateAccount(LedgerAccountType.ESCROW, null);
        LedgerAccount driverAcc = getOrCreateAccount(LedgerAccountType.DRIVER, driver.getId());
        LedgerAccount platform = getOrCreateAccount(LedgerAccountType.PLATFORM, null);

        LedgerTransaction tx = new LedgerTransaction();
        tx.setType(LedgerTransactionType.BOOKING_SETTLEMENT);
        tx.setStatus(LedgerTransactionStatus.DRAFT);
        tx.setBookingId(bookingId);
        tx.setIdempotencyKey(idem);
        tx.setDescription("Réservation #" + bookingId);
        tx.addEntry(LedgerEntry.of(passenger, LedgerDirection.DEBIT, total));
        tx.addEntry(LedgerEntry.of(escrow, LedgerDirection.CREDIT, total));
        tx.addEntry(LedgerEntry.of(escrow, LedgerDirection.DEBIT, net));
        tx.addEntry(LedgerEntry.of(driverAcc, LedgerDirection.CREDIT, net));
        tx.addEntry(LedgerEntry.of(escrow, LedgerDirection.DEBIT, commission));
        tx.addEntry(LedgerEntry.of(platform, LedgerDirection.CREDIT, commission));
        txRepo.save(tx);
        LOG.info("Hold DRAFT created for booking {} total={} net={} commission={}", bookingId, total, net, commission);
    }

    public void confirmBookingSettlement(Long bookingId) {
        LedgerTransaction tx = txRepo.lockByIdempotencyKey("SETTLE-" + bookingId).orElse(null);
        if (tx == null) {
            LOG.debug("confirmBookingSettlement: no settlement for booking {}", bookingId);
            return;
        }
        if (tx.getStatus() != LedgerTransactionStatus.DRAFT) {
            LOG.debug("confirmBookingSettlement idempotent skip booking {} status {}", bookingId, tx.getStatus());
            return;
        }
        applyEntries(tx);
        tx.setStatus(LedgerTransactionStatus.POSTED);
        txRepo.save(tx);
        LOG.info("Settlement POSTED for booking {}", bookingId);
    }

    public void voidBookingSettlement(Long bookingId) {
        LedgerTransaction tx = txRepo.lockByIdempotencyKey("SETTLE-" + bookingId).orElse(null);
        if (tx == null) {
            LOG.debug("voidBookingSettlement: no settlement for booking {}", bookingId);
            return;
        }
        if (tx.getStatus() == LedgerTransactionStatus.VOID) {
            LOG.debug("voidBookingSettlement idempotent skip booking {} status VOID", bookingId);
            return;
        }
        if (tx.getStatus() == LedgerTransactionStatus.POSTED) {
            throw new IllegalStateException("Cannot void a POSTED settlement for booking " + bookingId);
        }
        tx.setStatus(LedgerTransactionStatus.VOID);
        txRepo.save(tx);
        LOG.info("Settlement VOID for booking {}", bookingId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LedgerTransaction rechargeWallet(Long passengerPeopleId, BigDecimal netAmount, String phone) {
        if (netAmount.signum() <= 0) {
            throw new IllegalArgumentException("Recharge amount must be positive");
        }
        double feeRate = appSettingService.getCampayFeeRate();
        BigDecimal fee = netAmount.multiply(BigDecimal.valueOf(feeRate)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal gross = netAmount.add(fee);
        final String extRef = "RCG-" + UUID.randomUUID();

        // 1. Persist the DRAFT in its own committed transaction BEFORE calling Campay.
        LedgerTransaction draft = txTemplate.execute(st -> {
            LedgerAccount external = getOrCreateAccount(LedgerAccountType.EXTERNAL, null);
            LedgerAccount passenger = getOrCreateAccount(LedgerAccountType.PASSENGER, passengerPeopleId);
            LedgerTransaction t = new LedgerTransaction();
            t.setType(LedgerTransactionType.RECHARGE);
            t.setStatus(LedgerTransactionStatus.DRAFT);
            t.setExternalReference(extRef);
            t.setDescription("Recharge porte-monnaie (" + netAmount + " net, frais " + fee + ")");
            t.addEntry(LedgerEntry.of(external, LedgerDirection.DEBIT, netAmount));
            t.addEntry(LedgerEntry.of(passenger, LedgerDirection.CREDIT, netAmount));
            return txRepo.save(t);
        });

        // 2. Call Campay OUTSIDE any DB transaction (no connection held during network I/O).
        try {
            CampayService.CollectResponse resp = campayService.collect(phone, toInt(gross), extRef, "Recharge Mobigo #" + extRef);
            // 3. Attach the Campay reference in its own short transaction.
            txTemplate.execute(st -> {
                LedgerTransaction managed = txRepo.findByExternalReference(extRef).orElse(draft);
                managed.setCampayReference(resp.reference());
                return txRepo.save(managed);
            });
            draft.setCampayReference(resp.reference());
            return draft;
        } catch (Exception e) {
            LOG.error("Campay collect failed for recharge {}", extRef, e);
            // DRAFT was already committed; VOID it so it does not linger.
            txTemplate.execute(st -> {
                txRepo
                    .findByExternalReference(extRef)
                    .ifPresent(t -> {
                        t.setStatus(LedgerTransactionStatus.VOID);
                        txRepo.save(t);
                    });
                return null;
            });
            throw new RuntimeException("Échec de l'initiation de la recharge: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LedgerTransaction requestPayout(Long driverPeopleId, BigDecimal amount, String phone) {
        BigDecimal min = appSettingService.getMinWithdrawal();
        if (amount.compareTo(min) < 0) {
            throw new IllegalArgumentException("Montant inférieur au minimum de retrait (" + min + ")");
        }
        double feeRate = appSettingService.getCampayFeeRate();
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(feeRate)).setScale(0, RoundingMode.HALF_UP);
        final String extRef = "WDR-" + UUID.randomUUID();
        final String driverKey = accountKey(LedgerAccountType.DRIVER, driverPeopleId);

        // 1. Atomically: lock the driver account, re-check available balance, persist the DRAFT
        //    (committed before any Campay call). The driver-account pessimistic lock serializes
        //    concurrent payout requests so two cannot both pass the balance check.
        LedgerTransaction draft = txTemplate.execute(st -> {
            accountRepo.lockByAccountKey(driverKey);
            BigDecimal available = availableBalance(driverKey);
            if (available.compareTo(amount) < 0) {
                throw new InsufficientWalletBalanceException(amount.subtract(available));
            }
            LedgerAccount driver = getOrCreateAccount(LedgerAccountType.DRIVER, driverPeopleId);
            LedgerAccount external = getOrCreateAccount(LedgerAccountType.EXTERNAL, null);
            LedgerAccount platform = getOrCreateAccount(LedgerAccountType.PLATFORM, null);
            LedgerTransaction t = new LedgerTransaction();
            t.setType(LedgerTransactionType.WITHDRAWAL);
            t.setStatus(LedgerTransactionStatus.DRAFT);
            t.setExternalReference(extRef);
            t.setDescription("Retrait conducteur " + amount + " (frais plateforme " + fee + ")");
            t.addEntry(LedgerEntry.of(driver, LedgerDirection.DEBIT, amount));
            t.addEntry(LedgerEntry.of(external, LedgerDirection.CREDIT, amount));
            t.addEntry(LedgerEntry.of(platform, LedgerDirection.DEBIT, fee));
            t.addEntry(LedgerEntry.of(external, LedgerDirection.CREDIT, fee));
            return txRepo.save(t);
        });

        // 2. Call Campay OUTSIDE any DB transaction (no connection held during network I/O).
        try {
            CampayService.DisbursementResponse resp = campayService.disburse(phone, toInt(amount), extRef, "Versement Mobigo #" + extRef);
            // 3. Attach the Campay reference in its own short transaction.
            txTemplate.execute(st -> {
                LedgerTransaction managed = txRepo.findByExternalReference(extRef).orElse(draft);
                managed.setCampayReference(resp.reference());
                return txRepo.save(managed);
            });
            draft.setCampayReference(resp.reference());
            return draft;
        } catch (Exception e) {
            LOG.error("Campay disburse failed for payout {}", extRef, e);
            // DRAFT was already committed; VOID it so it does not linger.
            txTemplate.execute(st -> {
                txRepo
                    .findByExternalReference(extRef)
                    .ifPresent(t -> {
                        t.setStatus(LedgerTransactionStatus.VOID);
                        txRepo.save(t);
                    });
                return null;
            });
            throw new RuntimeException("Échec de l'initiation du retrait: " + e.getMessage(), e);
        }
    }

    public void handleCampayCallback(String externalReference, String status) {
        LedgerTransaction tx = txRepo.lockByExternalReference(externalReference).orElse(null);
        if (tx == null) {
            LOG.warn("Campay callback: no ledger tx for externalReference {}", externalReference);
            return;
        }
        if (tx.getStatus() != LedgerTransactionStatus.DRAFT) {
            LOG.debug("Campay callback idempotent skip {} status {}", externalReference, tx.getStatus());
            return;
        }
        String s = status == null ? "" : status.toUpperCase();
        if (s.contains("SUCCESS")) {
            applyEntries(tx);
            tx.setStatus(LedgerTransactionStatus.POSTED);
            txRepo.save(tx);
            LOG.info("Campay tx {} POSTED ({})", externalReference, tx.getType());
        } else if (s.contains("FAIL") || s.contains("CANCEL") || s.contains("EXPIR") || s.contains("REJECT")) {
            tx.setStatus(LedgerTransactionStatus.VOID);
            txRepo.save(tx);
            LOG.info("Campay tx {} VOID ({})", externalReference, tx.getType());
        } else {
            LOG.debug("Campay callback {} non-terminal status {}", externalReference, status);
        }
    }

    /** Applique chaque ligne au solde du compte (verrou pessimiste). */
    private void applyEntries(LedgerTransaction tx) {
        // Lock each DISTINCT account exactly once, in a deterministic global order
        // (sorted by accountKey) so concurrent transactions touching shared accounts
        // (ESCROW, PLATFORM) can never form a lock-ordering deadlock cycle.
        Map<String, LedgerAccount> locked = new LinkedHashMap<>();
        tx
            .getEntries()
            .stream()
            .map(e -> e.getAccount().getAccountKey())
            .distinct()
            .sorted()
            .forEach(key ->
                locked.put(key, accountRepo.lockByAccountKey(key).orElseThrow(() -> new IllegalStateException("Account not found: " + key)))
            );
        // Apply deltas in original entry order on the locked instances.
        for (LedgerEntry e : tx.getEntries()) {
            LedgerAccount acc = locked.get(e.getAccount().getAccountKey());
            BigDecimal delta = e.getDirection() == LedgerDirection.CREDIT ? e.getAmount() : e.getAmount().negate();
            acc.setBalance(acc.getBalance().add(delta));
        }
        // Persist each distinct locked account once.
        locked.values().forEach(accountRepo::save);
    }
}
