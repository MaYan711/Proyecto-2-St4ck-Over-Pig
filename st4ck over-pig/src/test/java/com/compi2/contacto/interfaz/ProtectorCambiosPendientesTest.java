package com.compi2.contacto.interfaz;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JOptionPane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectorCambiosPendientesTest {

    @Test
    void sinCambiosPermiteAbrirOtroProyectoSinGuardar() {
        AtomicInteger guardados = new AtomicInteger();
        assertTrue(ProtectorCambiosPendientes.continuar(
                false, JOptionPane.CLOSED_OPTION, () -> {
                    guardados.incrementAndGet();
                    return false;
                }));
        assertEquals(0, guardados.get());
    }

    @Test
    void guardarExitosoPermiteContinuar() {
        AtomicInteger guardados = new AtomicInteger();
        assertTrue(ProtectorCambiosPendientes.continuar(
                true, JOptionPane.YES_OPTION, () -> {
                    guardados.incrementAndGet();
                    return true;
                }));
        assertEquals(1, guardados.get());
    }

    @Test
    void falloDeGuardadoImpideContinuar() {
        assertFalse(ProtectorCambiosPendientes.continuar(
                true, JOptionPane.YES_OPTION, () -> false));
    }

    @Test
    void descartarCambiosPermiteContinuarSinGuardar() {
        AtomicInteger guardados = new AtomicInteger();
        assertTrue(ProtectorCambiosPendientes.continuar(
                true, JOptionPane.NO_OPTION, () -> {
                    guardados.incrementAndGet();
                    return true;
                }));
        assertEquals(0, guardados.get());
    }

    @Test
    void cancelarImpideContinuarSinGuardar() {
        AtomicInteger guardados = new AtomicInteger();
        assertFalse(ProtectorCambiosPendientes.continuar(
                true, JOptionPane.CANCEL_OPTION, () -> {
                    guardados.incrementAndGet();
                    return true;
                }));
        assertEquals(0, guardados.get());
    }

    @Test
    void cerrarDialogoImpideContinuar() {
        assertFalse(ProtectorCambiosPendientes.continuar(
                true, JOptionPane.CLOSED_OPTION, () -> true));
    }

    @Test
    void opcionDesconocidaNoDescartaCambios() {
        assertFalse(ProtectorCambiosPendientes.continuar(
                true, 999, () -> true));
    }

    @Test
    void requiereAccionDeGuardado() {
        assertThrows(NullPointerException.class,
                () -> ProtectorCambiosPendientes.continuar(false, JOptionPane.NO_OPTION, null));
    }
}
