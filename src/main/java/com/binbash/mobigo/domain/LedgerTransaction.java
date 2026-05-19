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

    public void setId(Long id) {
        this.id = id;
    }

    public LedgerTransactionType getType() {
        return type;
    }

    public void setType(LedgerTransactionType type) {
        this.type = type;
    }

    public LedgerTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerTransactionStatus status) {
        this.status = status;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String k) {
        this.idempotencyKey = k;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String r) {
        this.externalReference = r;
    }

    public String getCampayReference() {
        return campayReference;
    }

    public void setCampayReference(String r) {
        this.campayReference = r;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(LedgerEntry e) {
        e.setTransaction(this);
        this.entries.add(e);
    }
}
