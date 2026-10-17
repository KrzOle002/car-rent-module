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

  public LocalDateTime start() {
    return start;
  }

  public int days() {
    return days;
  }

  public LocalDateTime end() {
    return start.plusDays(days);
  }

  /** True when the two periods share at least one instant. */
  public boolean overlaps(RentPeriod other) {
    return start.isBefore(other.end()) && other.start.isBefore(end());
  }



}