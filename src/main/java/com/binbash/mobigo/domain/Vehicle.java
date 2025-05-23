package com.binbash.mobigo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Vehicle.
 */
@Entity
@Table(name = "vehicle")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "vehicle")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Vehicle extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "marque", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String marque;

    @NotNull
    @Column(name = "modele", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String modele;

    @NotNull
    @Column(name = "annee", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String annee;

    @NotNull
    @Column(name = "carte_grise", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String carteGrise;

    @NotNull
    @Column(name = "immatriculation", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String immatriculation;

    @NotNull
    @Column(name = "nb_places", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer nbPlaces;

    @NotNull
    @Column(name = "couleur", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String couleur;

    @Column(name = "photo")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String photo;

    @NotNull
    @Column(name = "actif", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String actif;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "vehicule")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "steps", "bookingsTrajets", "notations", "vehicule" }, allowSetters = true)
    private Set<Ride> trajets = new HashSet<>();

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
    private People proprietaire;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Vehicle id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarque() {
        return this.marque;
    }

    public Vehicle marque(String marque) {
        this.setMarque(marque);
        return this;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return this.modele;
    }

    public Vehicle modele(String modele) {
        this.setModele(modele);
        return this;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public String getAnnee() {
        return this.annee;
    }

    public Vehicle annee(String annee) {
        this.setAnnee(annee);
        return this;
    }

    public void setAnnee(String annee) {
        this.annee = annee;
    }

    public String getCarteGrise() {
        return this.carteGrise;
    }

    public Vehicle carteGrise(String carteGrise) {
        this.setCarteGrise(carteGrise);
        return this;
    }

    public void setCarteGrise(String carteGrise) {
        this.carteGrise = carteGrise;
    }

    public String getImmatriculation() {
        return this.immatriculation;
    }

    public Vehicle immatriculation(String immatriculation) {
        this.setImmatriculation(immatriculation);
        return this;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    public Integer getNbPlaces() {
        return this.nbPlaces;
    }

    public Vehicle nbPlaces(Integer nbPlaces) {
        this.setNbPlaces(nbPlaces);
        return this;
    }

    public void setNbPlaces(Integer nbPlaces) {
        this.nbPlaces = nbPlaces;
    }

    public String getCouleur() {
        return this.couleur;
    }

    public Vehicle couleur(String couleur) {
        this.setCouleur(couleur);
        return this;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getPhoto() {
        return this.photo;
    }

    public Vehicle photo(String photo) {
        this.setPhoto(photo);
        return this;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getActif() {
        return this.actif;
    }

    public Vehicle actif(String actif) {
        this.setActif(actif);
        return this;
    }

    public void setActif(String actif) {
        this.actif = actif;
    }

    public Set<Ride> getTrajets() {
        return this.trajets;
    }

    public void setTrajets(Set<Ride> rides) {
        if (this.trajets != null) {
            this.trajets.forEach(i -> i.setVehicule(null));
        }
        if (rides != null) {
            rides.forEach(i -> i.setVehicule(this));
        }
        this.trajets = rides;
    }

    public Vehicle trajets(Set<Ride> rides) {
        this.setTrajets(rides);
        return this;
    }

    public Vehicle addTrajets(Ride ride) {
        this.trajets.add(ride);
        ride.setVehicule(this);
        return this;
    }

    public Vehicle removeTrajets(Ride ride) {
        this.trajets.remove(ride);
        ride.setVehicule(null);
        return this;
    }

    public People getProprietaire() {
        return this.proprietaire;
    }

    public void setProprietaire(People people) {
        this.proprietaire = people;
    }

    public Vehicle proprietaire(People people) {
        this.setProprietaire(people);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        return getId() != null && getId().equals(((Vehicle) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Vehicle{" +
            "id=" + getId() +
            ", marque='" + getMarque() + "'" +
            ", modele='" + getModele() + "'" +
            ", annee='" + getAnnee() + "'" +
            ", carteGrise='" + getCarteGrise() + "'" +
            ", immatriculation='" + getImmatriculation() + "'" +
            ", nbPlaces=" + getNbPlaces() +
            ", couleur='" + getCouleur() + "'" +
            ", photo='" + getPhoto() + "'" +
            ", actif='" + getActif() + "'" +
            "}";
    }
}
