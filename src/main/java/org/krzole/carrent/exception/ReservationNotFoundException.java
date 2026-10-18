package org.krzole.carrent.exception;

import java.io.Serial;
import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(UUID id) {
        super("Reservation not found: " + id);
    }
}
