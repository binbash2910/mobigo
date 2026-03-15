package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {}
