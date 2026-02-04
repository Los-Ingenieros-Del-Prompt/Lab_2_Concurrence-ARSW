package edu.eci.arsw.dogsrace.domain;

import java.util.Objects;

/**
 * Thread-safe arrival registry.
 * Critical section is limited to the position assignment and winner selection.
 */
public final class ArrivalRegistry {

    private int nextPosition = 1;
    private String winner = null;

    public synchronized ArrivalSnapshot registerArrival(String dogName) {
        Objects.requireNonNull(dogName, "dogName");
        final int position = nextPosition++;
        if (position == 1) {
            winner = dogName;
        }
        return new ArrivalSnapshot(position, winner);
    }

    public synchronized int getNextPosition() {
        return nextPosition;
    }

    public synchronized String getWinner() {
        return winner;
    }

    public record ArrivalSnapshot(int position, String winner) { }
}
