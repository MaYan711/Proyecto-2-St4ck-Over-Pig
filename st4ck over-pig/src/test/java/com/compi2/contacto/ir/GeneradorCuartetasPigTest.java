package com.compi2.contacto.ir;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorCuartetasPigTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void generaMaiorDeclaracionesYSalida() {
        String codigo = """
                VARIABILES>
                esto edad : numerus 20;
                esto nombre : textum "Mario";

                MAIOR>
                >> nombre;
                >> edad;
                FINIS;
                """;

        ProgramaIntermedio programa =
                generar(
                        codigo
                );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.INICIO_FUNCION
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.DECLARAR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.ASIGNAR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.IMPRIMIR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.FIN_FUNCION
                )
        );
    }

    @Test
    void generaExpresionesYCondicionales() {
        String codigo = """
                VARIABILES>
                esto a : numerus 10;
                esto b : numerus 20;
                esto resultado : numerus 0;

                MAIOR>
                resultado = a + b * 2;

                si (resultado > 20) {
                    >> resultado;
                } aliter {
                    >> 0;
                } finis;

                FINIS;
                """;

        ProgramaIntermedio programa =
                generar(
                        codigo
                );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.MULTIPLICAR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.SUMAR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.MAYOR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.SALTAR_SI_FALSO
                )
        );
    }

    @Test
    void generaWhileDoWhileYFor() {
        String codigo = """
                VARIABILES>
                esto x : numerus 0;

                MAIOR>

                dum (x < 5) {
                    x++;
                } finis;

                facere {
                    x--;
                } dum (x > 0);

                per (esto i : numerus 0; i < 5; i++) {
                    si (i == 2) {
                        perge;
                    } finis;

                    si (i == 4) {
                        interrumpe;
                    } finis;
                }

                FINIS;
                """;

        ProgramaIntermedio programa =
                generar(
                        codigo
                );

        long etiquetas =
                programa.cuartetas()
                        .stream()
                        .filter(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ETIQUETA
                        )
                        .count();

        assertTrue(
                etiquetas >= 6
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.SALTAR
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.SALTAR_SI_FALSO
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.SALTAR_SI_VERDADERO
                )
        );
    }

    @Test
    void generaEntrada() {
        String codigo = """
                VARIABILES>
                esto nombre : textum "Inicial";

                MAIOR>
                nombre <<;
                >> nombre;
                FINIS;
                """;

        ProgramaIntermedio programa =
                generar(
                        codigo
                );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.LEER
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.ASIGNAR
                )
        );
    }

    @Test
    void generaCreacionObjetoYLlamadas() {
        String codigo = """
                VARIABILES>
                esto persona : novus Persona("Mario", 20);
                esto resultado : numerus 0;

                MAIOR>
                resultado = sumar(10, 20);
                resultado = persona.obtenerEdad();
                FINIS;
                """;

        ProgramaIntermedio programa =
                generar(
                        codigo
                );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.NUEVO_OBJETO
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.PARAMETRO
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.LLAMAR
                )
        );
    }

    @Test
    void generaArreglosYAccesos() {
        String codigo = """
                VARIABILES>
                series numeros[3] : numerus {1, 2, 3};

                MAIOR>
                numeros[0] = 10;
                >> numeros[0];
                FINIS;
                """;

        ProgramaIntermedio programa =
                generar(
                        codigo
                );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.INICIALIZAR_COMPUESTO
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.ASIGNAR
                )
        );
    }

    private ProgramaIntermedio generar(
            String codigo
    ) {
        ResultadoAnalisisArchivo analisis =
                analizador.analizar(
                        new ArchivoFuente(
                                Path.of(
                                        "Main.pig"
                                ),
                                LenguajeFuente.PIG_LATIN,
                                codigo
                        )
                );

        assertTrue(
                analisis.esValido()
        );

        ProgramaAst ast =
                analisis.ast()
                        .orElseThrow();

        ProgramaIntermedio programa =
                new GeneradorCuartetasPig()
                        .generar(
                                List.of(
                                        ast
                                )
                        );

        assertFalse(
                programa.cuartetas()
                        .isEmpty()
        );

        return programa;
    }

    private boolean contiene(
            ProgramaIntermedio programa,
            OperadorCuarteta operador
    ) {
        return programa.cuartetas()
                .stream()
                .anyMatch(
                        cuarteta ->
                                cuarteta.operador()
                                        == operador
                );
    }
}