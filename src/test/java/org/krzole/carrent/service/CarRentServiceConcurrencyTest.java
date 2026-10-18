package org.krzole.carrent.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.krzole.carrent.exception.NoCarAvailableException;
import org.krzole.carrent.model.Fleet;
import org.krzole.carrent.model.Reservation;
import org.krzole.carrent.model.enums.CarType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarRentServiceConcurrencyTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"),
                                                   ZoneOffset.UTC);
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final int THREADS = 32;
    private static final int ROUNDS = 50;

    @Test
    @DisplayName("Concurrent requests for the same type never exceed its capacity")
    void neverOverbooksUnderContention() throws Exception {
        int suvs = 3;
        for (int round = 0; round < ROUNDS; round++) {
            CarRentService service = new CarRentService(Fleet.withCounts(0, suvs, 0), CLOCK);

            List<Object> results = race(THREADS,
                                        i -> service.reserve("c" + i, CarType.SUV, START, 2));

            long successes = results.stream().filter(Reservation.class::isInstance).count();
            assertEquals(suvs, successes, "round " + round);
            results.stream().filter(r -> !(r instanceof Reservation))
                    .forEach(r -> assertInstanceOf(NoCarAvailableException.class, r));

            long distinctCars = results.stream().filter(Reservation.class::isInstance)
                    .map(r -> ((Reservation) r).getCar()).distinct().count();
            assertEquals(suvs, distinctCars, "each winner must get a different car");
        }
    }

    @Test
    @DisplayName("Concurrent requests for different types do not interfere")
    void differentTypesInParallel() throws Exception {
        CarRentService service = new CarRentService(Fleet.withCounts(10, 10, 10), CLOCK);
        CarType[] types = CarType.values();

        List<Object> results = race(30, i -> service.reserve("c" + i, types[i % 3], START, 1));

        assertTrue(results.stream().allMatch(Reservation.class::isInstance),
                   "all 30 fit in 30 cars");
        for (CarType type : types) {
            assertEquals(0, service.availableCount(type, START, 1));
        }
    }

    @FunctionalInterface
    private interface IndexedTask {
        Object run(int index);
    }

    private static List<Object> race(int n,
                                     IndexedTask task) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch ready = new CountDownLatch(n);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int index = i;
                Callable<Object> call = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        return task.run(index);
                    } catch (RuntimeException e) {
                        return e;
                    }
                };
                futures.add(pool.submit(call));
            }
            ready.await();
            go.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> f : futures) {
                results.add(f.get(10, TimeUnit.SECONDS));
            }
            return results;
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("Deadlock or starvation: tasks did not finish", e);
        } finally {
            pool.shutdownNow();
        }
    }
}
