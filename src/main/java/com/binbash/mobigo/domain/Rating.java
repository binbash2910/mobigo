package com.binbash.mobigo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * A Rating.
 */
@Entity
@Table(name = "rating")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "rating")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Rating extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "note")
    private Float note;

    @Column(name = "commentaire")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String commentaire;

    @NotNull
    @Column(name = "rating_date", nullable = false)
    private LocalDate ratingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "steps", "bookingsTrajets", "notations", "vehicule" }, allowSetters = true)
    private Ride trajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = {
            "vehicules",
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = {
            "vehicules",
            "bookingsPassagers",
            "notationsPassagers",
            "notationsConducteurs",
            "user",
            "messagesExpediteurs",
            "messagesDestinataires",
        },
        allowSetters = true
    )
    private People conducteur;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Rating id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getNote() {
        return this.note;
    }

    public Rating note(Float note) {
        this.setNote(note);
        return this;
    }

    public void setNote(Float note) {
        this.note = note;
    }

    public String getCommentaire() {
        return this.commentaire;
    }

    public Rating commentaire(String commentaire) {
        this.setCommentaire(commentaire);
        return this;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDate getRatingDate() {
        return this.ratingDate;
    }

    public Rating ratingDate(LocalDate ratingDate) {
        this.setRatingDate(ratingDate);
        return this;
    }

    public void setRatingDate(LocalDate ratingDate) {
        this.ratingDate = ratingDate;
    }

    public Ride getTrajet() {
        return this.trajet;
    }

    public void setTrajet(Ride ride) {
        this.trajet = ride;
    }

    public Rating trajet(Ride ride) {
        this.setTrajet(ride);
        return this;
    }

    public People getPassager() {
        return this.passager;
    }

    public void setPassager(People people) {
        this.passager = people;
    }

    public Rating passager(People people) {
        this.setPassager(people);
        return this;
    }

    public People getConducteur() {
        return this.conducteur;
    }

    public void setConducteur(People people) {
        this.conducteur = people;
    }

    public Rating conducteur(People people) {
        this.setConducteur(people);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rating)) {
            return false;
        }
        return getId() != null && getId().equals(((Rating) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Rating{" +
            "id=" + getId() +
            ", note=" + getNote() +
            ", commentaire='" + getCommentaire() + "'" +
            ", ratingDate='" + getRatingDate() + "'" +
            "}";
    }
}
