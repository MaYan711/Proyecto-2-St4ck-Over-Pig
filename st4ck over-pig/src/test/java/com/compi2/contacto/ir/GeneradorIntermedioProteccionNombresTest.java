package com.compi2.contacto.ir;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorIntermedioProteccionNombresTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void noRenombraTemporalesNiEtiquetasDentroDeLiterales() {
        ProgramaIntermedio programa =
                generarPig(
                        """
                        MAIOR>
                        >> "Resultado t1";
                        >> "Nivel L2";
                        >> "Texto t0 L0 t123 L456";
                        FINIS;
                        """
                );

        List<String> impresiones =
                programa.cuartetas()
                        .stream()
                        .filter(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.IMPRIMIR
                        )
                        .map(
                                Cuarteta::argumento1
                        )
                        .toList();

        assertEquals(
                List.of(
                        "\"Resultado t1\"",
                        "\"Nivel L2\"",
                        "\"Texto t0 L0 t123 L456\""
                ),
                impresiones
        );
    }

    @Test
    void separaIdentificadoresDeUsuarioDeTemporalesInternos() {
        ProgramaIntermedio programa =
                generarPig(
                        """
                        VARIABILES>
                        esto t0 : numerus 7;
                        esto L0 : numerus 9;
                        esto suma : numerus 0;

                        MAIOR>
                        suma = t0 + L0;
                        >> t0;
                        >> L0;
                        >> suma;
                        FINIS;
                        """
                );

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.DECLARAR
                                                && cuarteta.resultado()
                                                .equals(
                                                        "t0"
                                                )
                        )
        );

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.DECLARAR
                                                && cuarteta.resultado()
                                                .equals(
                                                        "L0"
                                                )
                        )
        );

        Cuarteta suma =
                programa.cuartetas()
                        .stream()
                        .filter(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.SUMAR
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "t0",
                suma.argumento1()
        );

        assertEquals(
                "L0",
                suma.argumento2()
        );

        assertTrue(
                suma.resultado()
                        .startsWith(
                                "pig_t"
                        )
        );
    }

    private ProgramaIntermedio generarPig(
            String codigo
    ) {
        ResultadoAnalisisArchivo analisis =
                analizador.analizar(
                        new ArchivoFuente(
                                Path.of(
                                        "main.pig"
                                ),
                                LenguajeFuente.PIG_LATIN,
                                codigo
                        )
                );

        assertTrue(
                analisis.esValido(),
                () -> analisis.diagnosticos()
                        .toString()
        );

        ProgramaAst pig =
                analisis.ast()
                        .orElseThrow();

        return new GeneradorIntermedioProyecto()
                .generar(
                        List.of(),
                        List.of(),
                        List.of(
                                pig
                        )
                );
    }
}
