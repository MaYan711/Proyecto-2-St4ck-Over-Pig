package com.compi2.contacto.analisis;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PigLatinComentarioHashTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaComentarioHashMultilineaComoEnElPdf() {
        String codigo = """
                ##
                Importaciones de los otros lenguajes
                Este texto no forma parte del programa
                ##

                ##
                Seccion de funcion principal
                Esta seccion es obligatoria
                ##
                MAIOR>
                >> "Hola";
                FINIS;
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(
                        codigo
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
    void ignoraTokensYCaracteresInvalidosDentroDeComentarioHash() {
        String codigo = """
                ## MAIOR> FINIS; << >> @ # public class Falsa ##
                MAIOR>
                ## 
                @@@ esto no debe producir error lexico
                si (esto tampoco es codigo) {
                    FINIS;
                }
                ##
                >> "OK";
                FINIS;
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(
                        codigo
                );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos()
                        .toString()
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
