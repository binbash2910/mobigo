package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VehicleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Vehicle getVehicleSample1() {
        return new Vehicle()
            .id(1L)
            .marque("marque1")
            .modele("modele1")
            .annee("annee1")
            .carteGrise("carteGrise1")
            .immatriculation("immatriculation1")
            .nbPlaces(1)
            .couleur("couleur1")
            .photo("photo1")
            .actif("actif1");
    }

    public static Vehicle getVehicleSample2() {
        return new Vehicle()
            .id(2L)
            .marque("marque2")
            .modele("modele2")
            .annee("annee2")
            .carteGrise("carteGrise2")
            .immatriculation("immatriculation2")
            .nbPlaces(2)
            .couleur("couleur2")
            .photo("photo2")
            .actif("actif2");
    }

    public static Vehicle getVehicleRandomSampleGenerator() {
        return new Vehicle()
            .id(longCount.incrementAndGet())
            .marque(UUID.randomUUID().toString())
            .modele(UUID.randomUUID().toString())
            .annee(UUID.randomUUID().toString())
            .carteGrise(UUID.randomUUID().toString())
            .immatriculation(UUID.randomUUID().toString())
            .nbPlaces(intCount.incrementAndGet())
            .couleur(UUID.randomUUID().toString())
            .photo(UUID.randomUUID().toString())
            .actif(UUID.randomUUID().toString());
    }
}
