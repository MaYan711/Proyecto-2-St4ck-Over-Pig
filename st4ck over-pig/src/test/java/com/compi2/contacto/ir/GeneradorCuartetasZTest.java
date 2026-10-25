package com.compi2.contacto.ir;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.semantica.AnalizadorSemanticoZ;
import com.compi2.contacto.semantica.ResultadoSemanticoZ;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorCuartetasZTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void generaConstructorYMetodo() {
        String codigo = """
                public class Persona {

                    int edad;

                    public Persona(int valor) {
                        edad = valor;
                    }

                    public int obtenerEdad() {
                        return edad;
                    }
                }
                """;

        ProgramaIntermedio programa =
                generar(
                        "Persona.z",
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
                        OperadorCuarteta.DECLARAR_PARAMETRO
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
                        OperadorCuarteta.RETORNAR
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
    void generaAritmeticaIfYSalida() {
        String codigo = """
                public class Operaciones {

                    public int calcular(
                        int a,
                        int b
                    ) {
                        int total =
                            a + b * 2;

                        if (total > 10) {
                            println(total);
                        }

                        return total;
                    }
                }
                """;

        ProgramaIntermedio programa =
                generar(
                        "Operaciones.z",
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

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.IMPRIMIR
                )
        );
    }

    @Test
    void generaObjetosArreglosYTernario() {
        String codigo = """
                public class Persona {

                    int edad;

                    public Persona(int edadParametro) {
                        edad = edadParametro;
                    }

                    public void prueba() {

                        Persona persona =
                            new Persona(20);

                        int[] numeros =
                            new int[5];

                        int resultado =
                            edad >= 18
                                ? 1
                                : 0;

                        numeros[0] = resultado;
                    }
                }
                """;

        ProgramaIntermedio programa =
                generar(
                        "Persona.z",
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
                        OperadorCuarteta.NUEVO_ARREGLO
                )
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.MAYOR_IGUAL
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
    void generaForWhileDoWhileYSwitch() {
        String codigo = """
                public class Control {

                    public void prueba() {

                        int i = 0;

                        for (
                            i = 0;
                            i < 5;
                            i++
                        ) {
                            if (i == 2) {
                                continue;
                            }
                        }

                        while (i < 10) {
                            i++;

                            if (i == 8) {
                                break;
                            }
                        }

                        do {
                            i--;
                        } while (i > 0);

                        switch (i) {
                            case 0:
                                println("cero");
                                break;

                            case 1:
                                println("uno");
                                break;

                            default:
                                println("otro");
                                break;
                        }
                    }
                }
                """;

        ProgramaIntermedio programa =
                generar(
                        "Control.z",
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
    void generaLlamadaAMetodo() {
        String codigo = """
                public class Calculadora {

                    public int sumar(
                        int a,
                        int b
                    ) {
                        return a + b;
                    }

                    public void prueba() {
                        int resultado =
                            sumar(10, 20);

                        println(resultado);
                    }
                }
                """;

        ProgramaIntermedio programa =
                generar(
                        "Calculadora.z",
                        codigo
                );

        long parametros =
                programa.cuartetas()
                        .stream()
                        .filter(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.PARAMETRO
                        )
                        .count();

        assertTrue(
                parametros >= 2
        );

        assertTrue(
                contiene(
                        programa,
                        OperadorCuarteta.LLAMAR
                )
        );
    }

    private ProgramaIntermedio generar(
            String nombre,
            String codigo
    ) {
        ResultadoAnalisisArchivo analisis =
                analizador.analizar(
                        new ArchivoFuente(
                                Path.of(
                                        nombre
                                ),
                                LenguajeFuente.ZETARIANO,
                                codigo
                        )
                );

        assertTrue(
                analisis.esValido()
        );

        ProgramaAst ast =
                analisis.ast()
                        .orElseThrow();

        ResultadoSemanticoZ semantica =
                new AnalizadorSemanticoZ()
                        .analizar(
                                List.of(
                                        ast
                                )
                        );

        assertTrue(
                semantica.esValido()
        );

        ProgramaIntermedio programa =
                new GeneradorCuartetasZ()
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