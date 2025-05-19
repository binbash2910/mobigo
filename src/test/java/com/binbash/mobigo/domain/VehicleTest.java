package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.PeopleTestSamples.*;
import static com.binbash.mobigo.domain.RideTestSamples.*;
import static com.binbash.mobigo.domain.VehicleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VehicleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Vehicle.class);
        Vehicle vehicle1 = getVehicleSample1();
        Vehicle vehicle2 = new Vehicle();
        assertThat(vehicle1).isNotEqualTo(vehicle2);

        vehicle2.setId(vehicle1.getId());
        assertThat(vehicle1).isEqualTo(vehicle2);

        vehicle2 = getVehicleSample2();
        assertThat(vehicle1).isNotEqualTo(vehicle2);
    }

    @Test
    void trajetsTest() {
        Vehicle vehicle = getVehicleRandomSampleGenerator();
        Ride rideBack = getRideRandomSampleGenerator();

        vehicle.addTrajets(rideBack);
        assertThat(vehicle.getTrajets()).containsOnly(rideBack);
        assertThat(rideBack.getVehicule()).isEqualTo(vehicle);

        vehicle.removeTrajets(rideBack);
        assertThat(vehicle.getTrajets()).doesNotContain(rideBack);
        assertThat(rideBack.getVehicule()).isNull();

        vehicle.trajets(new HashSet<>(Set.of(rideBack)));
        assertThat(vehicle.getTrajets()).containsOnly(rideBack);
        assertThat(rideBack.getVehicule()).isEqualTo(vehicle);

        vehicle.setTrajets(new HashSet<>());
        assertThat(vehicle.getTrajets()).doesNotContain(rideBack);
        assertThat(rideBack.getVehicule()).isNull();
    }

    @Test
    void proprietaireTest() {
        Vehicle vehicle = getVehicleRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        vehicle.setProprietaire(peopleBack);
        assertThat(vehicle.getProprietaire()).isEqualTo(peopleBack);

        vehicle.proprietaire(null);
        assertThat(vehicle.getProprietaire()).isNull();
    }
}
