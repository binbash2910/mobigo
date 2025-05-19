package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.MessageStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * A Message.
 */
@Entity
@Table(name = "message")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "message")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Message extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "contenu")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String contenu;

    @NotNull
    @Column(name = "date_envoi", nullable = false)
    private LocalDate dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private MessageStatusEnum statut;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "messagesExpediteur")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(
        value = {
            "vehicules", "bookingsPassagers", "notationsPassagers", "notationsConducteurs", "messagesExpediteur", "messagesDestinatire",
        },
        allowSetters = true
    )
    private Set<People> expediteurs = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "messagesDestinatire")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(
        value = {
            "vehicules", "bookingsPassagers", "notationsPassagers", "notationsConducteurs", "messagesExpediteur", "messagesDestinatire",
        },
        allowSetters = true
    )
    private Set<People> destinataires = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Message id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenu() {
        return this.contenu;
    }

    public Message contenu(String contenu) {
        this.setContenu(contenu);
        return this;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDate getDateEnvoi() {
        return this.dateEnvoi;
    }

    public Message dateEnvoi(LocalDate dateEnvoi) {
        this.setDateEnvoi(dateEnvoi);
        return this;
    }

    public void setDateEnvoi(LocalDate dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public MessageStatusEnum getStatut() {
        return this.statut;
    }

    public Message statut(MessageStatusEnum statut) {
        this.setStatut(statut);
        return this;
    }

    public void setStatut(MessageStatusEnum statut) {
        this.statut = statut;
    }

    public Set<People> getExpediteurs() {
        return this.expediteurs;
    }

    public void setExpediteurs(Set<People> people) {
        if (this.expediteurs != null) {
            this.expediteurs.forEach(i -> i.setMessagesExpediteur(null));
        }
        if (people != null) {
            people.forEach(i -> i.setMessagesExpediteur(this));
        }
        this.expediteurs = people;
    }

    public Message expediteurs(Set<People> people) {
        this.setExpediteurs(people);
        return this;
    }

    public Message addExpediteur(People people) {
        this.expediteurs.add(people);
        people.setMessagesExpediteur(this);
        return this;
    }

    public Message removeExpediteur(People people) {
        this.expediteurs.remove(people);
        people.setMessagesExpediteur(null);
        return this;
    }

    public Set<People> getDestinataires() {
        return this.destinataires;
    }

    public void setDestinataires(Set<People> people) {
        if (this.destinataires != null) {
            this.destinataires.forEach(i -> i.setMessagesDestinatire(null));
        }
        if (people != null) {
            people.forEach(i -> i.setMessagesDestinatire(this));
        }
        this.destinataires = people;
    }

    public Message destinataires(Set<People> people) {
        this.setDestinataires(people);
        return this;
    }

    public Message addDestinataire(People people) {
        this.destinataires.add(people);
        people.setMessagesDestinatire(this);
        return this;
    }

    public Message removeDestinataire(People people) {
        this.destinataires.remove(people);
        people.setMessagesDestinatire(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        return getId() != null && getId().equals(((Message) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Message{" +
            "id=" + getId() +
            ", contenu='" + getContenu() + "'" +
            ", dateEnvoi='" + getDateEnvoi() + "'" +
            ", statut='" + getStatut() + "'" +
            "}";
    }
}
