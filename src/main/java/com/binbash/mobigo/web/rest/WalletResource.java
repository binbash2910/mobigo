package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.service.InsufficientWalletBalanceException;
import com.binbash.mobigo.service.WalletService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletResource {

    private static final Logger LOG = LoggerFactory.getLogger(WalletResource.class);

    private final WalletService walletService;
    private final PeopleRepository peopleRepository;

    public WalletResource(WalletService walletService, PeopleRepository peopleRepository) {
        this.walletService = walletService;
        this.peopleRepository = peopleRepository;
    }

    private People currentPeople() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Not authenticated"));
        return peopleRepository.findByUserLogin(login).orElseThrow(() -> new IllegalStateException("No People for user " + login));
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> balance() {
        People p = currentPeople();
        String passKey = LedgerAccountType.PASSENGER.name() + ":" + p.getId();
        String driverKey = LedgerAccountType.DRIVER.name() + ":" + p.getId();
        Map<String, Object> body = new HashMap<>();
        body.put("available", walletService.availableBalance(passKey));
        body.put("driverAvailable", walletService.availableBalance(driverKey));
        return ResponseEntity.ok(body);
    }

    public record RechargeRequest(BigDecimal amount, String phone) {}

    /**
     * Credits the authenticated passenger's wallet via Campay COLLECT
     * (fee-on-top: passenger is charged amount + ~2% Campay fee, wallet credited the net amount).
     * Returns 200 with the externalReference once the USSD push is initiated;
     * the wallet is actually credited when the Campay webhook confirms SUCCESS.
     */
    @PostMapping("/recharge")
    public ResponseEntity<Map<String, Object>> recharge(@RequestBody RechargeRequest req) {
        if (req == null || req.amount() == null || req.phone() == null) {
            return ResponseEntity.badRequest().build();
        }
        People p = currentPeople();
        LOG.info("Wallet recharge requested: peopleId={} amount={}", p.getId(), req.amount());
        try {
            var tx = walletService.rechargeWallet(p.getId(), req.amount(), req.phone());
            LOG.info("Wallet recharge initiated: peopleId={} externalReference={}", p.getId(), tx.getExternalReference());
            Map<String, Object> body = new HashMap<>();
            body.put("externalReference", tx.getExternalReference());
            body.put("status", tx.getStatus().name());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            LOG.warn("Wallet recharge rejected - invalid amount: peopleId={} reason={}", p.getId(), ex.getMessage());
            return ResponseEntity.badRequest().header("X-mobigo-error", "invalid-amount").build();
        }
    }

    public record PayoutRequest(BigDecimal amount, String phone) {}

    /**
     * Withdraws the authenticated user's accumulated DRIVER balance (driver earnings)
     * to their Mobile Money number. The platform absorbs the Campay disbursement fee.
     * Returns 409 (X-mobigo-shortfall) if the driver balance is insufficient,
     * 400 if amount is below the configured minimum withdrawal threshold,
     * 500 (RuntimeException from Campay) if the disbursement initiation fails.
     */
    @PostMapping("/payout")
    public ResponseEntity<Map<String, Object>> payout(@RequestBody PayoutRequest req) {
        if (req == null || req.amount() == null || req.phone() == null) {
            return ResponseEntity.badRequest().build();
        }
        People p = currentPeople();
        LOG.info("Wallet payout requested: peopleId={} amount={}", p.getId(), req.amount());
        try {
            var tx = walletService.requestPayout(p.getId(), req.amount(), req.phone());
            LOG.info("Wallet payout initiated: peopleId={} externalReference={}", p.getId(), tx.getExternalReference());
            Map<String, Object> body = new HashMap<>();
            body.put("externalReference", tx.getExternalReference());
            body.put("status", tx.getStatus().name());
            return ResponseEntity.ok(body);
        } catch (InsufficientWalletBalanceException ex) {
            LOG.warn("Wallet payout rejected - insufficient balance: peopleId={} shortfall={}", p.getId(), ex.getShortfall());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("X-mobigo-error", "wallet-insufficient")
                .header("X-mobigo-shortfall", ex.getShortfall().toPlainString())
                .build();
        } catch (IllegalArgumentException ex) {
            LOG.warn("Wallet payout rejected - invalid amount: peopleId={} reason={}", p.getId(), ex.getMessage());
            return ResponseEntity.badRequest().header("X-mobigo-error", "invalid-amount").build();
        }
    }
}
