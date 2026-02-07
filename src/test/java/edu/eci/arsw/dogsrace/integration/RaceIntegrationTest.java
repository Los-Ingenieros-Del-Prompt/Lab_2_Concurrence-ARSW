package edu.eci.arsw.dogsrace.integration;

import edu.eci.arsw.dogsrace.control.RaceControl;
import edu.eci.arsw.dogsrace.domain.ArrivalRegistry;
import edu.eci.arsw.dogsrace.threads.Galgo;
import edu.eci.arsw.dogsrace.ui.Carril;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para el sistema completo de carreras
 * Cobertura: flujo completo de carreras, sincronización, competencia entre galgos
 */
@DisplayName("Integration Tests - Full Race System")
class RaceIntegrationTest {

    private ArrivalRegistry registry;
    private RaceControl control;
    private List<Carril> carriles;
    private List<Galgo> galgos;

    @BeforeEach
    void setUp() {
        registry = new ArrivalRegistry();
        control = new RaceControl();
        carriles = new ArrayList<>();
        galgos = new ArrayList<>();
    }

    @Test
    @DisplayName("Carrera completa con 5 galgos debe completarse correctamente")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCompleteRaceWithFiveRunners() throws InterruptedException {
        int numberOfRunners = 5;
        int trackLength = 10;

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            carriles.add(carril);
            Galgo galgo = new Galgo(carril, "Dog" + i, registry, control);
            galgos.add(galgo);
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
    @DisplayName("Pausar y reanudar durante la carrera debe funcionar correctamente")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testPauseAndResumeDuringRace() throws InterruptedException {
        int trackLength = 20;
        Carril carril1 = new Carril(trackLength, "Runner1");
        Carril carril2 = new Carril(trackLength, "Runner2");

        Galgo galgo1 = new Galgo(carril1, "Runner1", registry, control);
        Galgo galgo2 = new Galgo(carril2, "Runner2", registry, control);

        galgo1.start();
        galgo2.start();

        Thread.sleep(200);

        control.pause();
        assertTrue(control.isPaused());
        Thread.sleep(500);

        control.resume();
        assertFalse(control.isPaused());

        galgo1.join();
        galgo2.join();

        assertNotNull(registry.getWinner());
        assertEquals(3, registry.getNextPosition()); // 2 corredores + 1
    }

    @Test
    @DisplayName("Múltiples pausas y reanudaciones durante la carrera")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testMultiplePauseResumeCycles() throws InterruptedException {
        int trackLength = 30;
        Carril carril = new Carril(trackLength, "TestRunner");
        Galgo galgo = new Galgo(carril, "TestRunner", registry, control);

        galgo.start();

        for (int i = 0; i < 5; i++) {
            Thread.sleep(100);
            control.pause();
            Thread.sleep(100);
            control.resume();
        }

        galgo.join();

        assertNotNull(registry.getWinner());
        assertEquals("TestRunner", registry.getWinner());
    }

    @Test
    @DisplayName("Carrera con diferentes longitudes de pista")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testRaceWithDifferentTrackLengths() throws InterruptedException {
        Carril shortTrack = new Carril(5, "Fast");
        Carril mediumTrack = new Carril(10, "Medium");
        Carril longTrack = new Carril(20, "Slow");

        Galgo fastGalgo = new Galgo(shortTrack, "Fast", registry, control);
        Galgo mediumGalgo = new Galgo(mediumTrack, "Medium", registry, control);
        Galgo slowGalgo = new Galgo(longTrack, "Slow", registry, control);

        fastGalgo.start();
        mediumGalgo.start();
        slowGalgo.start();

        fastGalgo.join();
        mediumGalgo.join();
        slowGalgo.join();

        assertEquals("Fast", registry.getWinner());
        assertEquals(4, registry.getNextPosition()); // 3 corredores + 1
    }

    @Test
    @DisplayName("Todos los galgos deben registrar posiciones únicas")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testUniquePositions() throws InterruptedException {
        int numberOfRunners = 10;
        int trackLength = 15;

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            Galgo galgo = new Galgo(carril, "Dog" + i, registry, control);
            galgos.add(galgo);
        }

        for (Galgo galgo : galgos) {
            galgo.start();
        }

        for (Galgo galgo : galgos) {
            galgo.join();
        }

        assertEquals(numberOfRunners + 1, registry.getNextPosition());
    }

    @Test
    @DisplayName("El primer galgo en terminar debe ser el ganador")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testFirstFinisherIsWinner() throws InterruptedException {
        Carril track1 = new Carril(3, "First");
        Carril track2 = new Carril(10, "Second");
        Carril track3 = new Carril(15, "Third");

        Galgo galgo1 = new Galgo(track1, "First", registry, control);
        Galgo galgo2 = new Galgo(track2, "Second", registry, control);
        Galgo galgo3 = new Galgo(track3, "Third", registry, control);

        galgo3.start();
        galgo2.start();
        galgo1.start();

        galgo1.join();
        galgo2.join();
        galgo3.join();

        assertEquals("First", registry.getWinner());
    }

