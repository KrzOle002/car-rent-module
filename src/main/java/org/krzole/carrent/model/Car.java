package org.krzole.carrent.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.krzole.carrent.model.enums.CarType;

public final class Car {

    /**
     * It's mock for JPA's {@code @GeneratedValue}:
     * Thread-safe, so two cars created at the same time never get the same id.
     * With a real database this counter would be removed and the id generated on save.
     */
    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private final long id;
    private final String vin;
    private final CarType type;

    public Car(String vin, CarType type) {
        Objects.requireNonNull(vin, "vin");
        Objects.requireNonNull(type, "type");
        if (vin.isBlank()) {
            throw new IllegalArgumentException("VIN must not be blank");
        }
        this.id = NEXT_ID.getAndIncrement();
        this.vin = vin;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public String getVin() {
        return vin;
    }

    public CarType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Car other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Car[id=" + id + ", vin=" + vin + ", type=" + type + "]";
    }
}
