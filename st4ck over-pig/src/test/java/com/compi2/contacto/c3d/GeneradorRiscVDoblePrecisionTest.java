package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVDoblePrecisionTest {
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
    void literalPreservaLosBitsDeUnDouble() {
        String asm = generar(c(OperadorCuarteta.ASIGNAR, "16777217.0", "-", "z_t0"));
        long bits = Double.doubleToRawLongBits(16777217.0);
        assertTrue(asm.contains("li t0, " + (int) bits));
        assertTrue(asm.contains("li s10, " + (int) (bits >>> 32)));
        assertTrue(asm.contains("sw s10, 28(s0)"));
    }

    @Test
    void restaUsaOperacionDeDoblePrecision() {
        String asm = generar(c(OperadorCuarteta.RESTAR, "16777217.0", "16777216.0", "z_t0"),
                c(OperadorCuarteta.IMPRIMIR, "z_t0", "println", "-"));
        assertTrue(asm.contains("fsub.d ft2, ft0, ft1"));
        assertTrue(asm.contains("fsd ft2, 0(t6)"));
        assertTrue(asm.contains("fld ft0, 0(t6)"));
        assertFalse(asm.contains("fsub.s ft2, ft0, ft1"));
    }

    @Test
    void conservaParteAltaEnPilaYHeap() {
        String asm = generar(c(OperadorCuarteta.ESCRIBIR_STACK, "P+1", "16777217.0", "-"),
                c(OperadorCuarteta.LEER_STACK, "P+1", "-", "z_t0"),
                c(OperadorCuarteta.ESCRIBIR_HEAP, "H", "z_t0", "-"),
                c(OperadorCuarteta.LEER_HEAP, "H", "-", "z_t1"));
        assertTrue(asm.contains("rv_stack_decimal_alto: .space 400000"));
        assertTrue(asm.contains("rv_heap_decimal_alto: .space 400000"));
        assertTrue(asm.contains("sw s11, 0(t3)"));
        assertTrue(asm.contains("lw s10, 0(t3)"));
    }

    @Test
    void imprimeUnDoubleEnRars() {
        String asm = generar(c(OperadorCuarteta.IMPRIMIR, "3.5", "println", "-"));
        assertTrue(asm.contains("fsgnj.d fa0, ft0, ft0"));
        assertTrue(asm.contains("li a7, 3"));
    }

    @Test
    void leeUnDoubleEnRars() {
        String asm = generar(c(OperadorCuarteta.LEER, "double", "-", "z_t0"));
        assertTrue(asm.contains("li a7, 7"));
        assertTrue(asm.contains("fsd fa0, 0(t6)"));
    }

    @Test
    void operacionMixtaPromueveEnteroADouble() {
        String asm = generar(c(OperadorCuarteta.SUMAR, "2", "3.5", "z_t0"));
        assertTrue(asm.contains("fcvt.d.w ft0, t0"));
        assertTrue(asm.contains("fadd.d ft2, ft0, ft1"));
    }

    @Test
    void divisionConservaDoubleYControlaCero() {
        String asm = generar(c(OperadorCuarteta.DIVIDIR, "7", "2", "z_t0"));
        assertTrue(asm.contains("fdiv.d ft2, ft0, ft1"));
        assertTrue(asm.contains("feq.d t3, ft1, ft3"));
        assertTrue(asm.contains("rv_error_division:"));
    }

    @Test
    void preservaRegistrosEntreLlamadasRecursivas() {
        String asm = generar(c(OperadorCuarteta.LLAMAR, "MAIOR", "0", "-"));
        assertTrue(asm.contains("sw s9, 8(sp)"));
        assertTrue(asm.contains("sw s10, 12(sp)"));
        assertTrue(asm.contains("sw s11, 16(sp)"));
        assertTrue(asm.contains("lw s11, 16(s0)"));
    }
}
