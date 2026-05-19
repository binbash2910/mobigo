package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.LedgerTransactionStatus;
import com.binbash.mobigo.domain.enumeration.LedgerTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "ledger_transaction",
    uniqueConstraints = {
        @UniqueConstraint(name = "ux_ledger_tx_idem", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "ux_ledger_tx_extref", columnNames = "external_reference"),
    }
)
public class LedgerTransaction extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LedgerTransactionType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LedgerTransactionStatus status;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @Column(name = "external_reference", length = 80)
    private String externalReference;

    @Column(name = "campay_reference", length = 120)
    private String campayReference;

    @Column(name = "description", length = 240)
    private String description;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LedgerEntry> entries = new ArrayList<>();

    @Override
    public Long getId() {
        return id;
    }

    public LedgerTransaction id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LedgerTransactionType getType() {
        return type;
    }

    public LedgerTransaction type(LedgerTransactionType type) {
        this.setType(type);
        return this;
    }

    public void setType(LedgerTransactionType type) {
        this.type = type;
    }

    public LedgerTransactionStatus getStatus() {
        return status;
    }

    public LedgerTransaction status(LedgerTransactionStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(LedgerTransactionStatus status) {
        this.status = status;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public LedgerTransaction bookingId(Long bookingId) {
        this.setBookingId(bookingId);
        return this;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LedgerTransaction idempotencyKey(String idempotencyKey) {
        this.setIdempotencyKey(idempotencyKey);
        return this;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public LedgerTransaction externalReference(String externalReference) {
        this.setExternalReference(externalReference);
        return this;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getCampayReference() {
        return campayReference;
    }

    public LedgerTransaction campayReference(String campayReference) {
        this.setCampayReference(campayReference);
        return this;
    }

    public void setCampayReference(String campayReference) {
        this.campayReference = campayReference;
    }

    public String getDescription() {
        return description;
    }

    public LedgerTransaction description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public LedgerTransaction entries(List<LedgerEntry> entries) {
        this.setEntries(entries);
        return this;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(LedgerEntry entry) {
        entry.setTransaction(this);
        this.entries.add(entry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerTransaction)) {
            return false;
        }
        return getId() != null && getId().equals(((LedgerTransaction) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LedgerTransaction{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", status='" + getStatus() + "'" +
            ", bookingId=" + getBookingId() +
            ", externalReference='" + getExternalReference() + "'" +
            "}";
    }
}
