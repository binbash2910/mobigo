package com.binbash.mobigo.service;

import java.math.BigDecimal;

public class InsufficientWalletBalanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal shortfall;

    public InsufficientWalletBalanceException(BigDecimal shortfall) {
        super("Insufficient wallet balance, shortfall=" + shortfall);
        this.shortfall = shortfall;
    }

    public BigDecimal getShortfall() {
        return shortfall;
    }
}
