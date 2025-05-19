package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RatingTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Rating getRatingSample1() {
        return new Rating().id(1L).commentaire("commentaire1");
    }

    public static Rating getRatingSample2() {
        return new Rating().id(2L).commentaire("commentaire2");
    }

    public static Rating getRatingRandomSampleGenerator() {
        return new Rating().id(longCount.incrementAndGet()).commentaire(UUID.randomUUID().toString());
    }
}
