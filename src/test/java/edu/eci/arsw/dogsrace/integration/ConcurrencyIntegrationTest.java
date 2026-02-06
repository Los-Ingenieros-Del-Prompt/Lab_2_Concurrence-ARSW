package edu.eci.arsw.dogsrace.integration;

import edu.eci.arsw.dogsrace.control.RaceControl;
import edu.eci.arsw.dogsrace.domain.ArrivalRegistry;
import edu.eci.arsw.dogsrace.threads.Galgo;
import edu.eci.arsw.dogsrace.ui.Carril;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración enfocadas en concurrencia y condiciones de carrera
 */
@DisplayName("Integration Tests - Concurrency & Thread Safety")
class ConcurrencyIntegrationTest {

    private ArrivalRegistry registry;
    private RaceControl control;

    @BeforeEach
    void setUp() {
        registry = new ArrivalRegistry();
        control = new RaceControl();
    }

    @Test
    @DisplayName("Alta concurrencia: 20 galgos compitiendo simultáneamente")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHighConcurrency() throws InterruptedException {
        int numberOfRunners = 20;
        int trackLength = 15;
        Galgo[] galgos = new Galgo[numberOfRunners];

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            galgos[i] = new Galgo(carril, "Dog" + i, registry, control);
        }

        for (Galgo galgo : galgos) {
            galgo.start();
        }

        for (Galgo galgo : galgos) {
            galgo.join();
        }

        assertNotNull(registry.getWinner());
        assertEquals(numberOfRunners + 1, registry.getNextPosition());

