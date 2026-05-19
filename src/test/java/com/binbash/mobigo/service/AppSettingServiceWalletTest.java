package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.binbash.mobigo.domain.AppSetting;
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
        lenient().when(repository.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
    }

    @Test
    void campayFeeRateDefaultsTo2Percent() {
        assertThat(service.getCampayFeeRate()).isEqualTo(0.02);
    }

    @Test
    void minWithdrawalDefaultsTo5000() {
        assertThat(service.getMinWithdrawal()).isEqualByComparingTo(new java.math.BigDecimal("5000"));
    }

    @Test
    void campayFeeRateReadsStoredValue() {
        when(repository.findById("campay_fee_rate")).thenReturn(Optional.of(new AppSetting("campay_fee_rate", "0.03")));
        assertThat(service.getCampayFeeRate()).isEqualTo(0.03);
    }

    @Test
    void campayFeeRateFallsBackOnMalformedValue() {
        when(repository.findById("campay_fee_rate")).thenReturn(Optional.of(new AppSetting("campay_fee_rate", "not-a-number")));
        assertThat(service.getCampayFeeRate()).isEqualTo(0.02);
    }
}
