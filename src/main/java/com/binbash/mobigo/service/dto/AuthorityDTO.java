package com.binbash.mobigo.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthorityDTO {

    private String name;
    private String description;
    private String ordre;
    private boolean isPersisted;

    public AuthorityDTO() {}

    public AuthorityDTO(String name, String description, String ordre, boolean isPersisted) {
        this.name = name;
        this.description = description;
        this.ordre = ordre;
        this.isPersisted = isPersisted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrdre() {
        return ordre;
    }

    public void setOrdre(String ordre) {
        this.ordre = ordre;
    }

    public boolean isPersisted() {
        return isPersisted;
    }

    public void setPersisted(boolean isPersisted) {
        this.isPersisted = isPersisted;
    }
}
