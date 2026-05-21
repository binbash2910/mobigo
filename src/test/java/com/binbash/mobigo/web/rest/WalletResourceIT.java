package com.binbash.mobigo.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.repository.LedgerAccountRepository;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.UserRepository;
import com.binbash.mobigo.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@IntegrationTest
@Transactional
class WalletResourceIT {

    private static final String LOGIN = "wallet-user";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PeopleRepository peopleRepository;

    @Autowired
    LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    WalletService walletService;

    @Test
    @WithMockUser(username = LOGIN)
    void getBalanceReturnsAvailableForCurrentUser() throws Exception {
        User u = new User();
        u.setLogin(LOGIN);
        // @Size(min=60,max=60) on password_hash column — use a 60-char placeholder
        u.setPassword("x".repeat(60));
        u.setActivated(true);
        u.setEmail(LOGIN + "@example.test");
        u.setLangKey("fr");
        u = userRepository.saveAndFlush(u);

        People p = new People();
        p.setNom("W");
        p.setPrenom("U");
        p.setTelephone("237600000000");
        p.setCni("CNIW-" + LOGIN);
        p.setActif("true");
        p.setDateNaissance(LocalDate.of(1990, 1, 1));
        p.setUser(u);
        p = peopleRepository.saveAndFlush(p);

        LedgerAccount acc = walletService.getOrCreateAccount(LedgerAccountType.PASSENGER, p.getId());
        acc.setBalance(new BigDecimal("12000"));
        ledgerAccountRepository.saveAndFlush(acc);

        mockMvc
            .perform(get("/api/wallet/balance").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(12000))
            .andExpect(jsonPath("$.driverAvailable").exists());
    }
}
