package com.compi2.contacto.interfaz;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalidasGeneradasTest {

    @Test
    void tresGeneradoresExitosos() {
        SalidasGeneradas salida = SalidasGeneradas.generar(
                new ProgramaIntermedio(), p -> "C3D", p -> "C", p -> "RISC-V");
        assertEquals("C3D", salida.c3d().codigo());
        assertEquals("C", salida.c().codigo());
        assertEquals("RISC-V", salida.riscV().codigo());
        assertTrue(salida.c3d().generada());
        assertTrue(salida.c().generada());
        assertTrue(salida.riscV().generada());
    }

    @Test
    void fallaCodigoCYRiscVSigueGenerandose() {
        AtomicInteger llamadasRiscV = new AtomicInteger();
        SalidasGeneradas salida = SalidasGeneradas.generar(
                new ProgramaIntermedio(),
                p -> "C3D",
                p -> { throw new IllegalArgumentException("Fallo en C"); },
                p -> { llamadasRiscV.incrementAndGet(); return "ASM"; });
        assertFalse(salida.c().generada());
        assertTrue(salida.riscV().generada());
        assertEquals(1, llamadasRiscV.get());
        assertEquals("", salida.c().codigo());
        assertTrue(salida.c().mostrar("codigo C").contains("Fallo en C"));
    }

    @Test
    void fallaRiscVYCodigoCSeMantiene() {
        SalidasGeneradas salida = SalidasGeneradas.generar(
                new ProgramaIntermedio(),
                p -> "C3D",
                p -> "C VALIDO",
                p -> { throw new IllegalStateException("Operacion no soportada"); });
        assertTrue(salida.c().generada());
        assertFalse(salida.riscV().generada());
        assertEquals("C VALIDO", salida.c().codigo());
        assertEquals("", salida.riscV().codigo());
        assertTrue(salida.riscV().mostrar("codigo RISC-V").contains("Operacion no soportada"));
        assertTrue(salida.estado().contains("RISC-V: NO GENERADO"));
    }

    @Test
    void fallaC3DYLosOtrosBackendsSiguen() {
        SalidasGeneradas salida = SalidasGeneradas.generar(
                new ProgramaIntermedio(),
                p -> { throw new IllegalArgumentException("Fallo de C3D"); },
                p -> "C",
                p -> "RV");
        assertFalse(salida.c3d().generada());
        assertTrue(salida.c().generada());
        assertTrue(salida.riscV().generada());
    }

    @Test
    void textoVacioNoEsCodigoGenerado() {
        SalidasGeneradas salida = SalidasGeneradas.generar(
                new ProgramaIntermedio(), p -> "", p -> "   ", p -> null);
        assertFalse(salida.c3d().generada());
        assertFalse(salida.c().generada());
        assertFalse(salida.riscV().generada());
        assertTrue(salida.riscV().error().contains("vacio"));
    }

    @Test
    void errorSinMensajeEsPresentable() {
        SalidasGeneradas salida = SalidasGeneradas.generar(
                new ProgramaIntermedio(),
                p -> "C3D", p -> "C",
                p -> { throw new IllegalStateException(); });
        assertFalse(salida.riscV().generada());
        assertEquals("IllegalStateException", salida.riscV().error());
    }

    @Test
    void rechazaDependenciasNulas() {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        assertThrows(NullPointerException.class,
                () -> SalidasGeneradas.generar(null, p -> "a", p -> "b", p -> "c"));
        assertThrows(NullPointerException.class,
                () -> SalidasGeneradas.generar(programa, null, p -> "b", p -> "c"));
        assertThrows(NullPointerException.class,
                () -> SalidasGeneradas.generar(programa, p -> "a", null, p -> "c"));
        assertThrows(NullPointerException.class,
                () -> SalidasGeneradas.generar(programa, p -> "a", p -> "b", null));
    }

    @Test
    void generadoresRealesProducenCodigo() {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        programa.agregar(new Cuarteta(
                OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        programa.agregar(new Cuarteta(
                OperadorCuarteta.IMPRIMIR, "5", "println", "-"));
        programa.agregar(new Cuarteta(
                OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        SalidasGeneradas salida = SalidasGeneradas.generar(programa);
        assertTrue(salida.c3d().generada(), salida.c3d().error());
        assertTrue(salida.c().generada(), salida.c().error());
        assertTrue(salida.riscV().generada(), salida.riscV().error());
        assertTrue(salida.riscV().codigo().contains(".globl main"));
    }
}
