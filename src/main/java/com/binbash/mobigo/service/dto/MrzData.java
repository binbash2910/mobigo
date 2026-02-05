package com.binbash.mobigo.service.dto;

import java.time.LocalDate;

/**
 * Holds data extracted from the MRZ (Machine Readable Zone) of an identity document.
 */
public class MrzData {

    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String documentNumber;
    private LocalDate dateExpiration;
    private String sexe;
    private String format; // "TD1", "TD2", or "TD3" (passport)
    private boolean valid;
    private String rawMrz;
    private String documentType; // "CNI", "PASSPORT", "RESIDENCE_PERMIT"
    private String issuingCountry; // "CMR", "FRA"

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getRawMrz() {
        return rawMrz;
    }

    public void setRawMrz(String rawMrz) {
        this.rawMrz = rawMrz;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getIssuingCountry() {
        return issuingCountry;
    }

    public void setIssuingCountry(String issuingCountry) {
        this.issuingCountry = issuingCountry;
    }
}
