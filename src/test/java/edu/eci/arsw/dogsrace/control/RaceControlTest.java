package edu.eci.arsw.dogsrace.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RaceControlTest {

    @Test
    void pauseAndResume_blocksAndReleasesThreads() throws Exception {
        RaceControl control = new RaceControl();
        AtomicInteger ticks = new AtomicInteger(0);

        Thread worker = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    control.awaitIfPaused();
                    ticks.incrementAndGet();
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        TimeUnit.MILLISECONDS.sleep(50);
        int beforePause = ticks.get();

        control.pause();
        TimeUnit.MILLISECONDS.sleep(80);
        int duringPause = ticks.get();
        assertEquals(beforePause, duringPause, "Ticks must not increase while paused");

        control.resume();
        TimeUnit.MILLISECONDS.sleep(50);
        int afterResume = ticks.get();
        assertTrue(afterResume > duringPause, "Ticks must increase after resume");

        worker.interrupt();
        worker.join(500);
        assertFalse(worker.isAlive());
    }
}
