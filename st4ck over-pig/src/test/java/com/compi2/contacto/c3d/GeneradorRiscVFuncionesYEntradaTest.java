package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVFuncionesYEntradaTest {

    private static Cuarteta c(OperadorCuarteta op, String a, String b, String r) {
        return new Cuarteta(op, a, b, r);
    }

    private String generar(Cuarteta... instrucciones) {
        ProgramaIntermedio p = new ProgramaIntermedio();
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        for (Cuarteta q : instrucciones) p.agregar(q);
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        return new GeneradorRiscV().generar(p);
    }

    @Test
    void leeTextoComoCadenaTerminadaEnMenosUno() {
        String s = generar(c(OperadorCuarteta.LEER, "textum", "-", "str_t0"),
                c(OperadorCuarteta.IMPRIMIR, "@STR@str_t0", "println", "-"));
        assertTrue(s.contains("li a7, 8"));
        assertTrue(s.contains("rv_read_string_loop:"));
        assertTrue(s.contains("li a0, -1"));
        assertTrue(s.contains("jal ra, rv_append_char"));
        assertTrue(s.contains("rv_print_"));
    }

    @Test
    void leeCadenaParaYyZetariano() {
        String s = generar(c(OperadorCuarteta.LEER, "cadena", "-", "y_t0"),
                c(OperadorCuarteta.LEER, "String", "-", "z_t0"));
        assertTrue(s.contains("rv_entrada: .space 4096"));
        assertTrue(s.contains("jal ra, rv_read_string"));
    }

    @Test
    void descartaLineaDeEntradaSinAsignacion() {
        String s = generar(c(OperadorCuarteta.LEER, "descartar", "-", "-"));
        assertTrue(s.contains("jal ra, rv_read_string"));
    }

    @Test
    void imprimeTextoLeidoDesdeHeap() {
        String s = generar(c(OperadorCuarteta.LEER, "textum", "-", "str_t1"),
                c(OperadorCuarteta.IMPRIMIR, "@STR@str_t1", "println", "-"));
        assertTrue(s.contains("la t5, rv_heap"));
        assertTrue(s.contains("li a7, 11"));
    }

    @Test
    void protegeRecursionExcesiva() {
        String s = generar(c(OperadorCuarteta.LLAMAR, "MAIOR", "0", "-"));
        assertTrue(s.contains("rv_profundidad: .word 0"));
        assertTrue(s.contains("li t5, 1024"));
        assertTrue(s.contains("rv_error_recursion:"));
    }

    @Test
    void conservaDireccionDeRetornoConLlamadasAnidadas() {
        ProgramaIntermedio p = new ProgramaIntermedio();
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "subrutina", "void", "0"));
        p.agregar(c(OperadorCuarteta.RETORNAR, "-", "-", "-"));
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "subrutina", "-", "-"));
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        p.agregar(c(OperadorCuarteta.LLAMAR, "subrutina", "0", "-"));
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        String s = new GeneradorRiscV().generar(p);
        assertTrue(s.contains("sw ra, 0(sp)"));
        assertTrue(s.contains("lw ra, 0(s0)"));
        assertTrue(s.contains("jal ra, rv_fn_0"));
    }

    @Test
    void soportaMasDeDoscientosTemporalesEnUnMarco() {
        ProgramaIntermedio p = new ProgramaIntermedio();
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        for (int i = 0; i < 300; i++) {
            p.agregar(c(OperadorCuarteta.ASIGNAR, String.valueOf(i), "-", "tmp_" + i));
        }
        p.agregar(c(OperadorCuarteta.IMPRIMIR, "tmp_299", "-", "-"));
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        String s = new GeneradorRiscV().generar(p);
        assertTrue(s.contains("add sp, sp, t6"));
        assertTrue(s.contains("add t6, s0, t6"));
        assertTrue(s.contains("lw t0, 0(t6)"));
    }

    @Test
    void noExportaLlamadasSinFuncionDeDestino() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> generar(c(OperadorCuarteta.LLAMAR, "noExiste", "0", "-")));
        assertTrue(e.getMessage().contains("no definida"));
    }

    @Test
    void noExportaObjetosSinBajarAMemoria() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> generar(c(OperadorCuarteta.NUEVO_OBJETO, "Animal", "-", "tmp_1")));
        assertTrue(e.getMessage().contains("sin bajar a memoria"));
    }

    @Test
    void mantieneCadenasYDecimalesEnGeneracion() {
        String s = generar(c(OperadorCuarteta.SUMAR, "@STR@str_t0", "3.5", "str_t1"),
                c(OperadorCuarteta.DIVIDIR, "9", "2", "tmp_0"));
        assertTrue(s.contains("jal ra, rv_concat"));
        assertTrue(s.contains("fdiv.s ft2, ft0, ft1"));
        assertTrue(!s.isBlank());
    }
}
