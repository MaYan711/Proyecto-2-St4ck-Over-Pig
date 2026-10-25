package com.compi2.contacto.analisis;

import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PigLatinEntradaSintaxisTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaEntradaConDestinoALaIzquierda() {
        ResultadoAnalisisArchivo resultado =
                analizar(
                        """
                        VARIABILES>
                        esto texto : textum "";

                        MAIOR>
                        texto <<;
                        FINIS;
                        """
                );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos()
                        .toString()
        );
    }

    @Test
    void aceptaEntradaDescartadaConYSinPuntoYComa() {
        ResultadoAnalisisArchivo resultado =
                analizar(
                        """
                        MAIOR>
                        <<;
                        <<
                        >> "OK";
                        FINIS;
                        """
                );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos()
                        .toString()
        );
    }

    @Test
    void rechazaDestinoALaDerechaDeLeer() {
        ResultadoAnalisisArchivo resultado =
                analizar(
                        """
                        VARIABILES>
                        esto texto : textum "";

                        MAIOR>
                        << texto;
                        FINIS;
                        """
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
                                                == TipoDiagnostico.SINTACTICO
                                                && diagnostico.mensaje()
                                                .contains(
                                                        "variable <<"
                                                )
                        )
        );
    }

    private ResultadoAnalisisArchivo analizar(
            String codigo
    ) {
        return analizador.analizar(
                new ArchivoFuente(
                        Path.of(
                                "main.pig"
                        ),
                        LenguajeFuente.PIG_LATIN,
                        codigo
                )
        );
    }
}
