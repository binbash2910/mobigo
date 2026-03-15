package com.binbash.mobigo.domain;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "app_setting")
public class AppSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(name = "setting_value", length = 500)
    private String value;

    public AppSetting() {}

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
