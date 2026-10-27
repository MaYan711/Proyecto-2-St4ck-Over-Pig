package com.compi2.contacto;

import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LenguajeFuenteTest {

    @Test
    void reconoceLasTresExtensiones() {
        assertEquals(
                LenguajeFuente.Y,
                LenguajeFuente.desdeRuta(Path.of("Funciones.y")).orElseThrow()
        );
        assertEquals(
                LenguajeFuente.ZETARIANO,
                LenguajeFuente.desdeRuta(Path.of("Persona.z")).orElseThrow()
        );
        assertEquals(
                LenguajeFuente.PIG_LATIN,
                LenguajeFuente.desdeRuta(Path.of("principal.pig")).orElseThrow()
        );
    }

    @Test
    void ignoraArchivosNoSoportados() {
        assertTrue(
                LenguajeFuente.desdeRuta(Path.of("README.md")).isEmpty()
        );
    }
}

