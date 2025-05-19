package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.BookingTestSamples.*;
import static com.binbash.mobigo.domain.RatingTestSamples.*;
import static com.binbash.mobigo.domain.RideTestSamples.*;
import static com.binbash.mobigo.domain.StepTestSamples.*;
import static com.binbash.mobigo.domain.VehicleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RideTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Ride.class);
        Ride ride1 = getRideSample1();
        Ride ride2 = new Ride();
        assertThat(ride1).isNotEqualTo(ride2);

        ride2.setId(ride1.getId());
        assertThat(ride1).isEqualTo(ride2);

        ride2 = getRideSample2();
        assertThat(ride1).isNotEqualTo(ride2);
    }

    @Test
    void stepsTest() {
        Ride ride = getRideRandomSampleGenerator();
        Step stepBack = getStepRandomSampleGenerator();

        ride.addSteps(stepBack);
        assertThat(ride.getSteps()).containsOnly(stepBack);
        assertThat(stepBack.getTrajet()).isEqualTo(ride);

        ride.removeSteps(stepBack);
        assertThat(ride.getSteps()).doesNotContain(stepBack);
        assertThat(stepBack.getTrajet()).isNull();

        ride.steps(new HashSet<>(Set.of(stepBack)));
        assertThat(ride.getSteps()).containsOnly(stepBack);
        assertThat(stepBack.getTrajet()).isEqualTo(ride);

        ride.setSteps(new HashSet<>());
        assertThat(ride.getSteps()).doesNotContain(stepBack);
        assertThat(stepBack.getTrajet()).isNull();
    }

    @Test
    void bookingsTrajetTest() {
        Ride ride = getRideRandomSampleGenerator();
        Booking bookingBack = getBookingRandomSampleGenerator();

        ride.addBookingsTrajet(bookingBack);
        assertThat(ride.getBookingsTrajets()).containsOnly(bookingBack);
        assertThat(bookingBack.getTrajet()).isEqualTo(ride);

        ride.removeBookingsTrajet(bookingBack);
        assertThat(ride.getBookingsTrajets()).doesNotContain(bookingBack);
        assertThat(bookingBack.getTrajet()).isNull();

        ride.bookingsTrajets(new HashSet<>(Set.of(bookingBack)));
        assertThat(ride.getBookingsTrajets()).containsOnly(bookingBack);
        assertThat(bookingBack.getTrajet()).isEqualTo(ride);

        ride.setBookingsTrajets(new HashSet<>());
        assertThat(ride.getBookingsTrajets()).doesNotContain(bookingBack);
        assertThat(bookingBack.getTrajet()).isNull();
    }

    @Test
    void notationsTest() {
        Ride ride = getRideRandomSampleGenerator();
        Rating ratingBack = getRatingRandomSampleGenerator();

        ride.addNotations(ratingBack);
        assertThat(ride.getNotations()).containsOnly(ratingBack);
        assertThat(ratingBack.getTrajet()).isEqualTo(ride);

        ride.removeNotations(ratingBack);
        assertThat(ride.getNotations()).doesNotContain(ratingBack);
        assertThat(ratingBack.getTrajet()).isNull();

        ride.notations(new HashSet<>(Set.of(ratingBack)));
        assertThat(ride.getNotations()).containsOnly(ratingBack);
        assertThat(ratingBack.getTrajet()).isEqualTo(ride);

        ride.setNotations(new HashSet<>());
        assertThat(ride.getNotations()).doesNotContain(ratingBack);
        assertThat(ratingBack.getTrajet()).isNull();
    }

    @Test
    void vehiculeTest() {
        Ride ride = getRideRandomSampleGenerator();
        Vehicle vehicleBack = getVehicleRandomSampleGenerator();

        ride.setVehicule(vehicleBack);
        assertThat(ride.getVehicule()).isEqualTo(vehicleBack);

        ride.vehicule(null);
        assertThat(ride.getVehicule()).isNull();
    }
}
