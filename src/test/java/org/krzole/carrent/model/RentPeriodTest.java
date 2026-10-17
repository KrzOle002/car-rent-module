package org.krzole.carrent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentPeriodTest {

  private static final LocalDateTime mockDate = LocalDateTime.of(2026, 12, 31, 10, 0);

  private static RentPeriod period(LocalDateTime start, int days) {
    return new RentPeriod(start, days);
  }

  @Test
  @DisplayName("End is start plus the number of days, keeping the time of day")
  void endIsStartPlusDays() {
    assertEquals(LocalDateTime.of(2027, 1, 3, 10, 0), period(mockDate, 3).end());
  }

  @Test
  @DisplayName("Zero or negative days are rejected")
  void rejectsNonPositiveDays() {
    assertThrows(IllegalArgumentException.class, () -> period(mockDate, 0));
    assertThrows(IllegalArgumentException.class, () -> period(mockDate, -1));
  }

  @Test
  @DisplayName("Null start is rejected")
  void rejectsNullStart() {
    assertThrows(NullPointerException.class, () -> period(null, 1));
  }

  @Test
  @DisplayName("Back-to-back periods do not overlap (half-open interval)")
  void backToBackDoesNotOverlap() {
    RentPeriod first = period(mockDate, 2);
    RentPeriod second = period(mockDate.plusDays(2), 2);
    assertFalse(first.overlaps(second));
    assertFalse(second.overlaps(first));
  }

  @Test
  @DisplayName("Periods sharing even one hour overlap, symmetrically")
  void partialOverlap() {
    RentPeriod first = period(mockDate, 2);
    RentPeriod second = period(mockDate.plusDays(2).minusHours(1), 2);
    assertTrue(first.overlaps(second));
    assertTrue(second.overlaps(first));
  }

  @Test
  @DisplayName("Period fully inside another overlaps it")
  void containedOverlaps() {
    RentPeriod outer = period(mockDate, 7);
    RentPeriod inner = period(mockDate.plusDays(2), 1);
    assertTrue(outer.overlaps(inner));
    assertTrue(inner.overlaps(outer));
  }

  @Test
  @DisplayName("Identical periods overlap")
  void identicalOverlaps() {
    assertTrue(period(mockDate, 1).overlaps(period(mockDate, 1)));
  }
}