package org.krzole.carrent.exception;

import org.krzole.carrent.model.RentPeriod;
import org.krzole.carrent.model.enums.CarType;

import java.io.Serial;

/**
 * Thrown when every car of the requested type is booked during the requested period.
 */
public class NoCarAvailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final CarType carType;
    private final transient RentPeriod period;

    public NoCarAvailableException(CarType carType, RentPeriod period) {
        super("No " + carType + " available from " + period.getStart() + " to " + period.getEnd());
        this.carType = carType;
        this.period = period;
    }

    public CarType carType() {
        return carType;
    }

    public RentPeriod period() {
        return period;
    }
}
