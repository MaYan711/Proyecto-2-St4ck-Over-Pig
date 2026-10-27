package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVImpresionNumericaTest {
    private static Cuarteta c(OperadorCuarteta op, String a, String b, String r) {
        return new Cuarteta(op, a, b, r);
    }

    private static String generar(Cuarteta... instrucciones) {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        programa.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        for (Cuarteta instruccion : instrucciones) {
            programa.agregar(instruccion);
        }
        programa.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        return new GeneradorRiscV().generar(programa);
    }

    @Test
    void numerosDecimalesConParteFraccionariaCeroUsanRutaEnteraCuandoSonExactos() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "3.0", "println", "-"));
        assertTrue(asm.contains("fcvt.w.d t1, ft0, rtz"));
        assertTrue(asm.contains("fcvt.d.w ft1, t1"));
        assertTrue(asm.contains("feq.d t2, ft0, ft1"));
        assertTrue(asm.contains("mv a0, t1\n    li a7, 1\n    ecall"));
    }

    @Test
    void numerosFraccionariosConservanImpresionFlotante() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "3.5", "println", "-"));
        assertTrue(asm.contains("j rv_print_flotante_"));
        assertTrue(asm.contains("fsgnj.d fa0, ft0, ft0\n    li a7, 3\n    ecall"));
    }

    @Test
    void noConvierteFlotantesFueraDelRangoDeInt32() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "2147483648.0", "print", "-"));
        assertTrue(asm.contains("li t1, 2147483647"));
        assertTrue(asm.contains("flt.d t2, ft1, ft0"));
        assertTrue(asm.contains("li t1, -2147483648"));
        assertTrue(asm.contains("flt.d t2, ft0, ft1"));
    }

    @Test
    void conservaImpresionNormalDeEnteros() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "5", "println", "-"));
        assertTrue(asm.contains("j rv_print_entero_"));
        assertTrue(asm.contains("mv a0, t0\n    li a7, 1\n    ecall"));
    }

    @Test
    void conservaLaImpresionDeCaracteres() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "'A'", "println:char", "-"));
        assertTrue(asm.contains("li t0, 65"));
        assertTrue(asm.contains("mv a0, t0\n    li a7, 11\n    ecall"));
        assertFalse(asm.contains("rv_print_flotante_"));
    }

    @Test
    void conservaImpresionBooleana() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "true", "println:boolean", "-"));
        assertTrue(asm.contains("la a0, rv_bool_true"));
        assertTrue(asm.contains("la a0, rv_bool_false"));
        assertFalse(asm.contains("rv_print_flotante_"));
    }

    @Test
    void mantieneLaNuevaLineaDePrintln() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "3.0", "println", "-"));
        assertTrue(asm.contains("li a0, 10\n    li a7, 11\n    ecall"));
    }
}
