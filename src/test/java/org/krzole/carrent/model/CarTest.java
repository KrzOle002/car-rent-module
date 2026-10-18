package org.krzole.carrent.model;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.krzole.carrent.model.enums.CarType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarTest {

    @Test
    @DisplayName("Every new car gets its own generated id")
    void idsAreGeneratedAndUnique() {
        Car first = new Car("WVWZZZ1JZXW000001", CarType.SEDAN);
        Car second = new Car("WVWZZZ1JZXW000002", CarType.SEDAN);

        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first, second);
        assertEquals("WVWZZZ1JZXW000001", first.getVin());
    }

    @Test
    @DisplayName("Blank or missing VIN is rejected")
    void blankVinRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Car(" ", CarType.SUV));
        assertThrows(NullPointerException.class, () -> new Car(null, CarType.SUV));
        assertThrows(NullPointerException.class, () -> new Car("VIN", null));
    }
}
