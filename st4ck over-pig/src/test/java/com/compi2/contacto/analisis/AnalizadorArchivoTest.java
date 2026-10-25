package com.compi2.contacto.analisis;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalizadorArchivoTest {

    private final AnalizadorArchivo analizador = new AnalizadorArchivo();

    @Test
    void aceptaTokensInicialesDeLenguajeY() {
        ArchivoFuente archivo = new ArchivoFuente(
                Path.of("Funciones.y"),
                LenguajeFuente.Y,
                "%funciones\ndefinir sumar(entero a, entero b) -> entero:\n"
                        + "    retornar a + b\n"
        );

        assertTrue(analizador.analizar(archivo).esValido());
    }

    @Test
    void reportaSangriaInconsistenteEnLenguajeY() {
        ArchivoFuente archivo = new ArchivoFuente(
                Path.of("Funciones.y"),
                LenguajeFuente.Y,
                "%funciones\n"
                        + "definir prueba():\n"
                        + "    si(verdadero) entonces\n"
                        + "        imprimir(\"x\")\n"
                        + "      retornar 1\n"
        );

        ResultadoAnalisisArchivo resultado = analizador.analizar(archivo);

        assertFalse(resultado.esValido());
        assertEquals(5, resultado.diagnosticos().get(0).linea());
    }

    @Test
    void aceptaClaseZetarianaConNombreCorrecto() {
        ArchivoFuente archivo = new ArchivoFuente(
                Path.of("Persona.z"),
                LenguajeFuente.ZETARIANO,
                "public class Persona { int edad; }"
        );

        assertTrue(analizador.analizar(archivo).esValido());
    }

    @Test
    void rechazaClaseZetarianaConNombreDistinto() {
        ArchivoFuente archivo = new ArchivoFuente(
                Path.of("Otro.z"),
                LenguajeFuente.ZETARIANO,
                "public class Persona { int edad; }"
        );

        assertFalse(analizador.analizar(archivo).esValido());
    }

    @Test
    void reportaCaracterNoReconocido() {
        ArchivoFuente archivo = new ArchivoFuente(
                Path.of("principal.pig"),
                LenguajeFuente.PIG_LATIN,
                "MAIOR> @ FINIS;"
        );

        assertFalse(analizador.analizar(archivo).esValido());
    }
}
