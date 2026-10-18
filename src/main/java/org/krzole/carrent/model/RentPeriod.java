package org.krzole.carrent.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class RentPeriod {

    private final LocalDateTime start;
    private final int days;

    public RentPeriod(LocalDateTime start, int days) {
        Objects.requireNonNull(start, "start");
        if (days <= 0) {
            throw new IllegalArgumentException("Number of days must be positive, was " + days);
        }
        this.start = start;
        this.days = days;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public int getDays() {
        return days;
    }

    public LocalDateTime getEnd() {
        return start.plusDays(days);
    }

    /**
     * True when the two periods share at least one instant.
     */
    public boolean overlaps(RentPeriod other) {
        return start.isBefore(other.getEnd()) && other.start.isBefore(getEnd());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RentPeriod other)) return false;
        return days == other.days && start.equals(other.start);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, days);
    }

    @Override
    public String toString() {
        return "[" + start + " -> " + getEnd() + ")";
    }

}