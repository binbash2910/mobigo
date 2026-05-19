package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionStatus;
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
    LedgerAccountRepository accountRepo;

    @Mock
    LedgerTransactionRepository txRepo;

    @Mock
    LedgerEntryRepository entryRepo;

    @Mock
    BookingRepository bookingRepo;

    @Mock
    PeopleRepository peopleRepo;

    @Mock
    CampayService campayService;

    @Mock
    AppSettingService appSettingService;

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
}
