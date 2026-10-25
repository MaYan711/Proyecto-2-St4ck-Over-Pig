package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVTest {
    private ProgramaIntermedio programa(Cuarteta... instrucciones) {
        ProgramaIntermedio p = new ProgramaIntermedio();
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        for (Cuarteta c : instrucciones) p.agregar(c);
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        return p;
    }

    private Cuarteta c(OperadorCuarteta operacion, String a, String b, String resultado) {
        return new Cuarteta(operacion, a, b, resultado);
    }

    @Test
    void generaProgramaConSalidaEntera() {
        String salida = new GeneradorRiscV().generar(programa(
                c(OperadorCuarteta.IMPRIMIR, "222", "println", "-")));
        assertTrue(salida.contains(".globl main"));
        assertTrue(salida.contains("li a7, 1"));
        assertTrue(salida.contains("li a0, 10"));
        assertTrue(salida.contains("jal ra, rv_fn_0"));
    }

    @Test
    void operaYControlaFlujo() {
        String salida = new GeneradorRiscV().generar(programa(
                c(OperadorCuarteta.SUMAR, "2", "3", "call_t0"),
                c(OperadorCuarteta.MENOR, "call_t0", "8", "call_t1"),
                c(OperadorCuarteta.SALTAR_SI_FALSO, "call_t1", "-", "Lfin"),
                c(OperadorCuarteta.MULTIPLICAR, "call_t0", "4", "call_t2"),
                c(OperadorCuarteta.ETIQUETA, "-", "-", "Lfin"),
                c(OperadorCuarteta.IMPRIMIR, "call_t0", "println", "-")));
        assertTrue(salida.contains("add t2, t0, t1"));
        assertTrue(salida.contains("slt t2, t0, t1"));
        assertTrue(salida.contains("j rv_lbl_0_0"));
        assertTrue(salida.contains("mul t2, t0, t1"));
    }

    @Test
    void usaStackHeapYTipoDinamico() {
        String salida = new GeneradorRiscV().generar(programa(
                c(OperadorCuarteta.ASIGNAR, "H", "-", "obj_t0"),
                c(OperadorCuarteta.ESCRIBIR_TIPO_OBJETO, "obj_t0", "2", "-"),
                c(OperadorCuarteta.ESCRIBIR_HEAP, "obj_t0+1", "42", "-"),
                c(OperadorCuarteta.LEER_HEAP, "obj_t0+1", "-", "obj_t1"),
                c(OperadorCuarteta.ESCRIBIR_STACK, "P+1", "obj_t1", "-"),
                c(OperadorCuarteta.LEER_STACK, "P+1", "-", "obj_t2"),
                c(OperadorCuarteta.LEER_TIPO_OBJETO, "obj_t0", "-", "obj_t3")));
        assertTrue(salida.contains("la t5, rv_stack"));
        assertTrue(salida.contains("la t5, rv_heap"));
        assertTrue(salida.contains("la t5, rv_tipo"));
        assertTrue(salida.contains("rv_error_referencia:"));
    }

    @Test
    void llamaFuncionesConMarcoIndependiente() {
        ProgramaIntermedio p = new ProgramaIntermedio();
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "Padre.mostrar()", "void", "1"));
        p.agregar(c(OperadorCuarteta.ASIGNAR, "10", "-", "call_t0"));
        p.agregar(c(OperadorCuarteta.IMPRIMIR, "call_t0", "println", "-"));
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "Padre.mostrar()", "-", "-"));
        p.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        p.agregar(c(OperadorCuarteta.LLAMAR, "Padre.mostrar()", "1", "-"));
        p.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        String salida = new GeneradorRiscV().generar(p);
        assertTrue(salida.contains("jal ra, rv_fn_0"));
        assertTrue(salida.contains("rv_fn_1:"));
        assertTrue(salida.contains("sw ra, 0(sp)"));
        assertTrue(salida.contains("lw ra, 0(s0)"));
    }

    @Test
    void compilaLiteralDecimalConEtiquetaDeTipo() {
        String salida = new GeneradorRiscV().generar(programa(
                c(OperadorCuarteta.ASIGNAR, "3.5", "-", "pig_t0")));
        assertTrue(salida.contains("rv_stack_decimal: .space 400000"));
        assertTrue(salida.contains("li a4, 1"));
        assertTrue(salida.contains("sw a4, 12(s0)"));
    }

    @Test
    void divideEnterosConResultadoDecimal() {
        String salida = new GeneradorRiscV().generar(programa(
                c(OperadorCuarteta.DIVIDIR, "5", "2", "pig_t0")));
        assertTrue(salida.contains("fdiv.s ft2, ft0, ft1"));
        assertTrue(salida.contains("fcvt.s.w ft0, t0"));
        assertTrue(salida.contains("rv_error_division:"));
    }

    @Test
    void rechazaOperacionNoReducida() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GeneradorRiscV().generar(programa(
                        c(OperadorCuarteta.NUEVO_OBJETO, "Persona", "-", "pig_t0"))));
        assertTrue(ex.getMessage().contains("sin bajar"));
    }

    @Test
    void aceptaConcatenacionCadenaEntero() {
        String salida = new GeneradorRiscV().generar(programa(
                c(OperadorCuarteta.SUMAR, "@STR@str_t0", "22", "pig_t1")));
        assertTrue(salida.contains("jal ra, rv_concat"));
        assertTrue(salida.contains("li a2, 1"));
        assertTrue(salida.contains("li a3, 0"));
    }
}
