package com.binbash.mobigo.domain;

import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * A SavedPaymentMethod.
 */
@Entity
@Table(name = "saved_payment_method")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "savedpaymentmethod")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SavedPaymentMethod extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private PaymentMethodEnum type;

    @Column(name = "last4")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String last4;

    @Column(name = "holder_name")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String holderName;

    @Column(name = "expiry_date")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String expiryDate;

    @Column(name = "phone")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String phone;

    @Column(name = "label")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String label;

    @NotNull
    @Column(name = "is_default", nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Boolean)
    private Boolean isDefault = false;

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
    private People proprietaire;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SavedPaymentMethod id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentMethodEnum getType() {
        return this.type;
    }

    public SavedPaymentMethod type(PaymentMethodEnum type) {
        this.setType(type);
        return this;
    }

    public void setType(PaymentMethodEnum type) {
        this.type = type;
    }

    public String getLast4() {
        return this.last4;
    }

    public SavedPaymentMethod last4(String last4) {
        this.setLast4(last4);
        return this;
    }

    public void setLast4(String last4) {
        this.last4 = last4;
    }

    public String getHolderName() {
        return this.holderName;
    }

    public SavedPaymentMethod holderName(String holderName) {
        this.setHolderName(holderName);
        return this;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getExpiryDate() {
        return this.expiryDate;
    }

    public SavedPaymentMethod expiryDate(String expiryDate) {
        this.setExpiryDate(expiryDate);
        return this;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPhone() {
        return this.phone;
    }

    public SavedPaymentMethod phone(String phone) {
        this.setPhone(phone);
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLabel() {
        return this.label;
    }

    public SavedPaymentMethod label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getIsDefault() {
        return this.isDefault;
    }

    public SavedPaymentMethod isDefault(Boolean isDefault) {
        this.setIsDefault(isDefault);
        return this;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public People getProprietaire() {
        return this.proprietaire;
    }

    public void setProprietaire(People people) {
        this.proprietaire = people;
    }

    public SavedPaymentMethod proprietaire(People people) {
        this.setProprietaire(people);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SavedPaymentMethod)) {
            return false;
        }
        return getId() != null && getId().equals(((SavedPaymentMethod) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SavedPaymentMethod{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", last4='" + getLast4() + "'" +
            ", holderName='" + getHolderName() + "'" +
            ", expiryDate='" + getExpiryDate() + "'" +
            ", phone='" + getPhone() + "'" +
            ", label='" + getLabel() + "'" +
            ", isDefault='" + getIsDefault() + "'" +
            "}";
    }
}
