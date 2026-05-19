package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransaction transaction;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private LedgerDirection direction;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(LedgerTransaction t) {
        this.transaction = t;
    }

    public LedgerAccount getAccount() {
        return account;
    }

    public void setAccount(LedgerAccount a) {
        this.account = a;
    }

    public LedgerDirection getDirection() {
        return direction;
    }

    public void setDirection(LedgerDirection d) {
        this.direction = d;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public static LedgerEntry of(LedgerAccount account, LedgerDirection dir, BigDecimal amount) {
        LedgerEntry e = new LedgerEntry();
        e.setAccount(account);
        e.setDirection(dir);
        e.setAmount(amount);
        return e;
    }
}
