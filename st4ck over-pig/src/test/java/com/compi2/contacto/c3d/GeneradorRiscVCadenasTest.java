package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVCadenasTest {

    private String generar(Cuarteta... instrucciones) {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        programa.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        for (Cuarteta instruccion : instrucciones) {
            programa.agregar(instruccion);
        }
        programa.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));
        return new GeneradorRiscV().generar(programa);
    }

    private Cuarteta c(OperadorCuarteta operacion, String arg1, String arg2, String resultado) {
        return new Cuarteta(operacion, arg1, arg2, resultado);
    }

    @Test
    void concatenaDosReferenciasDeCadena() {
        String salida = generar(
                c(OperadorCuarteta.SUMAR, "@STR@str_t0", "@STR@str_t1", "str_t2"),
                c(OperadorCuarteta.IMPRIMIR, "@STR@str_t2", "println", "-"));
        assertTrue(salida.contains("li a2, 1"));
        assertTrue(salida.contains("li a3, 1"));
        assertTrue(salida.contains("jal ra, rv_concat"));
        assertTrue(salida.contains("rv_append_string_loop:"));
        assertTrue(salida.contains("rv_print_0_"));
    }

    @Test
    void concatenaNumeroAntesDeCadena() {
        String salida = generar(c(OperadorCuarteta.SUMAR, "12", "@STR@str_t0", "str_t1"));
        assertTrue(salida.contains("li a2, 0"));
        assertTrue(salida.contains("li a3, 1"));
        assertTrue(salida.contains("jal ra, rv_concat"));
    }

    @Test
    void incluyeConversionDeEnterosNegativos() {
        String salida = generar(c(OperadorCuarteta.SUMAR, "@STR@str_t0", "-2147483648", "str_t1"));
        assertTrue(salida.contains("li a1, -2147483648"));
        assertTrue(salida.contains("neg s1, s1"));
        assertTrue(salida.contains("remu t3, s1, t5"));
        assertTrue(salida.contains("divu s1, s1, t5"));
    }

    @Test
    void insertaRepresentacionNullEnConcatenacion() {
        String salida = generar(c(OperadorCuarteta.SUMAR, "@STR@null", "@STR@str_t0", "str_t1"));
        assertTrue(salida.contains("li a0, -1"));
        assertTrue(salida.contains("rv_append_string_null:"));
    }

    @Test
    void verificaLimitesDeHeapAlCrearCadena() {
        String salida = generar(c(OperadorCuarteta.SUMAR, "@STR@str_t0", "15", "str_t1"));
        assertTrue(salida.contains("rv_append_char:"));
        assertTrue(salida.contains("j rv_error_memoria"));
        assertTrue(salida.contains("rv_append_number_print:"));
    }

    @Test
    void conservaOperacionesNumericasNormales() {
        String salida = generar(c(OperadorCuarteta.SUMAR, "30", "12", "pig_t0"));
        assertTrue(salida.contains("add t2, t0, t1"));
    }
}
