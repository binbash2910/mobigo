package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.LedgerDirection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Immutable debit/credit line of a {@link LedgerTransaction}. Deliberately does
 * NOT extend AbstractAuditingEntity: entries are created atomically with their
 * owning transaction (cascade), whose audit timestamps cover them.
 */
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

    public LedgerEntry id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public LedgerEntry transaction(LedgerTransaction transaction) {
        this.setTransaction(transaction);
        return this;
    }

    public void setTransaction(LedgerTransaction transaction) {
        this.transaction = transaction;
    }

    public LedgerAccount getAccount() {
        return account;
    }

    public LedgerEntry account(LedgerAccount account) {
        this.setAccount(account);
        return this;
    }

    public void setAccount(LedgerAccount account) {
        this.account = account;
    }

    public LedgerDirection getDirection() {
        return direction;
    }

    public LedgerEntry direction(LedgerDirection direction) {
        this.setDirection(direction);
        return this;
    }

    public void setDirection(LedgerDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LedgerEntry amount(BigDecimal amount) {
        this.setAmount(amount);
        return this;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((LedgerEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LedgerEntry{" +
            "id=" + getId() +
            ", direction='" + getDirection() + "'" +
            ", amount=" + getAmount() +
            "}";
    }
}
