package edu.eci.arsw.dogsrace.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomGenerator {

    private RandomGenerator() { }

    public static int nextInt(int boundExclusive) {
        return ThreadLocalRandom.current().nextInt(boundExclusive);
    }
}
