package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * not an ignored comment
 */
@Schema(description = "not an ignored comment")
@Entity
@Table(name = "ride")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "ride")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Ride extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "ville_depart", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String villeDepart;

    @NotNull
    @Column(name = "ville_arrivee", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String villeArrivee;

    @NotNull
    @Column(name = "date_depart", nullable = false)
    private LocalDate dateDepart;

    @NotNull
    @Column(name = "date_arrivee", nullable = false)
    private LocalDate dateArrivee;

    @NotNull
    @Column(name = "heure_depart", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String heureDepart;

    @NotNull
    @Column(name = "heure_arrivee", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String heureArrivee;

    @NotNull
    @Column(name = "minute_depart", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String minuteDepart;

    @NotNull
    @Column(name = "minute_arrivee", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String minuteArrivee;

    @NotNull
    @Column(name = "prix_par_place", nullable = false)
    private Float prixParPlace;

    @NotNull
    @Column(name = "nbre_place_disponible", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer nbrePlaceDisponible;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private RideStatusEnum statut;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "trajet")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "trajet" }, allowSetters = true)
    private Set<Step> steps = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "trajet")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "payement", "trajet", "passager" }, allowSetters = true)
    private Set<Booking> bookingsTrajets = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "trajet")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "trajet", "passager", "conducteur" }, allowSetters = true)
    private Set<Rating> notations = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "trajets", "proprietaire" }, allowSetters = true)
    private Vehicle vehicule;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Ride id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVilleDepart() {
        return this.villeDepart;
    }

    public Ride villeDepart(String villeDepart) {
        this.setVilleDepart(villeDepart);
        return this;
    }

    public void setVilleDepart(String villeDepart) {
        this.villeDepart = villeDepart;
    }

    public String getVilleArrivee() {
        return this.villeArrivee;
    }

    public Ride villeArrivee(String villeArrivee) {
        this.setVilleArrivee(villeArrivee);
        return this;
    }

    public void setVilleArrivee(String villeArrivee) {
        this.villeArrivee = villeArrivee;
    }

    public LocalDate getDateDepart() {
        return this.dateDepart;
    }

    public Ride dateDepart(LocalDate dateDepart) {
        this.setDateDepart(dateDepart);
        return this;
    }

    public void setDateDepart(LocalDate dateDepart) {
        this.dateDepart = dateDepart;
    }

    public LocalDate getDateArrivee() {
        return this.dateArrivee;
    }

    public Ride dateArrivee(LocalDate dateArrivee) {
        this.setDateArrivee(dateArrivee);
        return this;
    }

    public void setDateArrivee(LocalDate dateArrivee) {
        this.dateArrivee = dateArrivee;
    }

    public String getHeureDepart() {
        return this.heureDepart;
    }

    public Ride heureDepart(String heureDepart) {
        this.setHeureDepart(heureDepart);
        return this;
    }

    public void setHeureDepart(String heureDepart) {
        this.heureDepart = heureDepart;
    }

    public String getHeureArrivee() {
        return this.heureArrivee;
    }

    public Ride heureArrivee(String heureArrivee) {
        this.setHeureArrivee(heureArrivee);
        return this;
    }

    public void setHeureArrivee(String heureArrivee) {
        this.heureArrivee = heureArrivee;
    }

    public String getMinuteDepart() {
        return this.minuteDepart;
    }

    public Ride minuteDepart(String minuteDepart) {
        this.setMinuteDepart(minuteDepart);
        return this;
    }

    public void setMinuteDepart(String minuteDepart) {
        this.minuteDepart = minuteDepart;
    }

    public String getMinuteArrivee() {
        return this.minuteArrivee;
    }

    public Ride minuteArrivee(String minuteArrivee) {
        this.setMinuteArrivee(minuteArrivee);
        return this;
    }

    public void setMinuteArrivee(String minuteArrivee) {
        this.minuteArrivee = minuteArrivee;
    }

    public Float getPrixParPlace() {
        return this.prixParPlace;
    }

    public Ride prixParPlace(Float prixParPlace) {
        this.setPrixParPlace(prixParPlace);
        return this;
    }

    public void setPrixParPlace(Float prixParPlace) {
        this.prixParPlace = prixParPlace;
    }

    public Integer getNbrePlaceDisponible() {
        return this.nbrePlaceDisponible;
    }

    public Ride nbrePlaceDisponible(Integer nbrePlaceDisponible) {
        this.setNbrePlaceDisponible(nbrePlaceDisponible);
        return this;
    }

    public void setNbrePlaceDisponible(Integer nbrePlaceDisponible) {
        this.nbrePlaceDisponible = nbrePlaceDisponible;
    }

    public RideStatusEnum getStatut() {
        return this.statut;
    }

    public Ride statut(RideStatusEnum statut) {
        this.setStatut(statut);
        return this;
    }

    public void setStatut(RideStatusEnum statut) {
        this.statut = statut;
    }

    public Set<Step> getSteps() {
        return this.steps;
    }

    public void setSteps(Set<Step> steps) {
        if (this.steps != null) {
            this.steps.forEach(i -> i.setTrajet(null));
        }
        if (steps != null) {
            steps.forEach(i -> i.setTrajet(this));
        }
        this.steps = steps;
    }

    public Ride steps(Set<Step> steps) {
        this.setSteps(steps);
        return this;
    }

    public Ride addSteps(Step step) {
        this.steps.add(step);
        step.setTrajet(this);
        return this;
    }

    public Ride removeSteps(Step step) {
        this.steps.remove(step);
        step.setTrajet(null);
        return this;
    }

    public Set<Booking> getBookingsTrajets() {
        return this.bookingsTrajets;
    }

    public void setBookingsTrajets(Set<Booking> bookings) {
        if (this.bookingsTrajets != null) {
            this.bookingsTrajets.forEach(i -> i.setTrajet(null));
        }
        if (bookings != null) {
            bookings.forEach(i -> i.setTrajet(this));
        }
        this.bookingsTrajets = bookings;
    }

    public Ride bookingsTrajets(Set<Booking> bookings) {
        this.setBookingsTrajets(bookings);
        return this;
    }

    public Ride addBookingsTrajet(Booking booking) {
        this.bookingsTrajets.add(booking);
        booking.setTrajet(this);
        return this;
    }

    public Ride removeBookingsTrajet(Booking booking) {
        this.bookingsTrajets.remove(booking);
        booking.setTrajet(null);
        return this;
    }

    public Set<Rating> getNotations() {
        return this.notations;
    }

    public void setNotations(Set<Rating> ratings) {
        if (this.notations != null) {
            this.notations.forEach(i -> i.setTrajet(null));
        }
        if (ratings != null) {
            ratings.forEach(i -> i.setTrajet(this));
        }
        this.notations = ratings;
    }

    public Ride notations(Set<Rating> ratings) {
        this.setNotations(ratings);
        return this;
    }

    public Ride addNotations(Rating rating) {
        this.notations.add(rating);
        rating.setTrajet(this);
        return this;
    }

    public Ride removeNotations(Rating rating) {
        this.notations.remove(rating);
        rating.setTrajet(null);
        return this;
    }

    public Vehicle getVehicule() {
        return this.vehicule;
    }

    public void setVehicule(Vehicle vehicle) {
        this.vehicule = vehicle;
    }

    public Ride vehicule(Vehicle vehicle) {
        this.setVehicule(vehicle);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ride)) {
            return false;
        }
        return getId() != null && getId().equals(((Ride) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Ride{" +
            "id=" + getId() +
            ", villeDepart='" + getVilleDepart() + "'" +
            ", villeArrivee='" + getVilleArrivee() + "'" +
            ", dateDepart='" + getDateDepart() + "'" +
            ", dateArrivee='" + getDateArrivee() + "'" +
            ", heureDepart='" + getHeureDepart() + "'" +
            ", heureArrivee='" + getHeureArrivee() + "'" +
            ", minuteDepart='" + getMinuteDepart() + "'" +
            ", minuteArrivee='" + getMinuteArrivee() + "'" +
            ", prixParPlace=" + getPrixParPlace() +
            ", nbrePlaceDisponible=" + getNbrePlaceDisponible() +
            ", statut='" + getStatut() + "'" +
            "}";
    }
}
