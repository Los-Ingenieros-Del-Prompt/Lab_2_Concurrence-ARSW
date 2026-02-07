package edu.eci.arsw.dogsrace.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para RandomGenerator
 * Cobertura: generación de números aleatorios, rangos, distribución
 */
@DisplayName("RandomGenerator Unit Tests")
class RandomGeneratorTest {

    @Test
    @DisplayName("Debe generar números dentro del rango especificado")
    void testNumbersWithinBound() {
        int bound = 10;
        for (int i = 0; i < 100; i++) {
            int random = RandomGenerator.nextInt(bound);
            assertTrue(random >= 0 && random < bound,
                    "Número aleatorio " + random + " fuera del rango [0, " + bound + ")");
        }
    }

    @Test
    @DisplayName("Debe generar 0 para bound = 1")
    void testBoundOne() {
        for (int i = 0; i < 10; i++) {
            assertEquals(0, RandomGenerator.nextInt(1));
        }
    }

    @Test
    @DisplayName("Debe generar valores variados")
    void testVariety() {
        Set<Integer> generatedNumbers = new HashSet<>();
        int bound = 100;

        for (int i = 0; i < 1000; i++) {
            generatedNumbers.add(RandomGenerator.nextInt(bound));
        }

        assertTrue(generatedNumbers.size() >= 50,
                "Debería generar variedad de números, pero solo generó " + generatedNumbers.size());
    }

    @Test
    @DisplayName("Debe funcionar con diferentes bounds")
    void testDifferentBounds() {
        int[] bounds = {2, 5, 10, 50, 100, 1000};

        for (int bound : bounds) {
            int random = RandomGenerator.nextInt(bound);
            assertTrue(random >= 0 && random < bound);
        }
    }

    @RepeatedTest(10)
    @DisplayName("Debe generar números consistentemente en el rango")
    void testConsistentRange() {
        int bound = 20;
        int random = RandomGenerator.nextInt(bound);
        assertTrue(random >= 0 && random < bound);
    }

    @Test
    @DisplayName("Debe ser thread-safe")
    void testThreadSafety() throws InterruptedException {
        int numberOfThreads = 10;
        Thread[] threads = new Thread[numberOfThreads];
        Set<Integer>[] resultSets = new Set[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            resultSets[index] = new HashSet<>();
            threads[index] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    int random = RandomGenerator.nextInt(50);
                    resultSets[index].add(random);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verificar que cada hilo generó números válidos
        for (Set<Integer> results : resultSets) {
            assertFalse(results.isEmpty());
            for (int num : results) {
                assertTrue(num >= 0 && num < 50);
            }
        }
    }

    @Test
    @DisplayName("Debe generar distribución aproximadamente uniforme")
    void testUniformDistribution() {
        int bound = 10;
        int[] counts = new int[bound];
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            counts[RandomGenerator.nextInt(bound)]++;
        }

        double expected = (double) iterations / bound;
        double tolerance = expected * 0.3; // 30% de tolerancia

        for (int i = 0; i < bound; i++) {
            assertTrue(Math.abs(counts[i] - expected) < tolerance,
                    "Número " + i + " apareció " + counts[i] + " veces, esperado ~" + expected);
        }
    }

    @Test
    @DisplayName("No debe generar números negativos")
    void testNoNegativeNumbers() {
        for (int i = 0; i < 1000; i++) {
            int random = RandomGenerator.nextInt(100);
            assertTrue(random >= 0);
        }
    }

    @Test
    @DisplayName("No debe generar el bound mismo")
    void testDoesNotGenerateBound() {
        int bound = 10;
        for (int i = 0; i < 100; i++) {
            int random = RandomGenerator.nextInt(bound);
            assertNotEquals(bound, random);
        }
    }

    @Test
    @DisplayName("Debe manejar bound grande")
    void testLargeBound() {
        int bound = 1000000;
        int random = RandomGenerator.nextInt(bound);
        assertTrue(random >= 0 && random < bound);
    }

    @Test
    @DisplayName("Múltiples llamadas deben producir resultados diferentes")
    void testMultipleCallsProduceDifferentResults() {
        int bound = 1000;
        int firstCall = RandomGenerator.nextInt(bound);

        boolean foundDifferent = false;
        for (int i = 0; i < 100; i++) {
            if (RandomGenerator.nextInt(bound) != firstCall) {
                foundDifferent = true;
                break;
            }
        }

        assertTrue(foundDifferent, "100 llamadas deberían producir al menos un valor diferente");
    }
}