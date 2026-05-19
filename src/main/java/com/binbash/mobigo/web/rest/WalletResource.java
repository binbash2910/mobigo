package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.service.WalletService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletResource {

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

    @PostMapping("/recharge")
    public ResponseEntity<Map<String, Object>> recharge(@RequestBody RechargeRequest req) {
        if (req == null || req.amount() == null || req.phone() == null) {
            return ResponseEntity.badRequest().build();
        }
        People p = currentPeople();
        var tx = walletService.rechargeWallet(p.getId(), req.amount(), req.phone());
        Map<String, Object> body = new HashMap<>();
        body.put("externalReference", tx.getExternalReference());
        body.put("status", tx.getStatus().name());
        return ResponseEntity.ok(body);
    }

    public record PayoutRequest(BigDecimal amount, String phone) {}

    @PostMapping("/payout")
    public ResponseEntity<Map<String, Object>> payout(@RequestBody PayoutRequest req) {
        if (req == null || req.amount() == null || req.phone() == null) {
            return ResponseEntity.badRequest().build();
        }
        People p = currentPeople();
        var tx = walletService.requestPayout(p.getId(), req.amount(), req.phone());
        Map<String, Object> body = new HashMap<>();
        body.put("externalReference", tx.getExternalReference());
        body.put("status", tx.getStatus().name());
        return ResponseEntity.ok(body);
    }
}
