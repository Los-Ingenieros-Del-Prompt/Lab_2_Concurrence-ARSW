package edu.eci.arsw.dogsrace.threads;

import edu.eci.arsw.dogsrace.control.RaceControl;
import edu.eci.arsw.dogsrace.domain.ArrivalRegistry;
import edu.eci.arsw.dogsrace.ui.Carril;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para Galgo
 * Cobertura: ejecución de carrera, interacción con controles, registro de llegada
 */
@DisplayName("Galgo Unit Tests")
class GalgoTest {

    private Carril mockCarril;
    private ArrivalRegistry mockRegistry;
    private RaceControl mockControl;
    private Galgo galgo;

    @BeforeEach
    void setUp() {
        mockCarril = mock(Carril.class);
        mockRegistry = mock(ArrivalRegistry.class);
        mockControl = mock(RaceControl.class);
    }

    @Test
    @DisplayName("Debe correr toda la pista y registrar llegada")
    void testCompleteRace() throws InterruptedException {
        when(mockCarril.size()).thenReturn(10);
        when(mockRegistry.registerArrival(anyString()))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(1, "TestDog"));

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(5000);

        verify(mockCarril, times(10)).setPasoOn(anyInt());

        verify(mockCarril, atLeastOnce()).displayPasos(anyInt());

        verify(mockCarril, times(1)).finish();

        verify(mockRegistry, times(1)).registerArrival("TestDog");
    }

    @Test
    @DisplayName("Debe respetar el control de pausa")
    void testRespectsPauseControl() throws InterruptedException {
        when(mockCarril.size()).thenReturn(5);
        when(mockRegistry.registerArrival(anyString()))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(1, "TestDog"));

        doNothing().when(mockControl).awaitIfPaused();

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(5000);

        verify(mockControl, atLeast(5)).awaitIfPaused();
    }

    @Test
    @DisplayName("Debe actualizar los pasos correctamente")
    void testStepProgression() throws InterruptedException {
        when(mockCarril.size()).thenReturn(5);
        when(mockRegistry.registerArrival(anyString()))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(1, "TestDog"));

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(5000);

        ArgumentCaptor<Integer> stepCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockCarril, times(5)).setPasoOn(stepCaptor.capture());

        assertEquals(5, stepCaptor.getAllValues().size());
        for (int i = 0; i < 5; i++) {
            assertTrue(stepCaptor.getAllValues().contains(i));
        }
    }

    @Test
    @DisplayName("Debe manejar interrupciones correctamente")
    void testHandlesInterruption() throws InterruptedException {
        when(mockCarril.size()).thenReturn(100); // Carril largo
        doThrow(new InterruptedException()).when(mockControl).awaitIfPaused();

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(2000);

        assertFalse(galgo.isAlive());

        verify(mockRegistry, never()).registerArrival(anyString());
    }

    @Test
    @DisplayName("Debe registrar la posición de llegada correcta")
    void testRegistersCorrectArrivalPosition() throws InterruptedException {
        when(mockCarril.size()).thenReturn(3);
        when(mockRegistry.registerArrival("Runner1"))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(2, "Winner"));

        galgo = new Galgo(mockCarril, "Runner1", mockRegistry, mockControl);
        galgo.start();
        galgo.join(2000);

        verify(mockRegistry, times(1)).registerArrival("Runner1");
    }

    @Test
    @DisplayName("Debe llamar a finish solo cuando llega al final")
    void testFinishCalledOnlyAtEnd() throws InterruptedException {
        when(mockCarril.size()).thenReturn(5);
        when(mockRegistry.registerArrival(anyString()))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(1, "TestDog"));

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(3000);

        verify(mockCarril, times(1)).finish();
    }

    @Test
    @DisplayName("Múltiples galgos deben correr independientemente")
    void testMultipleGalgosRunIndependently() throws InterruptedException {
        Carril carril1 = mock(Carril.class);
        Carril carril2 = mock(Carril.class);
        Carril carril3 = mock(Carril.class);

        when(carril1.size()).thenReturn(5);
        when(carril2.size()).thenReturn(5);
        when(carril3.size()).thenReturn(5);

        ArrivalRegistry registry = new ArrivalRegistry();
        RaceControl control = new RaceControl();

        Galgo galgo1 = new Galgo(carril1, "Dog1", registry, control);
        Galgo galgo2 = new Galgo(carril2, "Dog2", registry, control);
        Galgo galgo3 = new Galgo(carril3, "Dog3", registry, control);

        galgo1.start();
        galgo2.start();
        galgo3.start();

        galgo1.join(3000);
        galgo2.join(3000);
        galgo3.join(3000);

        assertFalse(galgo1.isAlive());
        assertFalse(galgo2.isAlive());
        assertFalse(galgo3.isAlive());

        verify(carril1, times(1)).finish();
        verify(carril2, times(1)).finish();
        verify(carril3, times(1)).finish();
    }

    @Test
    @DisplayName("Debe usar el nombre del hilo correctamente")
    void testThreadName() {
        galgo = new Galgo(mockCarril, "Champion", mockRegistry, mockControl);
        assertEquals("Champion", galgo.getName());
    }

    @Test
    @DisplayName("Debe esperar 100ms entre pasos")
    void testTimingBetweenSteps() throws InterruptedException {
        when(mockCarril.size()).thenReturn(3);
        when(mockRegistry.registerArrival(anyString()))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(1, "TestDog"));

        long startTime = System.currentTimeMillis();

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(5000);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 300, "La carrera debería tomar al menos 300ms");
    }

    @Test
    @DisplayName("Debe actualizar displayPasos con el progreso")
    void testDisplayPasosUpdates() throws InterruptedException {
        when(mockCarril.size()).thenReturn(3);
        when(mockRegistry.registerArrival(anyString()))
                .thenReturn(new ArrivalRegistry.ArrivalSnapshot(1, "TestDog"));

        galgo = new Galgo(mockCarril, "TestDog", mockRegistry, mockControl);
        galgo.start();
        galgo.join(2000);

        ArgumentCaptor<Integer> displayCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockCarril, atLeast(3)).displayPasos(displayCaptor.capture());

        assertTrue(displayCaptor.getAllValues().contains(1));
        assertTrue(displayCaptor.getAllValues().contains(2));
        assertTrue(displayCaptor.getAllValues().contains(3));
    }
}
