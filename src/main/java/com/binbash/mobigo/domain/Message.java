package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.MessageStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;

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
    private People expediteur;

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
    private People destinataire;

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

    public People getExpediteur() {
        return this.expediteur;
    }

    public void setExpediteur(People people) {
        this.expediteur = people;
    }

    public Message expediteur(People people) {
        this.setExpediteur(people);
        return this;
    }

    public People getDestinataire() {
        return this.destinataire;
    }

    public void setDestinataire(People people) {
        this.destinataire = people;
    }

    public Message destinataire(People people) {
        this.setDestinataire(people);
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
