package org.krzole.carrent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.krzole.carrent.exception.NoCarAvailableException;
import org.krzole.carrent.exception.ReservationNotFoundException;
import org.krzole.carrent.model.Fleet;
import org.krzole.carrent.model.Reservation;
import org.krzole.carrent.model.enums.CarType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarRentServiceTest {

    /**
     * "Now" is fixed so tests never depend on the real date.
     */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"),
                                                   ZoneOffset.UTC);
    private static final LocalDateTime NEXT_WEEK = LocalDateTime.of(2026, 10, 23, 10, 0);

    private CarRentService service;

    @BeforeEach
    void setUp() {
        service = new CarRentService(Fleet.withCounts(2, 1, 3), CLOCK);
    }

    @Test
    @DisplayName("Reserves a car of the requested type for the requested date, time and days")
    void reservesRequestedTypeAndPeriod() {
        Reservation r = service.reserve("alice", CarType.SUV, NEXT_WEEK, 3);

        assertEquals(CarType.SUV, r.getCarType());
        assertEquals(NEXT_WEEK, r.getPeriod().getStart());
        assertEquals(NEXT_WEEK.plusDays(3), r.getPeriod().getEnd());
        assertEquals("alice", r.getCustomerId());
        assertEquals(Optional.of(r), service.findReservation(r.getId()));
    }

    @Test
    @DisplayName("Each of the three car types can be reserved")
    void allThreeTypesReservable() {
        for (CarType type : CarType.values()) {
            assertEquals(type, service.reserve("c", type, NEXT_WEEK, 1).getCarType());
        }
    }

    @Test
    @DisplayName("Can reserve exactly as many cars of a type as the fleet has, then no more")
    void capacityIsEnforced() {
        Reservation first = service.reserve("a", CarType.SEDAN, NEXT_WEEK, 2);
        Reservation second = service.reserve("b", CarType.SEDAN, NEXT_WEEK, 2);

        assertNotEquals(first.getCar(), second.getCar(), "two concurrent bookings must get " +
                "different" +
                " " +
                "cars");
        NoCarAvailableException ex = assertThrows(NoCarAvailableException.class,
                                                  () -> service.reserve("c", CarType.SEDAN,
                                                                        NEXT_WEEK, 2));
        assertEquals(CarType.SEDAN, ex.carType());
    }

    @Test
    @DisplayName("Partially overlapping request is refused when all cars are busy")
    void partialOverlapRefusedWhenFull() {
        service.reserve("a", CarType.SUV, NEXT_WEEK, 3);
        assertThrows(NoCarAvailableException.class,
                     () -> service.reserve("b", CarType.SUV, NEXT_WEEK.plusDays(2), 3));
        assertThrows(NoCarAvailableException.class,
                     () -> service.reserve("b", CarType.SUV, NEXT_WEEK.minusDays(1), 2));
    }

    @Test
    @DisplayName("The limit is per period: the same car can be rented again afterwards")
    void sameCarReusableInLaterPeriod() {
        Reservation first = service.reserve("a", CarType.SUV, NEXT_WEEK, 3);
        Reservation backToBack = service.reserve("b", CarType.SUV, NEXT_WEEK.plusDays(3), 3);

        assertEquals(first.getCar(), backToBack.getCar());
    }

    @Test
    @DisplayName("The limit is per type: a full type does not block other types")
    void typesAreIndependent() {
        service.reserve("a", CarType.SUV, NEXT_WEEK, 3);
        assertThrows(NoCarAvailableException.class,
                     () -> service.reserve("b", CarType.SUV, NEXT_WEEK, 3));

        assertEquals(CarType.SEDAN, service.reserve("b", CarType.SEDAN, NEXT_WEEK, 3).getCarType());
        assertEquals(CarType.VAN, service.reserve("b", CarType.VAN, NEXT_WEEK, 3).getCarType());
    }

    @Test
    @DisplayName("Type with zero cars can never be reserved")
    void zeroCapacityType() {
        CarRentService noVans = new CarRentService(Fleet.withCounts(1, 1, 0), CLOCK);
        assertThrows(NoCarAvailableException.class,
                     () -> noVans.reserve("a", CarType.VAN, NEXT_WEEK, 1));
    }

    @Test
    @DisplayName("AvailableCount reflects bookings in the period")
    void availableCount() {
        assertEquals(3, service.availableCount(CarType.VAN, NEXT_WEEK, 2));
        service.reserve("a", CarType.VAN, NEXT_WEEK, 2);
        assertEquals(2, service.availableCount(CarType.VAN, NEXT_WEEK, 2));
        assertEquals(3, service.availableCount(CarType.VAN, NEXT_WEEK.plusDays(2), 2));
    }

    @Test
    @DisplayName("Cancelling a reservation frees the car for that period")
    void cancelFreesCar() {
        Reservation r = service.reserve("a", CarType.SUV, NEXT_WEEK, 3);
        service.cancel(r.getId());

        assertTrue(service.findReservation(r.getId()).isEmpty());
        assertEquals(CarType.SUV, service.reserve("b", CarType.SUV, NEXT_WEEK, 3).getCarType());
    }

    @Test
    @DisplayName("Cancelling an unknown or already cancelled reservation fails")
    void cancelUnknown() {
        assertThrows(ReservationNotFoundException.class, () -> service.cancel(UUID.randomUUID()));
        Reservation r = service.reserve("a", CarType.SUV, NEXT_WEEK, 1);
        service.cancel(r.getId());
        assertThrows(ReservationNotFoundException.class, () -> service.cancel(r.getId()));
    }

    @Test
    @DisplayName("Reservations in the past are rejected; starting right now is fine")
    void pastRejected() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        assertThrows(IllegalArgumentException.class,
                     () -> service.reserve("a", CarType.SUV, now.minusMinutes(1), 1));
        assertEquals(now, service.reserve("a", CarType.SUV, now, 1).getPeriod().getStart());
    }

    @Test
    @DisplayName("Nnon-positive number of days and missing arguments are rejected")
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class,
                     () -> service.reserve("a", CarType.SUV, NEXT_WEEK, 0));
        assertThrows(NullPointerException.class, () -> service.reserve("a", null, NEXT_WEEK, 1));
        assertThrows(NullPointerException.class, () -> service.reserve("a", CarType.SUV, null, 1));
        assertThrows(NullPointerException.class,
                     () -> service.reserve(null, CarType.SUV, NEXT_WEEK, 1));
        assertTrue(service.reservations().isEmpty(), "failed attempts leave no trace");
    }

    @Test
    @DisplayName("Custom allocation strategy decides which free car is used")
    void customStrategy() {
        AllocationStrategy lastAvailable = (schedules, period) -> schedules.reversed().stream()
                .filter(s -> s.isAvailable(period)).findFirst();
        CarRentService svc = new CarRentService(Fleet.withCounts(0, 0, 3), CLOCK, lastAvailable);

        assertEquals("VAN-3", svc.reserve("a", CarType.VAN, NEXT_WEEK, 1).getCar().getVin());
    }

    @Test
    @DisplayName("Buggy strategy returning a busy car cannot cause a double booking")
    void buggyStrategyCannotDoubleBook() {
        AllocationStrategy alwaysFirst = (schedules, period) -> schedules.stream().findFirst();
        CarRentService svc = new CarRentService(Fleet.withCounts(2, 0, 0), CLOCK, alwaysFirst);

        svc.reserve("a", CarType.SEDAN, NEXT_WEEK, 2);
        assertThrows(NoCarAvailableException.class,
                     () -> svc.reserve("b", CarType.SEDAN, NEXT_WEEK, 2));
        Set<String> booked =
                svc.reservations().stream().map(r -> r.getCar().getVin()).collect(
                        Collectors.toSet());
        assertEquals(Set.of("SEDAN-1"), booked);
    }
}
