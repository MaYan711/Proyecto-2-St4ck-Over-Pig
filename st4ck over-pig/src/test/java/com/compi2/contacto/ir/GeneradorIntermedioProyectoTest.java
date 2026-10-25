package com.compi2.contacto.ir;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorIntermedioProyectoTest {

    private static final Pattern TEMPORAL_SIN_PREFIJO =
            Pattern.compile("\\bt\\d+\\b");

    private static final Pattern ETIQUETA_SIN_PREFIJO =
            Pattern.compile("\\bL\\d+\\b");

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void uneLosTresProgramasIntermedios() {
        ProgramaAst y =
                analizar(
                        "Funciones.y",
                        LenguajeFuente.Y,
                        """
                        %funciones
                        definir sumar(entero a, entero b) -> entero:
                            entero resultado = a + b * 2
                            retornar resultado
                        """
                );

        ProgramaAst z =
                analizar(
                        "Persona.z",
                        LenguajeFuente.ZETARIANO,
                        """
                        public class Persona {

                            int edad;

                            public Persona(int valor) {
                                edad = valor;
                            }

                            public int doble() {
                                return edad * 2;
                            }
                        }
                        """
                );

        ProgramaAst pig =
                analizar(
                        "Main.pig",
                        LenguajeFuente.PIG_LATIN,
                        """
                        VARIABILES>
                        esto resultado : numerus 0;

                        MAIOR>
                        resultado = 10 + 20 * 2;
                        >> resultado;
                        FINIS;
                        """
                );

        ProgramaIntermedio intermedioY =
                new GeneradorCuartetasY()
                        .generar(
                                List.of(y)
                        );

        ProgramaIntermedio intermedioZ =
                new GeneradorCuartetasZ()
                        .generar(
                                List.of(z)
                        );

        ProgramaIntermedio intermedioPig =
                new GeneradorCuartetasPig()
                        .generar(
                                List.of(pig)
                        );

        ProgramaIntermedio unificado =
                new GeneradorIntermedioProyecto()
                        .generar(
                                List.of(y),
                                List.of(z),
                                List.of(pig)
                        );

        assertEquals(
                intermedioY.cuartetas().size()
                        + intermedioZ.cuartetas().size()
                        + intermedioPig.cuartetas().size(),
                unificado.cuartetas().size()
        );

        assertFalse(
                unificado.cuartetas()
                        .isEmpty()
        );
    }

    @Test
    void renombraTemporalesDeCadaLenguaje() {
        ProgramaAst y =
                analizar(
                        "Funciones.y",
                        LenguajeFuente.Y,
                        """
                        %funciones
                        definir calcular(entero a, entero b) -> entero:
                            retornar a + b * 2
                        """
                );

        ProgramaAst z =
                analizar(
                        "Calculadora.z",
                        LenguajeFuente.ZETARIANO,
                        """
                        public class Calculadora {

                            public int calcular(int a, int b) {
                                return a + b * 2;
                            }
                        }
                        """
                );

        ProgramaAst pig =
                analizar(
                        "Main.pig",
                        LenguajeFuente.PIG_LATIN,
                        """
                        MAIOR>
                        esto resultado : numerus 0;
                        resultado = 10 + 20 * 2;
                        FINIS;
                        """
                );

        ProgramaIntermedio programa =
                new GeneradorIntermedioProyecto()
                        .generar(
                                List.of(y),
                                List.of(z),
                                List.of(pig)
                        );

        String texto =
                texto(
                        programa
                );

        assertTrue(
                texto.contains(
                        "y_t"
                )
        );

        assertTrue(
                texto.contains(
                        "z_t"
                )
        );

        assertTrue(
                texto.contains(
                        "pig_t"
                )
        );

        assertFalse(
                TEMPORAL_SIN_PREFIJO
                        .matcher(
                                texto
                        )
                        .find()
        );
    }

    @Test
    void renombraEtiquetasDeControl() {
        ProgramaAst y =
                analizar(
                        "Funciones.y",
                        LenguajeFuente.Y,
                        """
                        %funciones
                        definir prueba(entero x):
                            mientras(x < 10) hacer
                                x++
                        """
                );

        ProgramaAst z =
                analizar(
                        "Control.z",
                        LenguajeFuente.ZETARIANO,
                        """
                        public class Control {

                            public void prueba() {

                                int x = 0;

                                while (x < 10) {
                                    x++;
                                }
                            }
                        }
                        """
                );

        ProgramaAst pig =
                analizar(
                        "Main.pig",
                        LenguajeFuente.PIG_LATIN,
                        """
                        VARIABILES>
                        esto x : numerus 0;

                        MAIOR>
                        dum (x < 10) {
                            x++;
                        } finis;
                        FINIS;
                        """
                );

        ProgramaIntermedio programa =
                new GeneradorIntermedioProyecto()
                        .generar(
                                List.of(y),
                                List.of(z),
                                List.of(pig)
                        );

        String texto =
                texto(
                        programa
                );

        assertTrue(
                texto.contains(
                        "y_L"
                )
        );

        assertTrue(
                texto.contains(
                        "z_L"
                )
        );

        assertTrue(
                texto.contains(
                        "pig_L"
                )
        );

        assertFalse(
                ETIQUETA_SIN_PREFIJO
                        .matcher(
                                texto
                        )
                        .find()
        );
    }

    private ProgramaAst analizar(
            String nombre,
            LenguajeFuente lenguaje,
            String codigo
    ) {
        ResultadoAnalisisArchivo resultado =
                analizador.analizar(
                        new ArchivoFuente(
                                Path.of(
                                        nombre
                                ),
                                lenguaje,
                                codigo
                        )
                );

        assertTrue(
                resultado.esValido()
        );

        return resultado.ast()
                .orElseThrow();
    }

    private String texto(
            ProgramaIntermedio programa
    ) {
        return programa.cuartetas()
                .stream()
                .flatMap(
                        cuarteta ->
                                Stream.of(
                                        cuarteta.argumento1(),
                                        cuarteta.argumento2(),
                                        cuarteta.resultado()
                                )
                )
                .filter(
                        valor ->
                                valor != null
                )
                .reduce(
                        "",
                        (actual, siguiente) ->
                                actual
                                        + "\n"
                                        + siguiente
                );
    }
}