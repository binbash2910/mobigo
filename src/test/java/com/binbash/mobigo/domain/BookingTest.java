package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.BookingTestSamples.*;
import static com.binbash.mobigo.domain.PaymentTestSamples.*;
import static com.binbash.mobigo.domain.PeopleTestSamples.*;
import static com.binbash.mobigo.domain.RideTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Booking.class);
        Booking booking1 = getBookingSample1();
        Booking booking2 = new Booking();
        assertThat(booking1).isNotEqualTo(booking2);

        booking2.setId(booking1.getId());
        assertThat(booking1).isEqualTo(booking2);

        booking2 = getBookingSample2();
        assertThat(booking1).isNotEqualTo(booking2);
    }

    @Test
    void payementTest() {
        Booking booking = getBookingRandomSampleGenerator();
        Payment paymentBack = getPaymentRandomSampleGenerator();

        booking.setPayement(paymentBack);
        assertThat(booking.getPayement()).isEqualTo(paymentBack);
        assertThat(paymentBack.getBooking()).isEqualTo(booking);

        booking.payement(null);
        assertThat(booking.getPayement()).isNull();
        assertThat(paymentBack.getBooking()).isNull();
    }

    @Test
    void trajetTest() {
        Booking booking = getBookingRandomSampleGenerator();
        Ride rideBack = getRideRandomSampleGenerator();

        booking.setTrajet(rideBack);
        assertThat(booking.getTrajet()).isEqualTo(rideBack);

        booking.trajet(null);
        assertThat(booking.getTrajet()).isNull();
    }

    @Test
    void passagerTest() {
        Booking booking = getBookingRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        booking.setPassager(peopleBack);
        assertThat(booking.getPassager()).isEqualTo(peopleBack);

        booking.passager(null);
        assertThat(booking.getPassager()).isNull();
    }
}
