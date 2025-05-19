package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RideTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Ride getRideSample1() {
        return new Ride()
            .id(1L)
            .villeDepart("villeDepart1")
            .villeArrivee("villeArrivee1")
            .heureDepart("heureDepart1")
            .heureArrivee("heureArrivee1")
            .minuteDepart("minuteDepart1")
            .minuteArrivee("minuteArrivee1")
            .nbrePlaceDisponible(1);
    }

    public static Ride getRideSample2() {
        return new Ride()
            .id(2L)
            .villeDepart("villeDepart2")
            .villeArrivee("villeArrivee2")
            .heureDepart("heureDepart2")
            .heureArrivee("heureArrivee2")
            .minuteDepart("minuteDepart2")
            .minuteArrivee("minuteArrivee2")
            .nbrePlaceDisponible(2);
    }

    public static Ride getRideRandomSampleGenerator() {
        return new Ride()
            .id(longCount.incrementAndGet())
            .villeDepart(UUID.randomUUID().toString())
            .villeArrivee(UUID.randomUUID().toString())
            .heureDepart(UUID.randomUUID().toString())
            .heureArrivee(UUID.randomUUID().toString())
            .minuteDepart(UUID.randomUUID().toString())
            .minuteArrivee(UUID.randomUUID().toString())
            .nbrePlaceDisponible(intCount.incrementAndGet());
    }
}
