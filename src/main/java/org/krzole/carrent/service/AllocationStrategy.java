package org.krzole.carrent.service;

import org.krzole.carrent.model.CarSchedule;
import org.krzole.carrent.model.RentPeriod;

import java.util.List;
import java.util.Optional;
@FunctionalInterface
public interface AllocationStrategy {

    Optional<CarSchedule> choose(List<CarSchedule> schedulesOfType, RentPeriod period);

    /** Default policy: the first car (in fleet order) that is free for the whole period. */
    static AllocationStrategy firstAvailable() {
        return (schedules, period) -> schedules.stream()
                .filter(s -> s.isAvailable(period))
                .findFirst();
    }
}
