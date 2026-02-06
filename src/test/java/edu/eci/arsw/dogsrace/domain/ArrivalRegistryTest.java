package edu.eci.arsw.dogsrace.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para ArrivalRegistry
 * Cobertura: sincronización, registro de llegadas, thread-safety
 */
@DisplayName("ArrivalRegistry Unit Tests")
class ArrivalRegistryTest {

    private ArrivalRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ArrivalRegistry();
    }

    @Test
    @DisplayName("Debe registrar el primer llegado como ganador")
    void testFirstArrivalIsWinner() {
        ArrivalRegistry.ArrivalSnapshot snapshot = registry.registerArrival("Galgo1");

        assertEquals(1, snapshot.position());
        assertEquals("Galgo1", snapshot.winner());
        assertEquals("Galgo1", registry.getWinner());
    }

    @Test
    @DisplayName("Debe asignar posiciones consecutivas")
    void testConsecutivePositions() {
        ArrivalRegistry.ArrivalSnapshot first = registry.registerArrival("Galgo1");
        ArrivalRegistry.ArrivalSnapshot second = registry.registerArrival("Galgo2");
        ArrivalRegistry.ArrivalSnapshot third = registry.registerArrival("Galgo3");

        assertEquals(1, first.position());
        assertEquals(2, second.position());
        assertEquals(3, third.position());

        assertEquals("Galgo1", first.winner());
        assertEquals("Galgo1", second.winner());
        assertEquals("Galgo1", third.winner());
    }

    @Test
    @DisplayName("Debe mantener el ganador invariante")
    void testWinnerRemainsSame() {
        registry.registerArrival("Winner");
        registry.registerArrival("Second");
        registry.registerArrival("Third");

        assertEquals("Winner", registry.getWinner());
    }

    @Test
    @DisplayName("Debe retornar la siguiente posición correctamente")
    void testGetNextPosition() {
        assertEquals(1, registry.getNextPosition());

        registry.registerArrival("Dog1");
        assertEquals(2, registry.getNextPosition());

        registry.registerArrival("Dog2");
        assertEquals(3, registry.getNextPosition());
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException si el nombre es null")
    void testNullDogNameThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            registry.registerArrival(null);
        });
    }

    @Test
    @DisplayName("Debe ser thread-safe con múltiples hilos")
    void testThreadSafety() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger maxPosition = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int dogNumber = i;
            executor.submit(() -> {
                try {
                    ArrivalRegistry.ArrivalSnapshot snapshot = registry.registerArrival("Dog" + dogNumber);
                    maxPosition.updateAndGet(current -> Math.max(current, snapshot.position()));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(numberOfThreads, maxPosition.get());
        assertEquals(numberOfThreads + 1, registry.getNextPosition());
        assertNotNull(registry.getWinner());
    }

    @Test
    @DisplayName("Debe manejar carrera con un solo participante")
    void testSingleParticipant() {
        ArrivalRegistry.ArrivalSnapshot snapshot = registry.registerArrival("OnlyOne");

        assertEquals(1, snapshot.position());
        assertEquals("OnlyOne", snapshot.winner());
        assertEquals(2, registry.getNextPosition());
    }

    @Test
    @DisplayName("Debe preservar el orden de llegada bajo concurrencia")
    void testArrivalOrderPreservation() throws InterruptedException {
        int participants = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(participants);

        for (int i = 0; i < participants; i++) {
            final int dogNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Todos empiezan al mismo tiempo
                    registry.registerArrival("Dog" + dogNum);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(participants + 1, registry.getNextPosition());
    }

    @Test
    @DisplayName("ArrivalSnapshot debe ser un record inmutable")
    void testArrivalSnapshotImmutability() {
        ArrivalRegistry.ArrivalSnapshot snapshot = new ArrivalRegistry.ArrivalSnapshot(1, "Winner");

        assertEquals(1, snapshot.position());
        assertEquals("Winner", snapshot.winner());

        assertNotNull(snapshot.toString());
        assertEquals(snapshot, new ArrivalRegistry.ArrivalSnapshot(1, "Winner"));
    }
}