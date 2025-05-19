package com.binbash.mobigo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * A Step.
 */
@Entity
@Table(name = "step")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "step")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Step extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "ville", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String ville;

    @NotNull
    @Column(name = "heure_depart", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String heureDepart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "steps", "bookingsTrajets", "notations", "vehicule" }, allowSetters = true)
    private Ride trajet;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Step id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVille() {
        return this.ville;
    }

    public Step ville(String ville) {
        this.setVille(ville);
        return this;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getHeureDepart() {
        return this.heureDepart;
    }

    public Step heureDepart(String heureDepart) {
        this.setHeureDepart(heureDepart);
        return this;
    }

    public void setHeureDepart(String heureDepart) {
        this.heureDepart = heureDepart;
    }

    public Ride getTrajet() {
        return this.trajet;
    }

    public void setTrajet(Ride ride) {
        this.trajet = ride;
    }

    public Step trajet(Ride ride) {
        this.setTrajet(ride);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Step)) {
            return false;
        }
        return getId() != null && getId().equals(((Step) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Step{" +
            "id=" + getId() +
            ", ville='" + getVille() + "'" +
            ", heureDepart='" + getHeureDepart() + "'" +
            "}";
    }
}
