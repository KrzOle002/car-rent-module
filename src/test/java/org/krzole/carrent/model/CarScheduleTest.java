package org.krzole.carrent.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.krzole.carrent.model.enums.CarType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarScheduleTest {

    private static final LocalDateTime DAY_1 = LocalDateTime.of(2026, 11, 1, 9, 0);

    private Car car;
    private CarSchedule schedule;

    @BeforeEach
    void setUp() {
        car = new Car("SUV-1", CarType.SUV);
        schedule = new CarSchedule(car);
    }

    private Reservation book(LocalDateTime start, int days) {
        Reservation r = new Reservation(UUID.randomUUID(), "customer", car,
                                        new RentPeriod(start, days));
        schedule.add(r);
        return r;
    }

    @Test
    @DisplayName("An empty schedule is available for any " +
            "period")
    void emptyIsAvailable() {
        assertTrue(schedule.isAvailable(new RentPeriod(DAY_1, 30)));
    }

    @Test
    @DisplayName("Overlapping periods are not " +
            "available, adjacent ones are")
    void overlapAndAdjacency() {
        book(DAY_1.plusDays(5), 3); // days 6..9

        assertFalse(schedule.isAvailable(new RentPeriod(DAY_1.plusDays(4), 2)),
                    "overlaps the start");
        assertFalse(schedule.isAvailable(new RentPeriod(DAY_1.plusDays(7), 5)), "overlaps the end");
        assertFalse(schedule.isAvailable(new RentPeriod(DAY_1, 20)), "covers it entirely");
        assertTrue(schedule.isAvailable(new RentPeriod(DAY_1, 5)), "ends exactly when it starts");
        assertTrue(schedule.isAvailable(new RentPeriod(DAY_1.plusDays(8), 2)),
                   "starts exactly when it ends");
    }

    @Test
    @DisplayName("Period fitting exactly into a gap " +
            "between two bookings is available")
    void fitsIntoGap() {
        book(DAY_1, 2);
        book(DAY_1.plusDays(5), 2);

        assertTrue(schedule.isAvailable(new RentPeriod(DAY_1.plusDays(2), 3)));
        assertFalse(schedule.isAvailable(new RentPeriod(DAY_1.plusDays(2), 4)));
    }

    @Test
    @DisplayName("An earlier long booking is detected " +
            "even when a later one exists")
    void earlierLongBookingIsDetected() {
        book(DAY_1, 10);
        assertFalse(schedule.isAvailable(new RentPeriod(DAY_1.plusDays(9), 1)));
    }

    @Test
    @DisplayName("Adding a conflicting reservation is " +
            "refused")
    void addConflictingThrows() {
        book(DAY_1, 3);
        assertThrows(IllegalStateException.class, () -> book(DAY_1.plusDays(1), 1));
        assertEquals(1, schedule.reservations().size());
    }

    @Test
    @DisplayName("Reservation for another car is refused")
    void addOtherCarThrows() {
        Car other = new Car("SUV-2", CarType.SUV);
        Reservation r = new Reservation(UUID.randomUUID(), "c", other, new RentPeriod(DAY_1, 1));
        assertThrows(IllegalArgumentException.class, () -> schedule.add(r));
    }

    @Test
    @DisplayName("Removing a reservation frees its period")
    void removeFreesPeriod() {
        Reservation r = book(DAY_1, 3);
        assertTrue(schedule.remove(r.getId()));
        assertTrue(schedule.isAvailable(new RentPeriod(DAY_1, 3)));
        assertFalse(schedule.remove(r.getId()));
    }
}
