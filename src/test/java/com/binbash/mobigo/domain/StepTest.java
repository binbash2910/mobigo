package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.RideTestSamples.*;
import static com.binbash.mobigo.domain.StepTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StepTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Step.class);
        Step step1 = getStepSample1();
        Step step2 = new Step();
        assertThat(step1).isNotEqualTo(step2);

        step2.setId(step1.getId());
        assertThat(step1).isEqualTo(step2);

        step2 = getStepSample2();
        assertThat(step1).isNotEqualTo(step2);
    }

    @Test
    void trajetTest() {
        Step step = getStepRandomSampleGenerator();
        Ride rideBack = getRideRandomSampleGenerator();

        step.setTrajet(rideBack);
        assertThat(step.getTrajet()).isEqualTo(rideBack);

        step.trajet(null);
        assertThat(step.getTrajet()).isNull();
    }
}
