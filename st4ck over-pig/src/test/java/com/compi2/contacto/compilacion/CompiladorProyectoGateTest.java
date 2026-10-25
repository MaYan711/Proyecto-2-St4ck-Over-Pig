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

class CompiladorProyectoGateTest {

    @Test
    void errorLexicoEnArchivoNoImportadoBloqueaGeneracion() {
        Path raiz = Path.of("proyecto");

        ProyectoCompilacion proyecto = new ProyectoCompilacion(
                raiz,
                List.of(
                        new ArchivoFuente(
                                raiz.resolve("main.pig"),
                                LenguajeFuente.PIG_LATIN,
                                "MAIOR>\nFINIS;"
                        ),
                        new ArchivoFuente(
                                raiz.resolve("Roto.y"),
                                LenguajeFuente.Y,
                                "%funciones\ndefinir rota():\n    @"
                        )
                )
        );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(proyecto);

        assertFalse(resultado.esValido());
        assertTrue(
                resultado.diagnosticos()
                        .stream()
                        .anyMatch(
                                diagnostico ->
                                        diagnostico.tipo()
                                                == TipoDiagnostico.LEXICO
                        )
        );
        assertTrue(
                resultado.programaIntermedio()
                        .cuartetas()
                        .isEmpty()
        );
    }

    @Test
    void errorSintacticoEnArchivoNoImportadoBloqueaGeneracion() {
        Path raiz = Path.of("proyecto");

        ProyectoCompilacion proyecto = new ProyectoCompilacion(
                raiz,
                List.of(
                        new ArchivoFuente(
                                raiz.resolve("main.pig"),
                                LenguajeFuente.PIG_LATIN,
                                "MAIOR>\nFINIS;"
                        ),
                        new ArchivoFuente(
                                raiz.resolve("Roto.y"),
                                LenguajeFuente.Y,
                                "%funciones\ndefinir rota():\n    entero valor ="
                        )
                )
        );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(proyecto);

        assertFalse(resultado.esValido());
        assertTrue(
                resultado.diagnosticos()
                        .stream()
                        .anyMatch(
                                diagnostico ->
                                        diagnostico.tipo()
                                                == TipoDiagnostico.SINTACTICO
                        )
        );
        assertTrue(
                resultado.programaIntermedio()
                        .cuartetas()
                        .isEmpty()
        );
    }
}
