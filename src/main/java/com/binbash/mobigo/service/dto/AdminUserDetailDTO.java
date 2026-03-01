package com.binbash.mobigo.service.dto;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.User;
import java.time.Instant;

/**
 * A DTO representing an admin user with their People details (phone, photo, CNI status).
 */
public class AdminUserDetailDTO extends AdminUserDTO {

    private static final long serialVersionUID = 1L;

    private Long peopleId;
    private String telephone;
    private String photo;
    private String cniStatut;
    private Instant cniVerifieAt;

    public AdminUserDetailDTO() {
        // Empty constructor needed for Jackson.
    }

    public AdminUserDetailDTO(User user, People people) {
        super(user);
        if (people != null) {
            this.peopleId = people.getId();
            this.telephone = people.getTelephone();
            this.photo = people.getPhoto();
            this.cniStatut = people.getCniStatut();
            this.cniVerifieAt = people.getCniVerifieAt();
        }
    }

    public Long getPeopleId() {
        return peopleId;
    }

    public void setPeopleId(Long peopleId) {
        this.peopleId = peopleId;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getCniStatut() {
        return cniStatut;
    }

    public void setCniStatut(String cniStatut) {
        this.cniStatut = cniStatut;
    }

    public Instant getCniVerifieAt() {
        return cniVerifieAt;
    }

    public void setCniVerifieAt(Instant cniVerifieAt) {
        this.cniVerifieAt = cniVerifieAt;
    }
}
