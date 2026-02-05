package com.binbash.mobigo.service.dto;

import java.time.LocalDate;

/**
 * DTO returned by the identity document verification endpoint.
 * Contains extracted MRZ data and match indicators against the user profile.
 */
public class CniVerificationDTO {

    private boolean verified;
    private String status; // VERIFIED, REJECTED, EXPIRED
    private String documentNumber;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private LocalDate dateExpiration;
    private String sexe;
    private boolean nomMatch;
    private boolean prenomMatch;
    private boolean dateNaissanceMatch;
    private boolean documentExpired;
    private String mrzFormat;
    private String message;
    private String documentType; // "CNI", "PASSPORT", "RESIDENCE_PERMIT"
    private String issuingCountry; // "CMR", "FRA"

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

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

    public boolean isNomMatch() {
        return nomMatch;
    }

    public void setNomMatch(boolean nomMatch) {
        this.nomMatch = nomMatch;
    }

    public boolean isPrenomMatch() {
        return prenomMatch;
    }

    public void setPrenomMatch(boolean prenomMatch) {
        this.prenomMatch = prenomMatch;
    }

    public boolean isDateNaissanceMatch() {
        return dateNaissanceMatch;
    }

    public void setDateNaissanceMatch(boolean dateNaissanceMatch) {
        this.dateNaissanceMatch = dateNaissanceMatch;
    }

    public boolean isDocumentExpired() {
        return documentExpired;
    }

    public void setDocumentExpired(boolean documentExpired) {
        this.documentExpired = documentExpired;
    }

    public String getMrzFormat() {
        return mrzFormat;
    }

    public void setMrzFormat(String mrzFormat) {
        this.mrzFormat = mrzFormat;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
