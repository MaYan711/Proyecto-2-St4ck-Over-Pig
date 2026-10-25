package com.compi2.contacto.compilacion;

import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiladorProyectoUnicoPigTest {

    @Test
    void aceptaExactamenteUnArchivoPig() {
        ProyectoCompilacion proyecto =
                new ProyectoCompilacion(
                        Path.of("proyecto"),
                        List.of(
                                archivoPig(
                                        "main.pig",
                                        """
                                        MAIOR>
                                        >> "OK";
                                        FINIS;
                                        """
                                )
                        )
                );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(
                                proyecto
                        );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos()
                        .toString()
        );

        assertFalse(
                resultado.programaIntermedio()
                        .cuartetas()
                        .isEmpty()
        );
    }

    @Test
    void rechazaProyectoSinArchivoPig() {
        ProyectoCompilacion proyecto =
                new ProyectoCompilacion(
                        Path.of("proyecto"),
                        List.of(
                                new ArchivoFuente(
                                        Path.of(
                                                "proyecto",
                                                "funciones.y"
                                        ),
                                        LenguajeFuente.Y,
                                        "%funciones"
                                )
                        )
                );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(
                                proyecto
                        );

        assertFalse(
                resultado.esValido()
        );

        assertTrue(
                resultado.diagnosticos()
                        .stream()
                        .anyMatch(
                                diagnostico ->
                                        diagnostico.tipo()
                                                == TipoDiagnostico.PROYECTO
                                                && diagnostico.mensaje()
                                                .contains(
                                                        "exactamente un archivo principal .pig"
                                                )
                        )
        );

        assertTrue(
                resultado.programaIntermedio()
                        .cuartetas()
                        .isEmpty()
        );
    }

    @Test
    void rechazaDosArchivosPigYNoGeneraBackend() {
        ProyectoCompilacion proyecto =
                new ProyectoCompilacion(
                        Path.of("proyecto"),
                        List.of(
                                archivoPig(
                                        "main1.pig",
                                        """
                                        MAIOR>
                                        >> "UNO";
                                        FINIS;
                                        """
                                ),
                                archivoPig(
                                        "main2.pig",
                                        """
                                        MAIOR>
                                        >> "DOS";
                                        FINIS;
                                        """
                                )
                        )
                );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(
                                proyecto
                        );

        assertFalse(
                resultado.esValido()
        );

        assertTrue(
                resultado.diagnosticos()
                        .stream()
                        .anyMatch(
                                diagnostico ->
                                        diagnostico.tipo()
                                                == TipoDiagnostico.PROYECTO
                                                && diagnostico.mensaje()
                                                .contains(
                                                        "se encontraron 2"
                                                )
                        )
        );

        assertTrue(
                resultado.programaIntermedio()
                        .cuartetas()
                        .isEmpty()
        );

        assertTrue(
                resultado.programaMemoria()
                        .cuartetas()
                        .isEmpty()
        );
    }

    private ArchivoFuente archivoPig(
            String nombre,
            String contenido
    ) {
        return new ArchivoFuente(
                Path.of(
                        "proyecto",
                        nombre
                ),
                LenguajeFuente.PIG_LATIN,
                contenido
        );
    }
}
