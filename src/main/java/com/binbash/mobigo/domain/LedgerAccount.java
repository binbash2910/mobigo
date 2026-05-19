package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_account", uniqueConstraints = @UniqueConstraint(columnNames = "account_key"))
public class LedgerAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "account_key", nullable = false, length = 60)
    private String accountKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private LedgerAccountType accountType;

    @Column(name = "owner_people_id")
    private Long ownerPeopleId;

    @NotNull
    @Column(name = "balance", nullable = false, precision = 19, scale = 0)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getId() {
        return id;
    }

    public LedgerAccount id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public LedgerAccount accountKey(String accountKey) {
        this.setAccountKey(accountKey);
        return this;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
    }

    public LedgerAccount accountType(LedgerAccountType accountType) {
        this.setAccountType(accountType);
        return this;
    }

    public void setAccountType(LedgerAccountType accountType) {
        this.accountType = accountType;
    }

    public Long getOwnerPeopleId() {
        return ownerPeopleId;
    }

    public LedgerAccount ownerPeopleId(Long ownerPeopleId) {
        this.setOwnerPeopleId(ownerPeopleId);
        return this;
    }

    public void setOwnerPeopleId(Long ownerPeopleId) {
        this.ownerPeopleId = ownerPeopleId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LedgerAccount balance(BigDecimal balance) {
        this.setBalance(balance);
        return this;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getVersion() {
        return version;
    }

    public LedgerAccount version(Long version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerAccount)) {
            return false;
        }
        return getId() != null && getId().equals(((LedgerAccount) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LedgerAccount{" +
            "id=" + getId() +
            ", accountKey='" + getAccountKey() + "'" +
            ", accountType='" + getAccountType() + "'" +
            ", balance=" + getBalance() +
            "}";
    }
}
