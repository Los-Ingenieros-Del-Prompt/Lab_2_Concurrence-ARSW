package edu.eci.arsw.dogsrace.control;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para RaceControl - VERSION CORREGIDA
 */
@DisplayName("RaceControl Unit Tests")
class RaceControlTest {

    private RaceControl control;

    @BeforeEach
    void setUp() {
        control = new RaceControl();
    }

    @Test
    @DisplayName("Estado inicial debe ser no pausado")
    void testInitialStateNotPaused() {
        assertFalse(control.isPaused());
    }

    @Test
    @DisplayName("Debe pausar la carrera")
    void testPause() {
        control.pause();
        assertTrue(control.isPaused());
    }

    @Test
    @DisplayName("Debe reanudar la carrera")
    void testResume() {
        control.pause();
        control.resume();
        assertFalse(control.isPaused());
    }

    @Test
    @DisplayName("Debe permitir múltiples pause/resume")
    void testMultiplePauseResume() {
        control.pause();
        assertTrue(control.isPaused());

        control.resume();
        assertFalse(control.isPaused());

        control.pause();
        assertTrue(control.isPaused());

        control.resume();
        assertFalse(control.isPaused());
    }

    @Test
    @DisplayName("awaitIfPaused no debe bloquear si no está pausado")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testAwaitIfPausedDoesNotBlockWhenNotPaused() throws InterruptedException {
        control.awaitIfPaused();
        assertTrue(true);
    }

    @Test
    @DisplayName("awaitIfPaused debe bloquear cuando está pausado")
    void testAwaitIfPausedBlocksWhenPaused() throws InterruptedException {
        control.pause();

        AtomicBoolean threadWaited = new AtomicBoolean(false);
        AtomicBoolean threadContinued = new AtomicBoolean(false);

        Thread testThread = new Thread(() -> {
            try {
                threadWaited.set(true);
                control.awaitIfPaused();
                threadContinued.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        testThread.start();
        Thread.sleep(100);

        assertTrue(threadWaited.get());
        assertFalse(threadContinued.get());

        control.resume();
        testThread.join(1000);

        assertTrue(threadContinued.get());
    }

    @Test
    @DisplayName("Resume debe despertar todos los hilos en espera")
    void testResumeWakesAllThreads() throws InterruptedException {
        control.pause();

        int numberOfThreads = 5;
        CountDownLatch allThreadsWaiting = new CountDownLatch(numberOfThreads);
        CountDownLatch allThreadsContinued = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            new Thread(() -> {
                try {
                    allThreadsWaiting.countDown();
                    control.awaitIfPaused();
                    allThreadsContinued.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        assertTrue(allThreadsWaiting.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);

        assertEquals(numberOfThreads, allThreadsContinued.getCount());

        control.resume();

        assertTrue(allThreadsContinued.await(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Debe manejar interrupción en awaitIfPaused")
    void testAwaitIfPausedHandlesInterruption() throws InterruptedException {
        control.pause();

        AtomicBoolean interrupted = new AtomicBoolean(false);

        Thread testThread = new Thread(() -> {
            try {
                control.awaitIfPaused();
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        });

        testThread.start();
        Thread.sleep(100);
        testThread.interrupt();
        testThread.join(1000);

        assertTrue(interrupted.get());
    }

    @Test
    @DisplayName("Múltiples pausas consecutivas deben mantener el estado")
    void testMultiplePausesKeepState() {
        control.pause();
        control.pause();
        control.pause();

        assertTrue(control.isPaused());
    }

    @Test
    @DisplayName("Múltiples resumes consecutivos deben mantener el estado")
    void testMultipleResumesKeepState() {
        control.pause();
        control.resume();
        control.resume();
        control.resume();

        assertFalse(control.isPaused());
    }

    @Test
    @DisplayName("awaitIfPaused debe ser thread-safe")
    void testAwaitIfPausedThreadSafety() throws InterruptedException {
        int iterations = 100;
        AtomicInteger completedIterations = new AtomicInteger(0);

        Thread worker = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                try {
                    control.awaitIfPaused();
                    completedIterations.incrementAndGet();
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        worker.start();

        for (int i = 0; i < 10; i++) {
            Thread.sleep(10);
            control.pause();
            Thread.sleep(5);
            control.resume();
        }

        worker.join(5000);
        assertEquals(iterations, completedIterations.get());
    }

    @Test
    @DisplayName("Estado de pausa debe ser thread-safe para lecturas")
    void testPauseStateConsistency() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger readCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        // Simplemente verificamos que podemos leer el estado
                        control.isPaused();
                        readCount.incrementAndGet();
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        for (int i = 0; i < 50; i++) {
            control.pause();
            Thread.sleep(5);
            control.resume();
            Thread.sleep(5);
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(1000, readCount.get());
    }
}