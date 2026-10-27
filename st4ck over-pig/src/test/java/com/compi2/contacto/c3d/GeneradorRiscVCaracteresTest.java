package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVCaracteresTest {
    private static Cuarteta c(OperadorCuarteta operador, String a, String b, String resultado) {
        return new Cuarteta(operador, a, b, resultado);
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
    void cargaCaracterComoValorEntero() {
        String salida = generar(c(OperadorCuarteta.ASIGNAR, "'A'", "-", "valor_t0"));
        assertTrue(salida.contains("li t0, 65"));
        assertTrue(salida.contains("li a4, 0"));
    }

    @Test
    void imprimeCaracterConServicioDeCaracter() {
        String salida = generar(c(OperadorCuarteta.IMPRIMIR, "'A'", "println:char", "-"));
        assertTrue(salida.contains("li t0, 65"));
        assertTrue(salida.contains("li a7, 11"));
    }

    @Test
    void convierteCaracterParaOperacionNumerica() {
        String salida = generar(c(OperadorCuarteta.SUMAR, "'A'", "1", "resultado_t0"));
        assertTrue(salida.contains("li t0, 65"));
        assertTrue(salida.contains("li t1, 1"));
        assertTrue(salida.contains("add t2, t0, t1"));
    }

    @Test
    void aceptaCaracteresEscapados() {
        String salida = generar(
                c(OperadorCuarteta.ASIGNAR, "'\\n'", "-", "v0"),
                c(OperadorCuarteta.ASIGNAR, "'\\t'", "-", "v1"),
                c(OperadorCuarteta.ASIGNAR, "'\\\\'", "-", "v2"),
                c(OperadorCuarteta.ASIGNAR, "'\\\''", "-", "v3"),
                c(OperadorCuarteta.ASIGNAR, "'\\0'", "-", "v4"));
        assertTrue(salida.contains("li t0, 10"));
        assertTrue(salida.contains("li t0, 9"));
        assertTrue(salida.contains("li t0, 92"));
        assertTrue(salida.contains("li t0, 39"));
        assertTrue(salida.contains("li t0, 0"));
    }

    @Test
    void aceptaCaracteresNoAscii() {
        String salida = generar(c(OperadorCuarteta.ASIGNAR, "'ñ'", "-", "valor_t0"));
        assertTrue(salida.contains("li t0, 241"));
    }

    @Test
    void rechazaLiteralConVariosCaracteres() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> generar(c(OperadorCuarteta.ASIGNAR, "'AB'", "-", "valor_t0")));
        assertTrue(error.getMessage().contains("literal de caracter invalido"));
    }

    @Test
    void rechazaEscapeDesconocido() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> generar(c(OperadorCuarteta.ASIGNAR, "'\\q'", "-", "valor_t0")));
        assertTrue(error.getMessage().contains("escape de caracter no soportado"));
    }
}
