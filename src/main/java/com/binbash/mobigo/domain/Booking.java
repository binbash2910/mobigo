package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * A Booking.
 */
@Entity
@Table(name = "booking")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "booking")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Booking extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "nb_places_reservees", nullable = false)
    private Long nbPlacesReservees;

    @NotNull
    @Column(name = "montant_total", nullable = false)
    private Float montantTotal;

    @Column(name = "commission")
    private Float commission;

    @NotNull
    @Column(name = "date_reservation", nullable = false)
    private LocalDate dateReservation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private BookingStatusEnum statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "methode_payment")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private PaymentMethodEnum methodePayment;

    @JsonIgnoreProperties(value = { "booking" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "booking")
    @org.springframework.data.annotation.Transient
    private Payment payement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "steps", "bookingsTrajets", "notations", "vehicule" }, allowSetters = true)
    private Ride trajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = {
            "vehicules",
            "savedPaymentMethods",
            "bookingsPassagers",
            "notationsPassagers",
            "notationsConducteurs",
            "user",
            "messagesExpediteurs",
            "messagesDestinataires",
        },
        allowSetters = true
    )
    private People passager;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Booking id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNbPlacesReservees() {
        return this.nbPlacesReservees;
    }

    public Booking nbPlacesReservees(Long nbPlacesReservees) {
        this.setNbPlacesReservees(nbPlacesReservees);
        return this;
    }

    public void setNbPlacesReservees(Long nbPlacesReservees) {
        this.nbPlacesReservees = nbPlacesReservees;
    }

    public Float getMontantTotal() {
        return this.montantTotal;
    }

    public Booking montantTotal(Float montantTotal) {
        this.setMontantTotal(montantTotal);
        return this;
    }

    public void setMontantTotal(Float montantTotal) {
        this.montantTotal = montantTotal;
    }

    public Float getCommission() {
        return this.commission;
    }

    public Booking commission(Float commission) {
        this.setCommission(commission);
        return this;
    }

    public void setCommission(Float commission) {
        this.commission = commission;
    }

    public LocalDate getDateReservation() {
        return this.dateReservation;
    }

    public Booking dateReservation(LocalDate dateReservation) {
        this.setDateReservation(dateReservation);
        return this;
    }

    public void setDateReservation(LocalDate dateReservation) {
        this.dateReservation = dateReservation;
    }

    public BookingStatusEnum getStatut() {
        return this.statut;
    }

    public Booking statut(BookingStatusEnum statut) {
        this.setStatut(statut);
        return this;
    }

    public void setStatut(BookingStatusEnum statut) {
        this.statut = statut;
    }

    public PaymentMethodEnum getMethodePayment() {
        return this.methodePayment;
    }

    public Booking methodePayment(PaymentMethodEnum methodePayment) {
        this.setMethodePayment(methodePayment);
        return this;
    }

    public void setMethodePayment(PaymentMethodEnum methodePayment) {
        this.methodePayment = methodePayment;
    }

    public Payment getPayement() {
        return this.payement;
    }

    public void setPayement(Payment payment) {
        if (this.payement != null) {
            this.payement.setBooking(null);
        }
        if (payment != null) {
            payment.setBooking(this);
        }
        this.payement = payment;
    }

    public Booking payement(Payment payment) {
        this.setPayement(payment);
        return this;
    }

    public Ride getTrajet() {
        return this.trajet;
    }

    public void setTrajet(Ride ride) {
        this.trajet = ride;
    }

    public Booking trajet(Ride ride) {
        this.setTrajet(ride);
        return this;
    }

    public People getPassager() {
        return this.passager;
    }

    public void setPassager(People people) {
        this.passager = people;
    }

    public Booking passager(People people) {
        this.setPassager(people);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Booking)) {
            return false;
        }
        return getId() != null && getId().equals(((Booking) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Booking{" +
            "id=" + getId() +
            ", nbPlacesReservees=" + getNbPlacesReservees() +
            ", montantTotal=" + getMontantTotal() +
            ", commission=" + getCommission() +
            ", dateReservation='" + getDateReservation() + "'" +
            ", statut='" + getStatut() + "'" +
            ", methodePayment='" + getMethodePayment() + "'" +
            "}";
    }
}
