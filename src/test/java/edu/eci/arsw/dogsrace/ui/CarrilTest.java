package edu.eci.arsw.dogsrace.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.swing.JButton;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para Carril (UI Component)
 * Cobertura: inicialización, estado de pasos, actualización de UI
 */
@DisplayName("Carril Unit Tests")
class CarrilTest {

    private Carril carril;

    @BeforeEach
    void setUp() {
        carril = new Carril(10, "Carril1");
    }

    @Test
    @DisplayName("Debe crear carril con el tamaño correcto")
    void testCarrilSize() {
        assertEquals(10, carril.size());
    }

    @Test
    @DisplayName("Debe retornar el nombre correcto")
    void testGetName() {
        assertEquals("Carril1", carril.getName());
    }

    @Test
    @DisplayName("Debe inicializar todos los pasos")
    void testPasosInitialization() {
        for (int i = 0; i < carril.size(); i++) {
            JButton paso = carril.getPaso(i);
            assertNotNull(paso);
            assertEquals("", paso.getText());
        }
    }

    @Test
    @DisplayName("Debe inicializar la bandera de llegada")
    void testLlegadaInitialization() {
        JButton llegada = carril.getLlegada();
        assertNotNull(llegada);
        assertEquals("Carril1", llegada.getText());
    }

    @Test
    @DisplayName("Debe marcar un paso como usado")
    void testSetPasoOn() {
        carril.setPasoOn(5);
        assertEquals("o", carril.getPaso(5).getText());
    }

    @Test
    @DisplayName("Debe desmarcar un paso")
    void testSetPasoOff() {
        carril.setPasoOn(3);
        assertEquals("o", carril.getPaso(3).getText());

        carril.setPasoOff(3);
        assertEquals("", carril.getPaso(3).getText());
    }

    @Test
    @DisplayName("Debe marcar la llegada al final")
    void testFinish() {
        carril.finish();
        assertEquals("!", carril.getLlegada().getText());
    }

    @Test
    @DisplayName("Debe mostrar el número de pasos")
    void testDisplayPasos() {
        carril.displayPasos(5);
        assertEquals("5", carril.getLlegada().getText());

        carril.displayPasos(10);
        assertEquals("10", carril.getLlegada().getText());
    }

    @Test
    @DisplayName("Debe reiniciar el carril correctamente")
    void testReStart() {
        carril.setPasoOn(0);
        carril.setPasoOn(1);
        carril.setPasoOn(2);
        carril.finish();

        carril.reStart();

        for (int i = 0; i < carril.size(); i++) {
            assertEquals("", carril.getPaso(i).getText());
        }
        assertEquals("Carril1", carril.getLlegada().getText());
    }

    @Test
    @DisplayName("Debe manejar múltiples actualizaciones de pasos")
    void testMultiplePasoUpdates() {
        carril.setPasoOn(0);
        carril.setPasoOn(1);
        carril.setPasoOn(2);

        assertEquals("o", carril.getPaso(0).getText());
        assertEquals("o", carril.getPaso(1).getText());
        assertEquals("o", carril.getPaso(2).getText());
        assertEquals("", carril.getPaso(3).getText());
    }

    @Test
    @DisplayName("Debe retornar paso específico correctamente")
    void testGetPasoAtIndex() {
        JButton paso0 = carril.getPaso(0);
        JButton paso5 = carril.getPaso(5);
        JButton paso9 = carril.getPaso(9);

        assertNotNull(paso0);
        assertNotNull(paso5);
        assertNotNull(paso9);

        assertNotSame(paso0, paso5);
        assertNotSame(paso5, paso9);
    }

    @Test
    @DisplayName("Debe crear carriles de diferentes tamaños")
    void testDifferentSizes() {
        Carril smallCarril = new Carril(5, "Small");
        Carril largeCarril = new Carril(50, "Large");

        assertEquals(5, smallCarril.size());
        assertEquals(50, largeCarril.size());
    }

    @Test
    @DisplayName("Debe preservar el nombre después de reiniciar")
    void testNamePreservedAfterRestart() {
        carril.displayPasos(10);
        assertEquals("10", carril.getLlegada().getText());

        carril.reStart();
        assertEquals("Carril1", carril.getLlegada().getText());
        assertEquals("Carril1", carril.getName());
    }

    @Test
    @DisplayName("Debe manejar actualizaciones rápidas de displayPasos")
    void testRapidDisplayPasosUpdates() {
        for (int i = 0; i <= 10; i++) {
            carril.displayPasos(i);
            assertEquals(String.valueOf(i), carril.getLlegada().getText());
        }
    }

    @Test
    @DisplayName("Finish debe sobrescribir displayPasos")
    void testFinishOverridesDisplayPasos() {
        carril.displayPasos(5);
        assertEquals("5", carril.getLlegada().getText());

        carril.finish();
        assertEquals("!", carril.getLlegada().getText());
    }

    @Test
    @DisplayName("Debe manejar carril de tamaño 1")
    void testMinimalCarril() {
        Carril minimal = new Carril(1, "Min");
        assertEquals(1, minimal.size());
        assertNotNull(minimal.getPaso(0));
    }

    @Test
    @DisplayName("Debe permitir marcar y desmarcar el mismo paso múltiples veces")
    void testTogglePasoMultipleTimes() {
        carril.setPasoOn(3);
        assertEquals("o", carril.getPaso(3).getText());

        carril.setPasoOff(3);
        assertEquals("", carril.getPaso(3).getText());

        carril.setPasoOn(3);
        assertEquals("o", carril.getPaso(3).getText());

        carril.setPasoOff(3);
        assertEquals("", carril.getPaso(3).getText());
    }

    @Test
    @DisplayName("Debe manejar una carrera completa simulada")
    void testSimulatedRace() {
        for (int i = 0; i < carril.size(); i++) {
            carril.setPasoOn(i);
            carril.displayPasos(i + 1);
        }
        carril.finish();

        for (int i = 0; i < carril.size(); i++) {
            assertEquals("o", carril.getPaso(i).getText());
        }
        assertEquals("!", carril.getLlegada().getText());
    }
}