    @Test
    @DisplayName("Sistema debe manejar inicio simultáneo de múltiples galgos")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSimultaneousStart() throws InterruptedException {
        int numberOfRunners = 8;
        int trackLength = 10;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch allStarted = new CountDownLatch(numberOfRunners);

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            final int dogNum = i;
            Thread runnerThread = new Thread(() -> {
                try {
                    allStarted.countDown();
                    startSignal.await(); // Esperar señal de inicio
                    Galgo galgo = new Galgo(carril, "Dog" + dogNum, registry, control);
                    galgo.run(); // Ejecutar directamente para control preciso
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            runnerThread.start();
            galgos.add(new Galgo(carril, "Dog" + i, registry, control)); // Para cleanup
        }

        allStarted.await();

        startSignal.countDown();

        Thread.sleep(3000);

        assertTrue(registry.getNextPosition() > 1);
    }

    @Test
    @DisplayName("Control de carrera debe sincronizar correctamente múltiples hilos")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testRaceControlSynchronization() throws InterruptedException {
        int numberOfRunners = 6;
        int trackLength = 20;
        AtomicInteger pauseCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            Galgo galgo = new Galgo(carril, "Dog" + i, registry, control);
            galgos.add(galgo);
            galgo.start();
        }

        Thread controlThread = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(150);
                    control.pause();
                    pauseCount.incrementAndGet();
                    Thread.sleep(100);
                    control.resume();
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

        assertEquals(3, pauseCount.get());

        assertNotNull(registry.getWinner());
    }

    @Test
    @DisplayName("Carrera con un solo galgo debe funcionar correctamente")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSingleRunnerRace() throws InterruptedException {
        Carril carril = new Carril(10, "Solo");
        Galgo galgo = new Galgo(carril, "Solo", registry, control);

        galgo.start();
        galgo.join();

        assertEquals("Solo", registry.getWinner());
        assertEquals(2, registry.getNextPosition());
    }

    @Test
    @DisplayName("Sistema debe manejar carreras muy cortas")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testVeryShortRace() throws InterruptedException {
        int numberOfRunners = 3;
        int trackLength = 1;

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            Galgo galgo = new Galgo(carril, "Dog" + i, registry, control);
            galgos.add(galgo);
            galgo.start();
        }

        for (Galgo galgo : galgos) {
            galgo.join();
        }

        assertNotNull(registry.getWinner());
        assertEquals(numberOfRunners + 1, registry.getNextPosition());
    }

    @Test
    @DisplayName("Sistema debe manejar carreras muy largas")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testVeryLongRace() throws InterruptedException {
        int numberOfRunners = 3;
        int trackLength = 50;

        for (int i = 0; i < numberOfRunners; i++) {
            Carril carril = new Carril(trackLength, "Dog" + i);
            Galgo galgo = new Galgo(carril, "Dog" + i, registry, control);
            galgos.add(galgo);
            galgo.start();
        }

        for (Galgo galgo : galgos) {
            galgo.join();
        }

        assertNotNull(registry.getWinner());
        assertEquals(numberOfRunners + 1, registry.getNextPosition());
    }

    @Test
    @DisplayName("Pausar antes de iniciar no debe afectar el inicio")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPauseBeforeStart() throws InterruptedException {
        control.pause();

        Carril carril = new Carril(10, "TestDog");
        Galgo galgo = new Galgo(carril, "TestDog", registry, control);

        galgo.start();
        Thread.sleep(200);

        assertTrue(galgo.isAlive());

        control.resume();
        galgo.join();

        assertEquals("TestDog", registry.getWinner());
    }

    @Test
    @DisplayName("Múltiples carreras consecutivas con reinicio de registry")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testConsecutiveRaces() throws InterruptedException {
        Carril carril1 = new Carril(5, "Race1");
        Galgo galgo1 = new Galgo(carril1, "Race1", registry, control);
        galgo1.start();
        galgo1.join();

        String winner1 = registry.getWinner();
        assertNotNull(winner1);

        ArrivalRegistry registry2 = new ArrivalRegistry();
        Carril carril2 = new Carril(5, "Race2");
        Galgo galgo2 = new Galgo(carril2, "Race2", registry2, control);
        galgo2.start();
        galgo2.join();

        String winner2 = registry2.getWinner();
        assertNotNull(winner2);

        assertNotEquals(winner1, winner2);
    }
}
