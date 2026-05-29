package com.josephyusuf.auth.repository;

import com.josephyusuf.auth.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
