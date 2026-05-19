package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.binbash.mobigo.repository.AppSettingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppSettingServiceWalletTest {

    @Mock
    private AppSettingRepository repository;

    private AppSettingService service;

    @BeforeEach
    void setUp() {
        service = new AppSettingService(repository);
        when(repository.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
    }

    @Test
    void campayFeeRateDefaultsTo2Percent() {
        assertThat(service.getCampayFeeRate()).isEqualTo(0.02);
    }

    @Test
    void minWithdrawalDefaultsTo5000() {
        assertThat(service.getMinWithdrawal()).isEqualByComparingTo(new java.math.BigDecimal("5000"));
    }
}
