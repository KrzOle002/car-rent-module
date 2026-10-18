package org.krzole.carrent.service;

import org.krzole.carrent.exception.NoCarAvailableException;
import org.krzole.carrent.exception.ReservationNotFoundException;
import org.krzole.carrent.model.*;
import org.krzole.carrent.model.enums.CarType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class CarRentService {

    private final Fleet fleet;
    private final Clock clock;
    private final AllocationStrategy allocationStrategy;

    private final Map<CarType, List<CarSchedule>> schedulesByType = new EnumMap<>(CarType.class);
    private final Map<CarType, ReentrantLock> locksByType = new EnumMap<>(CarType.class);
    private final Map<UUID, Reservation> reservationsById = new ConcurrentHashMap<>();

    public CarRentService(Fleet fleet, Clock clock) {
        this(fleet, clock, AllocationStrategy.firstAvailable());
    }

    public CarRentService(Fleet fleet, Clock clock, AllocationStrategy allocationStrategy) {
        this.fleet = Objects.requireNonNull(fleet, "fleet");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.allocationStrategy = Objects.requireNonNull(allocationStrategy, "allocationStrategy");
        for (CarType type : CarType.values()) {
            List<CarSchedule> schedules = new ArrayList<>();
            for (Car car : fleet.carsOf(type)) {
                schedules.add(new CarSchedule(car));
            }
            schedulesByType.put(type, List.copyOf(schedules));
            locksByType.put(type, new ReentrantLock());
        }
    }

    public Reservation reserve(String customerId, CarType type, LocalDateTime start, int days) {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(type, "type");
        RentPeriod period = new RentPeriod(start, days);
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Cannot reserve in the past: " + start);
        }

        ReentrantLock lock = locksByType.get(type);
        lock.lock();
        try {
            CarSchedule schedule = allocationStrategy
                    .choose(schedulesByType.get(type), period)
                    .filter(s -> s.isAvailable(period)) // never trust a pluggable strategy blindly
                    .orElseThrow(() -> new NoCarAvailableException(type, period));

            Reservation reservation = new Reservation(UUID.randomUUID(), customerId,
                                                      schedule.getCar(), period);
            schedule.add(reservation);
            reservationsById.put(reservation.getId(), reservation);
            return reservation;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancels a reservation, making its car available again for that period.
     */
    public void cancel(UUID reservationId) {
        Reservation reservation = reservationsById.get(reservationId);
        if (reservation == null) {
            throw new ReservationNotFoundException(reservationId);
        }
        ReentrantLock lock = locksByType.get(reservation.getCarType());
        lock.lock();
        try {
            if (reservationsById.remove(reservationId) == null) {
                throw new ReservationNotFoundException(reservationId); // cancelled concurrently
            }
            scheduleOf(reservation.getCar()).remove(reservationId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * How many cars are free for the whole period. Informational, not a promise.
     */
    public int availableCount(CarType type, LocalDateTime start, int days) {
        RentPeriod period = new RentPeriod(start, days);
        ReentrantLock lock = locksByType.get(type);
        lock.lock();
        try {
            return (int) schedulesByType.get(type).stream().filter(
                    s -> s.isAvailable(period)).count();
        } finally {
            lock.unlock();
        }
    }

    public Optional<Reservation> findReservation(UUID id) {
        return Optional.ofNullable(reservationsById.get(id));
    }

    public List<Reservation> reservations() {
        return Collections.unmodifiableList(new ArrayList<>(reservationsById.values()));
    }

    public int capacityOf(CarType type) {
        return fleet.capacityOf(type);
    }

    private CarSchedule scheduleOf(Car car) {
        return schedulesByType.get(car.getType()).stream()
                .filter(s -> s.getCar().equals(car))
                .findFirst()
                .orElseThrow();
    }
}
