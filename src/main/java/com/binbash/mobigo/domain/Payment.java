package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.binbash.mobigo.domain.enumeration.PaymentStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * A Payment.
 */
@Entity
@Table(name = "payment")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "payment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Payment extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "montant", nullable = false)
    private Float montant;

    @NotNull
    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "methode", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private PaymentMethodEnum methode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private PaymentStatusEnum statut;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "campay_transaction_id")
    private String campayTransactionId;

    @Column(name = "operateur")
    private String operateur;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "disbursement_reference")
    private String disbursementReference;

    @Column(name = "disbursement_status")
    private String disbursementStatus;

    @JsonIgnoreProperties(value = { "payement", "trajet", "passager" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private Booking booking;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Payment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getMontant() {
        return this.montant;
    }

    public Payment montant(Float montant) {
        this.setMontant(montant);
        return this;
    }

    public void setMontant(Float montant) {
        this.montant = montant;
    }

    public LocalDate getDatePaiement() {
        return this.datePaiement;
    }

    public Payment datePaiement(LocalDate datePaiement) {
        this.setDatePaiement(datePaiement);
        return this;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }

    public PaymentMethodEnum getMethode() {
        return this.methode;
    }

    public Payment methode(PaymentMethodEnum methode) {
        this.setMethode(methode);
        return this;
    }

    public void setMethode(PaymentMethodEnum methode) {
        this.methode = methode;
    }

    public PaymentStatusEnum getStatut() {
        return this.statut;
    }

    public Payment statut(PaymentStatusEnum statut) {
        this.setStatut(statut);
        return this;
    }

    public void setStatut(PaymentStatusEnum statut) {
        this.statut = statut;
    }

    public Booking getBooking() {
        return this.booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Payment booking(Booking booking) {
        this.setBooking(booking);
        return this;
    }

    public String getExternalReference() {
        return this.externalReference;
    }

    public Payment externalReference(String externalReference) {
        this.setExternalReference(externalReference);
        return this;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getCampayTransactionId() {
        return this.campayTransactionId;
    }

    public Payment campayTransactionId(String campayTransactionId) {
        this.setCampayTransactionId(campayTransactionId);
        return this;
    }

    public void setCampayTransactionId(String campayTransactionId) {
        this.campayTransactionId = campayTransactionId;
    }

    public String getOperateur() {
        return this.operateur;
    }

    public Payment operateur(String operateur) {
        this.setOperateur(operateur);
        return this;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public Payment phoneNumber(String phoneNumber) {
        this.setPhoneNumber(phoneNumber);
        return this;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDisbursementReference() {
        return this.disbursementReference;
    }

    public Payment disbursementReference(String disbursementReference) {
        this.setDisbursementReference(disbursementReference);
        return this;
    }

    public void setDisbursementReference(String disbursementReference) {
        this.disbursementReference = disbursementReference;
    }

    public String getDisbursementStatus() {
        return this.disbursementStatus;
    }

    public Payment disbursementStatus(String disbursementStatus) {
        this.setDisbursementStatus(disbursementStatus);
        return this;
    }

    public void setDisbursementStatus(String disbursementStatus) {
        this.disbursementStatus = disbursementStatus;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment)) {
            return false;
        }
        return getId() != null && getId().equals(((Payment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Payment{" +
            "id=" + getId() +
            ", montant=" + getMontant() +
            ", datePaiement='" + getDatePaiement() + "'" +
            ", methode='" + getMethode() + "'" +
            ", statut='" + getStatut() + "'" +
            "}";
    }
}
