package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.LedgerTransaction;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionStatus;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionType;
import com.binbash.mobigo.repository.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private LedgerAccountRepository accountRepo;

    @Mock
    private LedgerTransactionRepository txRepo;

    @Mock
    private LedgerEntryRepository entryRepo;

    @Mock
    private BookingRepository bookingRepo;

    @Mock
    private PeopleRepository peopleRepo;

    @Mock
    private CampayService campayService;

    @Mock
    private AppSettingService appSettingService;

    WalletService wallet;

    @BeforeEach
    void setUp() {
        wallet = new WalletService(accountRepo, txRepo, entryRepo, bookingRepo, peopleRepo, campayService, appSettingService);
    }

    @Test
    void getOrCreateAccountCreatesWhenMissing() {
        when(accountRepo.findByAccountKey("PASSENGER:42")).thenReturn(Optional.empty());
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));

        LedgerAccount acc = wallet.getOrCreateAccount(LedgerAccountType.PASSENGER, 42L);

        assertThat(acc.getAccountKey()).isEqualTo("PASSENGER:42");
        assertThat(acc.getAccountType()).isEqualTo(LedgerAccountType.PASSENGER);
        assertThat(acc.getOwnerPeopleId()).isEqualTo(42L);
        assertThat(acc.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void singletonAccountKeyHasNoOwnerSuffix() {
        when(accountRepo.findByAccountKey("ESCROW")).thenReturn(Optional.empty());
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));

        LedgerAccount acc = wallet.getOrCreateAccount(LedgerAccountType.ESCROW, null);

        assertThat(acc.getAccountKey()).isEqualTo("ESCROW");
        assertThat(acc.getOwnerPeopleId()).isNull();
    }

    @Test
    void availableBalanceSubtractsDraftDebits() {
        LedgerAccount acc = new LedgerAccount();
        acc.setAccountKey("PASSENGER:7");
        acc.setBalance(new BigDecimal("10000"));
        when(accountRepo.findByAccountKey("PASSENGER:7")).thenReturn(Optional.of(acc));
        when(entryRepo.sumByAccountDirectionAndStatus("PASSENGER:7", LedgerDirection.DEBIT, LedgerTransactionStatus.DRAFT)).thenReturn(
            new BigDecimal("3000")
        );

        BigDecimal available = wallet.availableBalance("PASSENGER:7");

        assertThat(available).isEqualByComparingTo(new BigDecimal("7000"));
    }

    @Test
    void getOrCreateAccountReturnsExistingAccount() {
        LedgerAccount existing = new LedgerAccount();
        existing.setAccountKey("PASSENGER:99");
        when(accountRepo.findByAccountKey("PASSENGER:99")).thenReturn(Optional.of(existing));

        LedgerAccount result = wallet.getOrCreateAccount(LedgerAccountType.PASSENGER, 99L);

        assertThat(result).isSameAs(existing);
        verify(accountRepo, never()).save(any());
    }

    private com.binbash.mobigo.domain.Booking bookingFixture(long id, float total, Float commission, long passengerId, long driverId) {
        com.binbash.mobigo.domain.People passenger = new com.binbash.mobigo.domain.People();
        passenger.setId(passengerId);
        com.binbash.mobigo.domain.People driver = new com.binbash.mobigo.domain.People();
        driver.setId(driverId);
        com.binbash.mobigo.domain.Vehicle v = new com.binbash.mobigo.domain.Vehicle();
        v.setProprietaire(driver);
        com.binbash.mobigo.domain.Ride ride = new com.binbash.mobigo.domain.Ride();
        ride.setVehicule(v);
        com.binbash.mobigo.domain.Booking b = new com.binbash.mobigo.domain.Booking();
        b.setId(id);
        b.setMontantTotal(total);
        b.setCommission(commission);
        b.setPassager(passenger);
        b.setTrajet(ride);
        return b;
    }

    @Test
    void holdForBookingCreatesDraftSettlementWhenSolvent() {
        com.binbash.mobigo.domain.Booking b = bookingFixture(100L, 6000f, 1000f, 7L, 9L);
        LedgerAccount pass = new LedgerAccount();
        pass.setAccountKey("PASSENGER:7");
        pass.setBalance(new BigDecimal("10000"));
        when(entryRepo.sumByAccountDirectionAndStatus("PASSENGER:7", LedgerDirection.DEBIT, LedgerTransactionStatus.DRAFT)).thenReturn(
            BigDecimal.ZERO
        );
        when(txRepo.findByIdempotencyKey("SETTLE-100")).thenReturn(Optional.empty());
        when(accountRepo.findByAccountKey(any())).thenAnswer(i -> {
            String k = i.getArgument(0);
            if (k.equals("PASSENGER:7")) return Optional.of(pass);
            return Optional.empty();
        });
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(txRepo.save(any(LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));

        wallet.holdForBooking(b);

        org.mockito.ArgumentCaptor<LedgerTransaction> cap = org.mockito.ArgumentCaptor.forClass(LedgerTransaction.class);
        verify(txRepo).save(cap.capture());
        LedgerTransaction tx = cap.getValue();
        assertThat(tx.getType()).isEqualTo(LedgerTransactionType.BOOKING_SETTLEMENT);
        assertThat(tx.getStatus()).isEqualTo(LedgerTransactionStatus.DRAFT);
        assertThat(tx.getIdempotencyKey()).isEqualTo("SETTLE-100");
        assertThat(tx.getEntries()).hasSize(6);
        BigDecimal debitSum = tx
            .getEntries()
            .stream()
            .filter(e -> e.getDirection() == LedgerDirection.DEBIT)
            .map(com.binbash.mobigo.domain.LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditSum = tx
            .getEntries()
            .stream()
            .filter(e -> e.getDirection() == LedgerDirection.CREDIT)
            .map(com.binbash.mobigo.domain.LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debitSum).isEqualByComparingTo(creditSum);
    }

    @Test
    void holdForBookingThrowsWhenInsufficient() {
        com.binbash.mobigo.domain.Booking b = bookingFixture(101L, 6000f, 1000f, 7L, 9L);
        LedgerAccount pass = new LedgerAccount();
        pass.setAccountKey("PASSENGER:7");
        pass.setBalance(new BigDecimal("1000"));
        when(txRepo.findByIdempotencyKey("SETTLE-101")).thenReturn(Optional.empty());
        when(accountRepo.findByAccountKey("PASSENGER:7")).thenReturn(Optional.of(pass));
        when(entryRepo.sumByAccountDirectionAndStatus("PASSENGER:7", LedgerDirection.DEBIT, LedgerTransactionStatus.DRAFT)).thenReturn(
            BigDecimal.ZERO
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> wallet.holdForBooking(b)).isInstanceOf(
            InsufficientWalletBalanceException.class
        );
    }

    @Test
    void holdForBookingHandlesNullCommission() {
        com.binbash.mobigo.domain.Booking b = bookingFixture(103L, 6000f, null, 7L, 9L);
        LedgerAccount pass = new LedgerAccount();
        pass.setAccountKey("PASSENGER:7");
        pass.setBalance(new BigDecimal("10000"));
        when(entryRepo.sumByAccountDirectionAndStatus("PASSENGER:7", LedgerDirection.DEBIT, LedgerTransactionStatus.DRAFT)).thenReturn(
            BigDecimal.ZERO
        );
        when(txRepo.findByIdempotencyKey("SETTLE-103")).thenReturn(Optional.empty());
        when(accountRepo.findByAccountKey(any())).thenAnswer(i -> {
            String k = i.getArgument(0);
            if (k.equals("PASSENGER:7")) return Optional.of(pass);
            return Optional.empty();
        });
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(txRepo.save(any(com.binbash.mobigo.domain.LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));

        wallet.holdForBooking(b);

        org.mockito.ArgumentCaptor<com.binbash.mobigo.domain.LedgerTransaction> cap = org.mockito.ArgumentCaptor.forClass(
            com.binbash.mobigo.domain.LedgerTransaction.class
        );
        verify(txRepo).save(cap.capture());
        com.binbash.mobigo.domain.LedgerTransaction tx = cap.getValue();
        BigDecimal debitSum = tx
            .getEntries()
            .stream()
            .filter(e -> e.getDirection() == LedgerDirection.DEBIT)
            .map(com.binbash.mobigo.domain.LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditSum = tx
            .getEntries()
            .stream()
            .filter(e -> e.getDirection() == LedgerDirection.CREDIT)
            .map(com.binbash.mobigo.domain.LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debitSum).isEqualByComparingTo(creditSum);
        assertThat(tx.getEntries()).hasSize(6);
    }

    @Test
    void holdForBookingIsIdempotent() {
        com.binbash.mobigo.domain.Booking b = bookingFixture(102L, 6000f, 1000f, 7L, 9L);
        LedgerTransaction existing = new LedgerTransaction();
        existing.setIdempotencyKey("SETTLE-102");
        when(txRepo.findByIdempotencyKey("SETTLE-102")).thenReturn(Optional.of(existing));

        wallet.holdForBooking(b);

        verify(txRepo, never()).save(any());
    }

    @Test
    void confirmAppliesEntriesToBalancesAndPosts() {
        LedgerAccount pass = new LedgerAccount();
        pass.setAccountKey("PASSENGER:7");
        pass.setBalance(new BigDecimal("10000"));
        LedgerAccount esc = new LedgerAccount();
        esc.setAccountKey("ESCROW");
        esc.setBalance(BigDecimal.ZERO);
        com.binbash.mobigo.domain.LedgerTransaction tx = new com.binbash.mobigo.domain.LedgerTransaction();
        tx.setStatus(LedgerTransactionStatus.DRAFT);
        tx.setIdempotencyKey("SETTLE-100");
        tx.addEntry(com.binbash.mobigo.domain.LedgerEntry.of(pass, LedgerDirection.DEBIT, new BigDecimal("6000")));
        tx.addEntry(com.binbash.mobigo.domain.LedgerEntry.of(esc, LedgerDirection.CREDIT, new BigDecimal("6000")));
        when(txRepo.lockByIdempotencyKey("SETTLE-100")).thenReturn(Optional.of(tx));
        when(accountRepo.lockByAccountKey("PASSENGER:7")).thenReturn(Optional.of(pass));
        when(accountRepo.lockByAccountKey("ESCROW")).thenReturn(Optional.of(esc));
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(txRepo.save(any(com.binbash.mobigo.domain.LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));

        wallet.confirmBookingSettlement(100L);

        assertThat(tx.getStatus()).isEqualTo(LedgerTransactionStatus.POSTED);
        assertThat(pass.getBalance()).isEqualByComparingTo(new BigDecimal("4000"));
        assertThat(esc.getBalance()).isEqualByComparingTo(new BigDecimal("6000"));
    }

    @Test
    void confirmIsIdempotentWhenAlreadyPosted() {
        com.binbash.mobigo.domain.LedgerTransaction tx = new com.binbash.mobigo.domain.LedgerTransaction();
        tx.setStatus(LedgerTransactionStatus.POSTED);
        tx.setIdempotencyKey("SETTLE-100");
        when(txRepo.lockByIdempotencyKey("SETTLE-100")).thenReturn(Optional.of(tx));

        wallet.confirmBookingSettlement(100L);

        verify(accountRepo, never()).save(any());
        verify(txRepo, never()).save(any());
    }

    @Test
    void voidDraftLeavesBalancesUntouched() {
        LedgerAccount pass = new LedgerAccount();
        pass.setAccountKey("PASSENGER:7");
        pass.setBalance(new BigDecimal("10000"));
        com.binbash.mobigo.domain.LedgerTransaction tx = new com.binbash.mobigo.domain.LedgerTransaction();
        tx.setStatus(LedgerTransactionStatus.DRAFT);
        tx.addEntry(com.binbash.mobigo.domain.LedgerEntry.of(pass, LedgerDirection.DEBIT, new BigDecimal("6000")));
        when(txRepo.lockByIdempotencyKey("SETTLE-200")).thenReturn(Optional.of(tx));
        when(txRepo.save(any(com.binbash.mobigo.domain.LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));

        wallet.voidBookingSettlement(200L);

        assertThat(tx.getStatus()).isEqualTo(LedgerTransactionStatus.VOID);
        assertThat(pass.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void voidPostedThrows() {
        com.binbash.mobigo.domain.LedgerTransaction tx = new com.binbash.mobigo.domain.LedgerTransaction();
        tx.setStatus(LedgerTransactionStatus.POSTED);
        tx.setIdempotencyKey("SETTLE-300");
        when(txRepo.lockByIdempotencyKey("SETTLE-300")).thenReturn(Optional.of(tx));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> wallet.voidBookingSettlement(300L)).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    void rechargeComputesGrossFeeOnTopAndCallsCampay() throws Exception {
        when(appSettingService.getCampayFeeRate()).thenReturn(0.02);
        when(accountRepo.findByAccountKey("EXTERNAL")).thenReturn(Optional.empty());
        when(accountRepo.findByAccountKey("PASSENGER:7")).thenReturn(Optional.empty());
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(txRepo.save(any(com.binbash.mobigo.domain.LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(campayService.collect(eq("237600000000"), eq(10200), any(), any())).thenReturn(
            new CampayService.CollectResponse("CR-1", "PENDING", "#150#")
        );

        com.binbash.mobigo.domain.LedgerTransaction tx = wallet.rechargeWallet(7L, new BigDecimal("10000"), "237600000000");

        assertThat(tx.getType()).isEqualTo(com.binbash.mobigo.domain.enumeration.LedgerTransactionType.RECHARGE);
        assertThat(tx.getStatus()).isEqualTo(LedgerTransactionStatus.DRAFT);
        assertThat(tx.getCampayReference()).isEqualTo("CR-1");
        verify(campayService).collect(eq("237600000000"), eq(10200), any(), any());
    }

    @Test
    void rechargeCallbackSuccessPostsAndCreditsPassenger() {
        LedgerAccount ext = new LedgerAccount();
        ext.setAccountKey("EXTERNAL");
        ext.setBalance(BigDecimal.ZERO);
        LedgerAccount pass = new LedgerAccount();
        pass.setAccountKey("PASSENGER:7");
        pass.setBalance(BigDecimal.ZERO);
        com.binbash.mobigo.domain.LedgerTransaction tx = new com.binbash.mobigo.domain.LedgerTransaction();
        tx.setType(com.binbash.mobigo.domain.enumeration.LedgerTransactionType.RECHARGE);
        tx.setStatus(LedgerTransactionStatus.DRAFT);
        tx.setExternalReference("RCG-x");
        tx.addEntry(com.binbash.mobigo.domain.LedgerEntry.of(ext, LedgerDirection.DEBIT, new BigDecimal("10000")));
        tx.addEntry(com.binbash.mobigo.domain.LedgerEntry.of(pass, LedgerDirection.CREDIT, new BigDecimal("10000")));
        when(txRepo.findByExternalReference("RCG-x")).thenReturn(Optional.of(tx));
        when(accountRepo.lockByAccountKey("EXTERNAL")).thenReturn(Optional.of(ext));
        when(accountRepo.lockByAccountKey("PASSENGER:7")).thenReturn(Optional.of(pass));
        when(accountRepo.save(any(LedgerAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(txRepo.save(any(com.binbash.mobigo.domain.LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));

        wallet.handleCampayCallback("RCG-x", "SUCCESSFUL");

        assertThat(tx.getStatus()).isEqualTo(LedgerTransactionStatus.POSTED);
        assertThat(pass.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void rechargeCallbackFailedVoids() {
        com.binbash.mobigo.domain.LedgerTransaction tx = new com.binbash.mobigo.domain.LedgerTransaction();
        tx.setType(com.binbash.mobigo.domain.enumeration.LedgerTransactionType.RECHARGE);
        tx.setStatus(LedgerTransactionStatus.DRAFT);
        tx.setExternalReference("RCG-y");
        when(txRepo.findByExternalReference("RCG-y")).thenReturn(Optional.of(tx));
        when(txRepo.save(any(com.binbash.mobigo.domain.LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0));

        wallet.handleCampayCallback("RCG-y", "FAILED");

        assertThat(tx.getStatus()).isEqualTo(LedgerTransactionStatus.VOID);
    }
}
