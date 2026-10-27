package com.compi2.contacto.semantica;

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

class AnalizadorSemanticoZTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaClaseSemanticamenteValida() {
        String codigo = """
                public class Persona {

                    String nombre;
                    int edad;

                    public Persona(
                        String nombreParametro,
                        int edadParametro
                    ) {
                        nombre = nombreParametro;
                        edad = edadParametro;
                    }

                    public Persona() {
                        nombre = "Sin nombre";
                        edad = 0;
                    }

                    public int obtenerEdad() {
                        return edad;
                    }

                    public void saludar() {
                        println("Hola");
                    }
                }
                """;

        ResultadoSemanticoZ resultado =
                analizar(
                        "Persona.z",
                        codigo
                );

        assertTrue(
                resultado.esValido()
        );
    }

    @Test
    void detectaConstructorConNombreIncorrecto() {
        String codigo = """
                public class Persona {

                    public Humano() {
                    }
                }
                """;

        ResultadoSemanticoZ resultado =
                analizar(
                        "Persona.z",
                        codigo
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void aceptaSobrecargaYRechazaFirmaDuplicada() {
        String valido = """
                public class Calculadora {

                    public int sumar(
                        int a,
                        int b
                    ) {
                        return a + b;
                    }

                    public double sumar(
                        double a,
                        double b
                    ) {
                        return a + b;
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Calculadora.z",
                        valido
                ).esValido()
        );

        String invalido = """
                public class Calculadora {

                    public int sumar(
                        int a,
                        int b
                    ) {
                        return a + b;
                    }

                    public double sumar(
                        int x,
                        int y
                    ) {
                        return x + y;
                    }
                }
                """;

        assertFalse(
                analizar(
                        "Calculadora.z",
                        invalido
                ).esValido()
        );
    }

    @Test
    void detectaAsignacionDeTipoIncorrecto() {
        String codigo = """
                public class Prueba {

                    public void ejecutar() {
                        int edad = "Mario";
                    }
                }
                """;

        ResultadoSemanticoZ resultado =
                analizar(
                        "Prueba.z",
                        codigo
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaRetornoIncorrecto() {
        String codigo = """
                public class Prueba {

                    public int obtenerEdad() {
                        return "veinte";
                    }
                }
                """;

        assertFalse(
                analizar(
                        "Prueba.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void detectaVariableNoDeclarada() {
        String codigo = """
                public class Prueba {

                    public void ejecutar() {
                        resultado = 10;
                    }
                }
                """;

        assertFalse(
                analizar(
                        "Prueba.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void detectaBreakYContinueFueraDeCiclo() {
        String codigo = """
                public class Prueba {

                    public void ejecutar() {
                        break;
                        continue;
                    }
                }
                """;

        assertFalse(
                analizar(
                        "Prueba.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaArreglosObjetosYLlamadas() {
        String persona = """
                public class Persona {

                    int edad;

                    public Persona(int edadParametro) {
                        edad = edadParametro;
                    }

                    public int obtenerEdad() {
                        return edad;
                    }
                }
                """;

        String uso = """
                public class Uso {

                    public void ejecutar() {

                        Persona persona =
                            new Persona(20);

                        int edad =
                            persona.obtenerEdad();

                        int[] numeros =
                            new int[5];

                        numeros[0] = edad;

                        println(
                            numeros[0]
                        );
                    }
                }
                """;

        ResultadoAnalisisArchivo analisisPersona =
                parsear(
                        "Persona.z",
                        persona
                );

        ResultadoAnalisisArchivo analisisUso =
                parsear(
                        "Uso.z",
                        uso
                );

        assertTrue(
                analisisPersona.esValido()
        );

        assertTrue(
                analisisUso.esValido()
        );

        ResultadoSemanticoZ resultado =
                new AnalizadorSemanticoZ()
                        .analizar(
                                List.of(
                                        analisisPersona.ast()
                                                .orElseThrow(),

                                        analisisUso.ast()
                                                .orElseThrow()
                                )
                        );

        assertTrue(
                resultado.esValido()
        );
    }

    @Test
    void detectaConstructorInexistente() {
        String persona = """
                public class Persona {

                    public Persona(int edad) {
                    }
                }
                """;

        String uso = """
                public class Uso {

                    public void ejecutar() {
                        Persona p =
                            new Persona("Mario");
                    }
                }
                """;

        ResultadoAnalisisArchivo a =
                parsear(
                        "Persona.z",
                        persona
                );

        ResultadoAnalisisArchivo b =
                parsear(
                        "Uso.z",
                        uso
                );

        ResultadoSemanticoZ resultado =
                new AnalizadorSemanticoZ()
                        .analizar(
                                List.of(
                                        a.ast().orElseThrow(),
                                        b.ast().orElseThrow()
                                )
                        );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaCondicionNoBooleana() {
        String codigo = """
                public class Prueba {

                    public void ejecutar() {

                        int edad = 20;

                        if (edad) {
                            println("Hola");
                        }
                    }
                }
                """;

        assertFalse(
                analizar(
                        "Prueba.z",
                        codigo
                ).esValido()
        );
    }

    private ResultadoSemanticoZ analizar(
            String nombre,
            String codigo
    ) {
        ResultadoAnalisisArchivo analisis =
                parsear(
                        nombre,
                        codigo
                );

        assertTrue(
                analisis.esValido(),
                "El codigo debe pasar primero el analisis lexico y sintactico"
        );

        ProgramaAst ast =
                analisis.ast()
                        .orElseThrow();

        return new AnalizadorSemanticoZ()
                .analizar(
                        List.of(ast)
                );
    }

    private ResultadoAnalisisArchivo parsear(
            String nombre,
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of(nombre),
                        LenguajeFuente.ZETARIANO,
                        codigo
                );

        return analizador.analizar(
                archivo
        );
    }
}