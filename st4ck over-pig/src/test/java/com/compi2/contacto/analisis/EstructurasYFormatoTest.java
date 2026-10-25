package com.compi2.contacto.analisis;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstructurasYFormatoTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaUnicamenteFormatoIndentadoDeEstructuraY() {
        String valido = """
                %estructuras
                estructura Persona:
                    cadena nombre
                    entero edad
                %funciones
                definir prueba():
                    imprimir("ok")
                """;

        assertTrue(
                analizar(valido).esValido()
        );

        String invalido = """
                %estructuras
                estructura {
                    cadena nombre;
                    entero edad;
                } Persona;
                %funciones
                definir prueba():
                    imprimir("ok")
                """;

        assertFalse(
                analizar(invalido).esValido()
        );
    }

    private ResultadoAnalisisArchivo analizar(
            String codigo
    ) {
        return analizador.analizar(
                new ArchivoFuente(
                        Path.of("Estructuras.y"),
                        LenguajeFuente.Y,
                        codigo
                )
        );
    }
}