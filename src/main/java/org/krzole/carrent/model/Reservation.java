package org.krzole.carrent.model;

import org.krzole.carrent.model.enums.CarType;

import java.util.Objects;
import java.util.UUID;

public final class Reservation {

    private final UUID id;
    private final String customerId;
    private final Car car;
    private final RentPeriod period;

    public Reservation(UUID id, String customerId, Car car, RentPeriod period) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.car = Objects.requireNonNull(car, "car");
        this.period = Objects.requireNonNull(period, "period");
    }

    public UUID id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public Car car() {
        return car;
    }

    public RentPeriod period() {
        return period;
    }

    public CarType carType() {
        return car.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Reservation[" + id + ", " + customerId +
                ", " + car.getVin() + ", " + period + "]";
    }
}
