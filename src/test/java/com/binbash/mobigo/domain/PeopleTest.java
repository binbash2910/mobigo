package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.BookingTestSamples.*;
import static com.binbash.mobigo.domain.MessageTestSamples.*;
import static com.binbash.mobigo.domain.PeopleTestSamples.*;
import static com.binbash.mobigo.domain.RatingTestSamples.*;
import static com.binbash.mobigo.domain.VehicleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PeopleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(People.class);
        People people1 = getPeopleSample1();
        People people2 = new People();
        assertThat(people1).isNotEqualTo(people2);

        people2.setId(people1.getId());
        assertThat(people1).isEqualTo(people2);

        people2 = getPeopleSample2();
        assertThat(people1).isNotEqualTo(people2);
    }

    @Test
    void vehiculesTest() {
        People people = getPeopleRandomSampleGenerator();
        Vehicle vehicleBack = getVehicleRandomSampleGenerator();

        people.addVehicules(vehicleBack);
        assertThat(people.getVehicules()).containsOnly(vehicleBack);
        assertThat(vehicleBack.getProprietaire()).isEqualTo(people);

        people.removeVehicules(vehicleBack);
        assertThat(people.getVehicules()).doesNotContain(vehicleBack);
        assertThat(vehicleBack.getProprietaire()).isNull();

        people.vehicules(new HashSet<>(Set.of(vehicleBack)));
        assertThat(people.getVehicules()).containsOnly(vehicleBack);
        assertThat(vehicleBack.getProprietaire()).isEqualTo(people);

        people.setVehicules(new HashSet<>());
        assertThat(people.getVehicules()).doesNotContain(vehicleBack);
        assertThat(vehicleBack.getProprietaire()).isNull();
    }

    @Test
    void bookingsPassagerTest() {
        People people = getPeopleRandomSampleGenerator();
        Booking bookingBack = getBookingRandomSampleGenerator();

        people.addBookingsPassager(bookingBack);
        assertThat(people.getBookingsPassagers()).containsOnly(bookingBack);
        assertThat(bookingBack.getPassager()).isEqualTo(people);

        people.removeBookingsPassager(bookingBack);
        assertThat(people.getBookingsPassagers()).doesNotContain(bookingBack);
        assertThat(bookingBack.getPassager()).isNull();

        people.bookingsPassagers(new HashSet<>(Set.of(bookingBack)));
        assertThat(people.getBookingsPassagers()).containsOnly(bookingBack);
        assertThat(bookingBack.getPassager()).isEqualTo(people);

        people.setBookingsPassagers(new HashSet<>());
        assertThat(people.getBookingsPassagers()).doesNotContain(bookingBack);
        assertThat(bookingBack.getPassager()).isNull();
    }

    @Test
    void notationsPassagerTest() {
        People people = getPeopleRandomSampleGenerator();
        Rating ratingBack = getRatingRandomSampleGenerator();

        people.addNotationsPassager(ratingBack);
        assertThat(people.getNotationsPassagers()).containsOnly(ratingBack);
        assertThat(ratingBack.getPassager()).isEqualTo(people);

        people.removeNotationsPassager(ratingBack);
        assertThat(people.getNotationsPassagers()).doesNotContain(ratingBack);
        assertThat(ratingBack.getPassager()).isNull();

        people.notationsPassagers(new HashSet<>(Set.of(ratingBack)));
        assertThat(people.getNotationsPassagers()).containsOnly(ratingBack);
        assertThat(ratingBack.getPassager()).isEqualTo(people);

        people.setNotationsPassagers(new HashSet<>());
        assertThat(people.getNotationsPassagers()).doesNotContain(ratingBack);
        assertThat(ratingBack.getPassager()).isNull();
    }

    @Test
    void notationsConducteurTest() {
        People people = getPeopleRandomSampleGenerator();
        Rating ratingBack = getRatingRandomSampleGenerator();

        people.addNotationsConducteur(ratingBack);
        assertThat(people.getNotationsConducteurs()).containsOnly(ratingBack);
        assertThat(ratingBack.getConducteur()).isEqualTo(people);

        people.removeNotationsConducteur(ratingBack);
        assertThat(people.getNotationsConducteurs()).doesNotContain(ratingBack);
        assertThat(ratingBack.getConducteur()).isNull();

        people.notationsConducteurs(new HashSet<>(Set.of(ratingBack)));
        assertThat(people.getNotationsConducteurs()).containsOnly(ratingBack);
        assertThat(ratingBack.getConducteur()).isEqualTo(people);

        people.setNotationsConducteurs(new HashSet<>());
        assertThat(people.getNotationsConducteurs()).doesNotContain(ratingBack);
        assertThat(ratingBack.getConducteur()).isNull();
    }

    @Test
    void messagesExpediteurTest() {
        People people = getPeopleRandomSampleGenerator();
        Message messageBack = getMessageRandomSampleGenerator();

        people.addMessagesExpediteur(messageBack);
        assertThat(people.getMessagesExpediteurs()).containsOnly(messageBack);
        assertThat(messageBack.getExpediteur()).isEqualTo(people);

        people.removeMessagesExpediteur(messageBack);
        assertThat(people.getMessagesExpediteurs()).doesNotContain(messageBack);
        assertThat(messageBack.getExpediteur()).isNull();

        people.messagesExpediteurs(new HashSet<>(Set.of(messageBack)));
        assertThat(people.getMessagesExpediteurs()).containsOnly(messageBack);
        assertThat(messageBack.getExpediteur()).isEqualTo(people);

        people.setMessagesExpediteurs(new HashSet<>());
        assertThat(people.getMessagesExpediteurs()).doesNotContain(messageBack);
        assertThat(messageBack.getExpediteur()).isNull();
    }

    @Test
    void messagesDestinataireTest() {
        People people = getPeopleRandomSampleGenerator();
        Message messageBack = getMessageRandomSampleGenerator();

        people.addMessagesDestinataire(messageBack);
        assertThat(people.getMessagesDestinataires()).containsOnly(messageBack);
        assertThat(messageBack.getDestinataire()).isEqualTo(people);

        people.removeMessagesDestinataire(messageBack);
        assertThat(people.getMessagesDestinataires()).doesNotContain(messageBack);
        assertThat(messageBack.getDestinataire()).isNull();

        people.messagesDestinataires(new HashSet<>(Set.of(messageBack)));
        assertThat(people.getMessagesDestinataires()).containsOnly(messageBack);
        assertThat(messageBack.getDestinataire()).isEqualTo(people);

        people.setMessagesDestinataires(new HashSet<>());
        assertThat(people.getMessagesDestinataires()).doesNotContain(messageBack);
        assertThat(messageBack.getDestinataire()).isNull();
    }
}
