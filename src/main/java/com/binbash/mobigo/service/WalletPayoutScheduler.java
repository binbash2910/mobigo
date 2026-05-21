package com.binbash.mobigo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WalletPayoutScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(WalletPayoutScheduler.class);

    private final WalletService walletService;

    public WalletPayoutScheduler(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Periodic grouped driver payouts. Sweep DRIVER accounts and disburse
     * accumulated balance ≥ min withdrawal. Default interval: 24h (configurable).
     */
    @Scheduled(fixedDelayString = "${application.wallet.scheduled-payout-delay-ms:86400000}")
    public void scheduledGroupedPayout() {
        LOG.info("Running scheduled grouped driver payouts");
        walletService.runScheduledPayouts();
    }
}
