package org.krzole.carrent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.krzole.carrent.model.enums.CarType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FleetTest {

    @Test
    @DisplayName("Capacity per type equals the number of cars of that type")
    void capacityPerType() {
        Fleet fleet = Fleet.withCounts(3, 2, 0);
        assertEquals(3, fleet.capacityOf(CarType.SEDAN));
        assertEquals(2, fleet.capacityOf(CarType.SUV));
        assertEquals(0, fleet.capacityOf(CarType.VAN));
    }

    @Test
    @DisplayName("Duplicate VINs are rejected")
    void duplicateVinsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Fleet.of(List.of(
                new Car("X", CarType.SEDAN),
                new Car("X", CarType.VAN))));
    }

    @Test
    @DisplayName("The fleet cannot be modified from outside")
    void fleetIsImmutable() {
        Fleet fleet = Fleet.withCounts(1, 1, 1);
        assertThrows(UnsupportedOperationException.class,
                     () -> fleet.carsOf(CarType.SEDAN).add(new Car("S-2", CarType.SEDAN)));
    }
}
