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
