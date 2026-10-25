package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVDecimalesMemoriaTest {
    private static Cuarteta c(OperadorCuarteta o, String a, String b, String r) {
        return new Cuarteta(o, a, b, r);
    }

    private String generar(Cuarteta... cuerpo) {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        programa.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        for (Cuarteta q : cuerpo) programa.agregar(q);
        programa.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        return new GeneradorRiscV().generar(programa);
    }

    @Test void sumaDecimalConEntero() {
        String s = generar(c(OperadorCuarteta.SUMAR, "3.25", "2", "pig_t0"));
        assertTrue(s.contains("fadd.s ft2, ft0, ft1"));
        assertTrue(s.contains("fcvt.s.w ft1, t1"));
    }

    @Test void restaYMultiplicaDecimal() {
        String s = generar(c(OperadorCuarteta.RESTAR, "5.75", "1.5", "pig_t0"),
                c(OperadorCuarteta.MULTIPLICAR, "pig_t0", "2", "pig_t1"));
        assertTrue(s.contains("fsub.s ft2, ft0, ft1"));
        assertTrue(s.contains("fmul.s ft2, ft0, ft1"));
    }

    @Test void comparaDecimales() {
        String s = generar(c(OperadorCuarteta.MENOR, "1.25", "1.5", "pig_t0"),
                c(OperadorCuarteta.MAYOR_IGUAL, "2.25", "2", "pig_t1"));
        assertTrue(s.contains("flt.s t2, ft0, ft1"));
        assertTrue(s.contains("fle.s t2, ft1, ft0"));
    }

    @Test void soportaRestoDecimal() {
        String s = generar(c(OperadorCuarteta.MODULO, "5.5", "2", "pig_t0"));
        assertTrue(s.contains("fcvt.w.s t3, ft2, rtz"));
        assertTrue(s.contains("fsub.s ft2, ft0, ft2"));
    }

    @Test void conservaTipoAlPasarPorStack() {
        String s = generar(c(OperadorCuarteta.ESCRIBIR_STACK, "P+1", "3.75", "-"),
                c(OperadorCuarteta.LEER_STACK, "P+1", "-", "pig_t0"));
        assertTrue(s.contains("la t3, rv_stack_decimal"));
        assertTrue(s.contains("sw a5, 0(t3)"));
        assertTrue(s.contains("lw a4, 0(t3)"));
    }

    @Test void conservaTipoAlPasarPorHeap() {
        String s = generar(c(OperadorCuarteta.ESCRIBIR_HEAP, "H", "1.25", "-"),
                c(OperadorCuarteta.LEER_HEAP, "H", "-", "pig_t0"));
        assertTrue(s.contains("la t3, rv_heap_decimal"));
        assertTrue(s.contains("sw a5, 0(t3)"));
        assertTrue(s.contains("lw a4, 0(t3)"));
    }

    @Test void imprimeDecimalConEcallRars() {
        String s = generar(c(OperadorCuarteta.IMPRIMIR, "4.5", "println", "-"));
        assertTrue(s.contains("fmv.w.x fa0, t0"));
        assertTrue(s.contains("li a7, 2"));
    }

    @Test void leeDecimalConEcallRars() {
        String s = generar(c(OperadorCuarteta.LEER, "decimalis", "-", "pig_t0"));
        assertTrue(s.contains("li a7, 6"));
        assertTrue(s.contains("fmv.x.w t0, fa0"));
    }

    @Test void concatenacionDecimalNoImprimeBits() {
        String s = generar(c(OperadorCuarteta.SUMAR, "@STR@str_t0", "2.5", "str_t1"));
        assertTrue(s.contains("rv_append_float:"));
        assertTrue(s.contains("jal ra, rv_append_float"));
        assertTrue(s.contains("mv s7, a5"));
    }

    @Test void accesoArregloRespetaHeapYLimites() {
        String s = generar(c(OperadorCuarteta.ASIGNAR, "H", "-", "arr_t0"),
                c(OperadorCuarteta.ESCRIBIR_HEAP, "arr_t0+2", "40", "-"),
                c(OperadorCuarteta.LEER_HEAP, "arr_t0+2", "-", "arr_t1"));
        assertTrue(s.contains("la t5, rv_heap"));
        assertTrue(s.contains("j rv_error_memoria"));
    }

    @Test void camposDeEstructuraOcupanDesplazamientosDistintos() {
        String s = generar(c(OperadorCuarteta.ASIGNAR, "H", "-", "obj_t0"),
                c(OperadorCuarteta.ESCRIBIR_HEAP, "obj_t0+0", "10", "-"),
                c(OperadorCuarteta.ESCRIBIR_HEAP, "obj_t0+1", "2.5", "-"),
                c(OperadorCuarteta.LEER_HEAP, "obj_t0+1", "-", "obj_t1"));
        assertTrue(s.contains("addi t0, t0, 1"));
        assertTrue(s.contains("la t3, rv_heap_decimal"));
    }

    @Test void manejaNegativosFlotantes() {
        String s = generar(c(OperadorCuarteta.NEGATIVO, "1.25", "-", "pig_t0"));
        assertTrue(s.contains("fneg.s ft0, ft0"));
    }
}
