package com.compi2.contacto.compilacion;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiladorProyectoTest {

    @Test
    void aceptaProyectoInicialConArchivoPig() {
        ProyectoCompilacion proyecto = new ProyectoCompilacion(
                Path.of("proyecto"),
                List.of(new ArchivoFuente(
                        Path.of("proyecto", "principal.pig"),
                        LenguajeFuente.PIG_LATIN,
                        "MAIOR> FINIS;"
                ))
        );

        assertTrue(new CompiladorProyecto().compilar(proyecto).esValido());
    }

    @Test
    void rechazaProyectoSinArchivoPig() {
        ProyectoCompilacion proyecto = new ProyectoCompilacion(
                Path.of("proyecto"),
                List.of(new ArchivoFuente(
                        Path.of("proyecto", "Funciones.y"),
                        LenguajeFuente.Y,
                        "%funciones"
                ))
        );

        assertFalse(new CompiladorProyecto().compilar(proyecto).esValido());
    }
}

