package org.krzole.carrent.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CarSchedule {

    private final Car car;
    private final List<Reservation> reservations = new ArrayList<>();

    public CarSchedule(Car car) {
        this.car = car;
    }

    public Car car() {
        return car;
    }

    public boolean isAvailable(RentPeriod period) {
        for (Reservation existing : reservations) {
            if (existing.period().overlaps(period)) {
                return false;
            }
        }
        return true;
    }

    public void add(Reservation reservation) {
        if (!reservation.car().equals(car)) {
            throw new IllegalArgumentException("Reservation is for a different car");
        }
        if (!isAvailable(reservation.period())) {
            throw new IllegalStateException("Car " + car.getVin() + " is already booked in " + reservation.period());
        }
        reservations.add(reservation);
    }

    public boolean remove(UUID reservationId) {
        return reservations.removeIf(r -> r.id().equals(reservationId));
    }

    public List<Reservation> reservations() {
        return Collections.unmodifiableList(new ArrayList<>(reservations));
    }
}
