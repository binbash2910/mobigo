package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.AppSetting;
import com.binbash.mobigo.repository.AppSettingRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppSettingService {

    private static final Logger LOG = LoggerFactory.getLogger(AppSettingService.class);

    public static final String COMMISSION_RATE = "commission_rate";
    private static final String DEFAULT_COMMISSION_RATE = "0.10";

    private final AppSettingRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public String get(String key, String defaultValue) {
        String cached = cache.get(key);
        if (cached != null) return cached;

        String value = repository.findById(key).map(AppSetting::getValue).orElse(defaultValue);

        cache.put(key, value);
        return value;
    }

    public void set(String key, String value) {
        AppSetting setting = repository.findById(key).orElse(new AppSetting(key, value));
        setting.setValue(value);
        repository.save(setting);
        cache.put(key, value);
        LOG.info("Setting updated: {} = {}", key, value);
    }

    public double getCommissionRate() {
        String value = get(COMMISSION_RATE, DEFAULT_COMMISSION_RATE);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid commission rate in DB: {}, using default", value);
            return 0.10;
        }
    }

    public void setCommissionRate(double rate) {
        set(COMMISSION_RATE, String.valueOf(rate));
    }
}
