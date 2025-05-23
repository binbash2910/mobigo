package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PeopleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static People getPeopleSample1() {
        return new People()
            .id(1L)
            .nom("nom1")
            .prenom("prenom1")
            .telephone("telephone1")
            .cni("cni1")
            .photo("photo1")
            .actif("actif1")
            .musique("musique1")
            .discussion("discussion1")
            .cigarette("cigarette1")
            .alcool("alcool1")
            .animaux("animaux1")
            .conducteur("conducteur1")
            .passager("passager1");
    }

    public static People getPeopleSample2() {
        return new People()
            .id(2L)
            .nom("nom2")
            .prenom("prenom2")
            .telephone("telephone2")
            .cni("cni2")
            .photo("photo2")
            .actif("actif2")
            .musique("musique2")
            .discussion("discussion2")
            .cigarette("cigarette2")
            .alcool("alcool2")
            .animaux("animaux2")
            .conducteur("conducteur2")
            .passager("passager2");
    }

    public static People getPeopleRandomSampleGenerator() {
        return new People()
            .id(longCount.incrementAndGet())
            .nom(UUID.randomUUID().toString())
            .prenom(UUID.randomUUID().toString())
            .telephone(UUID.randomUUID().toString())
            .cni(UUID.randomUUID().toString())
            .photo(UUID.randomUUID().toString())
            .actif(UUID.randomUUID().toString())
            .musique(UUID.randomUUID().toString())
            .discussion(UUID.randomUUID().toString())
            .cigarette(UUID.randomUUID().toString())
            .alcool(UUID.randomUUID().toString())
            .animaux(UUID.randomUUID().toString())
            .conducteur(UUID.randomUUID().toString())
            .passager(UUID.randomUUID().toString());
    }
}