        for (Galgo galgo : galgos) {
            assertFalse(galgo.isAlive());
        }
    }

    @Test
    @DisplayName("Stress test: múltiples operaciones de pause/resume bajo carga")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testPauseResumeUnderLoad() throws InterruptedException {
        int numberOfRunners = 10;
        int trackLength = 30;
        Galgo[] galgos = new Galgo[numberOfRunners];
        AtomicInteger pauseResumeCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            galgos[i] = new Galgo(carril, "Dog" + i, registry, control);
            galgos[i].start();
        }

        Thread controlThread = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(50);
                    control.pause();
                    pauseResumeCount.incrementAndGet();
                    Thread.sleep(50);
                    control.resume();
                    pauseResumeCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controlThread.start();
        controlThread.join();

        for (Galgo galgo : galgos) {
            galgo.join();
        }

        assertNotNull(registry.getWinner());
        assertEquals(20, pauseResumeCount.get()); // 10 pausas + 10 reanudaciones
    }

    @Test
    @DisplayName("No debe haber condiciones de carrera en el registro de llegadas")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testNoRaceConditionInArrivalRegistry() throws InterruptedException {
        int numberOfRunners = 50;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfRunners);
        AtomicBoolean errorDetected = new AtomicBoolean(false);

        for (int i = 0; i < numberOfRunners; i++) {
            final int dogNum = i;
            new Thread(() -> {
                try {
                    startSignal.await(); // Todos esperan la señal
                    ArrivalRegistry.ArrivalSnapshot snapshot =
                            registry.registerArrival("Dog" + dogNum);

                    // Verificar que la posición es válida
                    if (snapshot.position() < 1 || snapshot.position() > numberOfRunners) {
                        errorDetected.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            }).start();
        }

        startSignal.countDown();
        assertTrue(doneSignal.await(10, TimeUnit.SECONDS));

        assertFalse(errorDetected.get());
        assertEquals(numberOfRunners + 1, registry.getNextPosition());
    }

    @Test
    @DisplayName("Control de carrera debe ser consistente con múltiples observadores")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testRaceControlConsistency() throws InterruptedException {
        int numberOfObservers = 10;
        int checksPerObserver = 1000;
        CountDownLatch allDone = new CountDownLatch(numberOfObservers);
        AtomicBoolean inconsistencyFound = new AtomicBoolean(false);

        for (int i = 0; i < numberOfObservers; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < checksPerObserver; j++) {
                        boolean state1 = control.isPaused();
                        boolean state2 = control.isPaused();
                        if (state1 != state2) {
                            inconsistencyFound.set(true);
                        }
                    }
                } finally {
                    allDone.countDown();
                }
            }).start();
        }

        Thread changer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                control.pause();
                control.resume();
            }
        });

        changer.start();
        changer.join();
        assertTrue(allDone.await(10, TimeUnit.SECONDS));

        assertFalse(inconsistencyFound.get());
    }

    @Test
    @DisplayName("Interrupción de galgos durante pausa debe manejarse correctamente")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInterruptionDuringPause() throws InterruptedException {
        int trackLength = 100;
        Carril carril = new Carril(trackLength, "TestDog");
        Galgo galgo = new Galgo(carril, "TestDog", registry, control);

        control.pause();
        galgo.start();

        Thread.sleep(200);

        galgo.interrupt();
        galgo.join(2000);

        assertFalse(galgo.isAlive());
    }

    @Test
    @DisplayName("Múltiples galgos con diferentes velocidades de ejecución")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMixedSpeedRunners() throws InterruptedException {
        Carril fastTrack = new Carril(5, "Fast");
        Carril mediumTrack = new Carril(10, "Medium");
        Carril slowTrack = new Carril(20, "Slow");
        Carril verySlowTrack = new Carril(30, "VerySlow");

        Galgo fast = new Galgo(fastTrack, "Fast", registry, control);
        Galgo medium = new Galgo(mediumTrack, "Medium", registry, control);
        Galgo slow = new Galgo(slowTrack, "Slow", registry, control);
        Galgo verySlow = new Galgo(verySlowTrack, "VerySlow", registry, control);

        slow.start();
        fast.start();
        verySlow.start();
        medium.start();

        fast.join();
        medium.join();
        slow.join();
        verySlow.join();

        assertEquals("Fast", registry.getWinner());
    }

    @Test
    @DisplayName("Sistema debe manejar pausas muy rápidas consecutivas")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testRapidPauseResumeCycles() throws InterruptedException {
        int numberOfRunners = 5;
        int trackLength = 20;
        Galgo[] galgos = new Galgo[numberOfRunners];

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            galgos[i] = new Galgo(carril, "Dog" + i, registry, control);
            galgos[i].start();
        }

        for (int i = 0; i < 50; i++) {
            control.pause();
            Thread.sleep(10);
            control.resume();
            Thread.sleep(10);
        }

        for (Galgo galgo : galgos) {
            galgo.join();
        }

        assertNotNull(registry.getWinner());
        assertEquals(numberOfRunners + 1, registry.getNextPosition());
    }

    @Test
    @DisplayName("Deadlock no debe ocurrir con múltiples controles")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testNoDeadlock() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch allCompleted = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            final int taskNum = i;
            executor.submit(() -> {
                try {
                    if (taskNum % 2 == 0) {
                        control.pause();
                        Thread.sleep(10);
                        control.resume();
                    } else {
                        control.awaitIfPaused();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allCompleted.countDown();
                }
            });
        }

        assertTrue(allCompleted.await(8, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verificar atomicidad en asignación de posiciones")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAtomicPositionAssignment() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);

        int[] positions = new int[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startSignal.await();
                    ArrivalRegistry.ArrivalSnapshot snapshot =
                            registry.registerArrival("Dog" + index);
                    positions[index] = snapshot.position();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        assertTrue(doneSignal.await(8, TimeUnit.SECONDS));

        boolean[] seen = new boolean[numberOfThreads + 1];
        for (int pos : positions) {
            assertFalse(seen[pos], "Posición duplicada: " + pos);
            seen[pos] = true;
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Sistema debe recuperarse de excepciones en hilos individuales")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSystemRecoveryFromThreadExceptions() throws InterruptedException {
        int numberOfRunners = 5;
        Galgo[] galgos = new Galgo[numberOfRunners];

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(10, "Dog" + i);
            galgos[i] = new Galgo(carril, "Dog" + i, registry, control);
            galgos[i].start();
        }

        galgos[1].interrupt();
        galgos[3].interrupt();

        for (Galgo galgo : galgos) {
            galgo.join(3000);
        }

        assertTrue(registry.getNextPosition() > 1);
    }
}