package com.binbash.mobigo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * A People.
 */
@Entity
@Table(name = "people")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "people")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class People extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "nom", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String nom;

    @Column(name = "prenom")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String prenom;

    @NotNull
    @Column(name = "telephone", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String telephone;

    @NotNull
    @Column(name = "cni", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String cni;

    @Column(name = "photo")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String photo;

    @NotNull
    @Column(name = "actif", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String actif;

    @NotNull
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "musique")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String musique;

    @Column(name = "discussion")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String discussion;

    @Column(name = "cigarette")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String cigarette;

    @Column(name = "alcool")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String alcool;

    @Column(name = "animaux")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String animaux;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "proprietaire")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "trajets", "proprietaire" }, allowSetters = true)
    private Set<Vehicle> vehicules = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "passager")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "payement", "trajet", "passager" }, allowSetters = true)
    private Set<Booking> bookingsPassagers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "passager")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "trajet", "passager", "conducteur" }, allowSetters = true)
    private Set<Rating> notationsPassagers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "conducteur")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "trajet", "passager", "conducteur" }, allowSetters = true)
    private Set<Rating> notationsConducteurs = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "expediteurs", "destinataires" }, allowSetters = true)
    private Message messagesExpediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "expediteurs", "destinataires" }, allowSetters = true)
    private Message messagesDestinatire;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public People id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return this.nom;
    }

    public People nom(String nom) {
        this.setNom(nom);
        return this;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return this.prenom;
    }

    public People prenom(String prenom) {
        this.setPrenom(prenom);
        return this;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return this.telephone;
    }

    public People telephone(String telephone) {
        this.setTelephone(telephone);
        return this;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getCni() {
        return this.cni;
    }

    public People cni(String cni) {
        this.setCni(cni);
        return this;
    }

    public void setCni(String cni) {
        this.cni = cni;
    }

    public String getPhoto() {
        return this.photo;
    }

    public People photo(String photo) {
        this.setPhoto(photo);
        return this;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getActif() {
        return this.actif;
    }

    public People actif(String actif) {
        this.setActif(actif);
        return this;
    }

    public void setActif(String actif) {
        this.actif = actif;
    }

    public LocalDate getDateNaissance() {
        return this.dateNaissance;
    }

    public People dateNaissance(LocalDate dateNaissance) {
        this.setDateNaissance(dateNaissance);
        return this;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getMusique() {
        return this.musique;
    }

    public People musique(String musique) {
        this.setMusique(musique);
        return this;
    }

    public void setMusique(String musique) {
        this.musique = musique;
    }

    public String getDiscussion() {
        return this.discussion;
    }

    public People discussion(String discussion) {
        this.setDiscussion(discussion);
        return this;
    }

    public void setDiscussion(String discussion) {
        this.discussion = discussion;
    }

    public String getCigarette() {
        return this.cigarette;
    }

    public People cigarette(String cigarette) {
        this.setCigarette(cigarette);
        return this;
    }

    public void setCigarette(String cigarette) {
        this.cigarette = cigarette;
    }

    public String getAlcool() {
        return this.alcool;
    }

    public People alcool(String alcool) {
        this.setAlcool(alcool);
        return this;
    }

    public void setAlcool(String alcool) {
        this.alcool = alcool;
    }

    public String getAnimaux() {
        return this.animaux;
    }

    public People animaux(String animaux) {
        this.setAnimaux(animaux);
        return this;
    }

    public void setAnimaux(String animaux) {
        this.animaux = animaux;
    }

    public Set<Vehicle> getVehicules() {
        return this.vehicules;
    }

    public void setVehicules(Set<Vehicle> vehicles) {
        if (this.vehicules != null) {
            this.vehicules.forEach(i -> i.setProprietaire(null));
        }
        if (vehicles != null) {
            vehicles.forEach(i -> i.setProprietaire(this));
        }
        this.vehicules = vehicles;
    }

    public People vehicules(Set<Vehicle> vehicles) {
        this.setVehicules(vehicles);
        return this;
    }

    public People addVehicules(Vehicle vehicle) {
        this.vehicules.add(vehicle);
        vehicle.setProprietaire(this);
        return this;
    }

    public People removeVehicules(Vehicle vehicle) {
        this.vehicules.remove(vehicle);
        vehicle.setProprietaire(null);
        return this;
    }

    public Set<Booking> getBookingsPassagers() {
        return this.bookingsPassagers;
    }

    public void setBookingsPassagers(Set<Booking> bookings) {
        if (this.bookingsPassagers != null) {
            this.bookingsPassagers.forEach(i -> i.setPassager(null));
        }
        if (bookings != null) {
            bookings.forEach(i -> i.setPassager(this));
        }
        this.bookingsPassagers = bookings;
    }

    public People bookingsPassagers(Set<Booking> bookings) {
        this.setBookingsPassagers(bookings);
        return this;
    }

    public People addBookingsPassager(Booking booking) {
        this.bookingsPassagers.add(booking);
        booking.setPassager(this);
        return this;
    }

    public People removeBookingsPassager(Booking booking) {
        this.bookingsPassagers.remove(booking);
        booking.setPassager(null);
        return this;
    }

    public Set<Rating> getNotationsPassagers() {
        return this.notationsPassagers;
    }

    public void setNotationsPassagers(Set<Rating> ratings) {
        if (this.notationsPassagers != null) {
            this.notationsPassagers.forEach(i -> i.setPassager(null));
        }
        if (ratings != null) {
            ratings.forEach(i -> i.setPassager(this));
        }
        this.notationsPassagers = ratings;
    }

    public People notationsPassagers(Set<Rating> ratings) {
        this.setNotationsPassagers(ratings);
        return this;
    }

    public People addNotationsPassager(Rating rating) {
        this.notationsPassagers.add(rating);
        rating.setPassager(this);
        return this;
    }

    public People removeNotationsPassager(Rating rating) {
        this.notationsPassagers.remove(rating);
        rating.setPassager(null);
        return this;
    }

    public Set<Rating> getNotationsConducteurs() {
        return this.notationsConducteurs;
    }

    public void setNotationsConducteurs(Set<Rating> ratings) {
        if (this.notationsConducteurs != null) {
            this.notationsConducteurs.forEach(i -> i.setConducteur(null));
        }
        if (ratings != null) {
            ratings.forEach(i -> i.setConducteur(this));
        }
        this.notationsConducteurs = ratings;
    }

    public People notationsConducteurs(Set<Rating> ratings) {
        this.setNotationsConducteurs(ratings);
        return this;
    }

    public People addNotationsConducteur(Rating rating) {
        this.notationsConducteurs.add(rating);
        rating.setConducteur(this);
        return this;
    }

    public People removeNotationsConducteur(Rating rating) {
        this.notationsConducteurs.remove(rating);
        rating.setConducteur(null);
        return this;
    }

    public Message getMessagesExpediteur() {
        return this.messagesExpediteur;
    }

    public void setMessagesExpediteur(Message message) {
        this.messagesExpediteur = message;
    }

    public People messagesExpediteur(Message message) {
        this.setMessagesExpediteur(message);
        return this;
    }

    public Message getMessagesDestinatire() {
        return this.messagesDestinatire;
    }

    public void setMessagesDestinatire(Message message) {
        this.messagesDestinatire = message;
    }

    public People messagesDestinatire(Message message) {
        this.setMessagesDestinatire(message);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof People)) {
            return false;
        }
        return getId() != null && getId().equals(((People) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "People{" +
            "id=" + getId() +
            ", nom='" + getNom() + "'" +
            ", prenom='" + getPrenom() + "'" +
            ", telephone='" + getTelephone() + "'" +
            ", cni='" + getCni() + "'" +
            ", photo='" + getPhoto() + "'" +
            ", actif='" + getActif() + "'" +
            ", dateNaissance='" + getDateNaissance() + "'" +
            ", musique='" + getMusique() + "'" +
            ", discussion='" + getDiscussion() + "'" +
            ", cigarette='" + getCigarette() + "'" +
            ", alcool='" + getAlcool() + "'" +
            ", animaux='" + getAnimaux() + "'" +
            "}";
    }
}
