package com.compi2.contacto.analisis;

import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalizadorArchivoNombreClaseZTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void usaNombreDelAstYNoElDeUnComentario() {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Real.z"),
                        LenguajeFuente.ZETARIANO,
                        """
                        // public class Falsa
                        public class Real {
                            public Real() {
                            }
                        }
                        """
                );

        ResultadoAnalisisArchivo resultado =
                analizador.analizar(
                        archivo
                );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos()
                        .toString()
        );

        assertTrue(
                resultado.tieneAst()
        );
    }

    @Test
    void reportaNombreRealDeLaClaseObtenidoDelAst() {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Falsa.z"),
                        LenguajeFuente.ZETARIANO,
                        """
                        // public class Falsa
                        public class Real {
                            public Real() {
                            }
                        }
                        """
                );

        ResultadoAnalisisArchivo resultado =
                analizador.analizar(
                        archivo
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
                                                == TipoDiagnostico.SEMANTICO
                                                && diagnostico.mensaje()
                                                .contains(
                                                        "Real.z"
                                                )
                        )
        );

        assertEquals(
                2,
                resultado.diagnosticos()
                        .stream()
                        .filter(
                                diagnostico ->
                                        diagnostico.tipo()
                                                == TipoDiagnostico.SEMANTICO
                        )
                        .findFirst()
                        .orElseThrow()
                        .linea()
        );
    }
}
