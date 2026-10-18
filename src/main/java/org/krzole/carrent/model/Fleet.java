package org.krzole.carrent.model;

import org.krzole.carrent.model.enums.CarType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Fleet {

    private final Map<CarType, List<Car>> carsByType;

    private Fleet(Map<CarType, List<Car>> carsByType) {
        this.carsByType = carsByType;
    }

    public static Fleet of(List<Car> cars) {
        Set<String> seenVins = new HashSet<>();
        Map<CarType, List<Car>> byType = new EnumMap<>(CarType.class);
        for (CarType type : CarType.values()) {
            byType.put(type, new ArrayList<>());
        }
        for (Car car : cars) {
            if (!seenVins.add(car.getVin())) {
                throw new IllegalArgumentException(
                        "Duplicate VIN: " + car.getVin());
            }
            byType.get(car.getType()).add(car);
        }
        byType.replaceAll((type, list) -> List.copyOf(list));
        return new Fleet(Collections.unmodifiableMap(byType));
    }

    public static Fleet withCounts(int sedans, int suvs, int vans) {
        List<Car> cars = new ArrayList<>();
        addCars(cars, CarType.SEDAN, sedans);
        addCars(cars, CarType.SUV, suvs);
        addCars(cars, CarType.VAN, vans);
        return of(cars);
    }

    private static void addCars(List<Car> target, CarType type, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must not be negative");
        }
        for (int i = 1; i <= count; i++) {
            target.add(new Car(type.name() + "-" + i, type));
        }
    }

    public List<Car> carsOf(CarType type) {
        return carsByType.get(type);
    }

    public int capacityOf(CarType type) {
        return carsByType.get(type).size();
    }
}
