package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class StepTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Step getStepSample1() {
        return new Step().id(1L).ville("ville1").heureDepart("heureDepart1");
    }

    public static Step getStepSample2() {
        return new Step().id(2L).ville("ville2").heureDepart("heureDepart2");
    }

    public static Step getStepRandomSampleGenerator() {
        return new Step().id(longCount.incrementAndGet()).ville(UUID.randomUUID().toString()).heureDepart(UUID.randomUUID().toString());
    }
}
