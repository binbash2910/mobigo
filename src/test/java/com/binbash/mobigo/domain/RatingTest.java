package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.PeopleTestSamples.*;
import static com.binbash.mobigo.domain.RatingTestSamples.*;
import static com.binbash.mobigo.domain.RideTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Rating.class);
        Rating rating1 = getRatingSample1();
        Rating rating2 = new Rating();
        assertThat(rating1).isNotEqualTo(rating2);

        rating2.setId(rating1.getId());
        assertThat(rating1).isEqualTo(rating2);

        rating2 = getRatingSample2();
        assertThat(rating1).isNotEqualTo(rating2);
    }

    @Test
    void trajetTest() {
        Rating rating = getRatingRandomSampleGenerator();
        Ride rideBack = getRideRandomSampleGenerator();

        rating.setTrajet(rideBack);
        assertThat(rating.getTrajet()).isEqualTo(rideBack);

        rating.trajet(null);
        assertThat(rating.getTrajet()).isNull();
    }

    @Test
    void passagerTest() {
        Rating rating = getRatingRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        rating.setPassager(peopleBack);
        assertThat(rating.getPassager()).isEqualTo(peopleBack);

        rating.passager(null);
        assertThat(rating.getPassager()).isNull();
    }

    @Test
    void conducteurTest() {
        Rating rating = getRatingRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        rating.setConducteur(peopleBack);
        assertThat(rating.getConducteur()).isEqualTo(peopleBack);

        rating.conducteur(null);
        assertThat(rating.getConducteur()).isNull();
    }
}
