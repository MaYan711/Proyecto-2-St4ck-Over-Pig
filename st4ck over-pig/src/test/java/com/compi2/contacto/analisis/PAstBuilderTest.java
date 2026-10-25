package com.compi2.contacto.analisis;

import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PAstBuilderTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void construyeProgramaPrincipal() {
        String codigo = """
                MAIOR>
                >> "Hola";
                FINIS;
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(codigo);

        assertTrue(
                resultado.esValido()
        );

        assertTrue(
                resultado.tieneAst()
        );

        ProgramaAst programa =
                resultado.ast()
                        .orElseThrow();

        assertEquals(
                LenguajeFuente.PIG_LATIN,
                programa.lenguaje()
        );

        PAst.Programa pig =
                assertInstanceOf(
                        PAst.Programa.class,
                        programa.elementos()
                                .get(0)
                );

        assertEquals(
                1,
                pig.principal().size()
        );

        assertInstanceOf(
                PAst.Salida.class,
                pig.principal().get(0)
        );
    }

    @Test
    void construyeImportacionesYGlobales() {
        String codigo = """
                import modelos.Persona.z
                import datos.Estructuras.y

                VARIABILES>
                esto edad : numerus 20;
                esto activo : verum;
                esto persona : novus Persona();
                esto direccion : Direccion {"Centro", 10};
                series numeros[3] : numerus {1, 2, 3};

                MAIOR>
                >> edad;
                FINIS;
                """;

        PAst.Programa pig =
                raiz(codigo);

        assertEquals(
                2,
                pig.importaciones().size()
        );

        assertEquals(
                5,
                pig.globales().size()
        );

        assertEquals(
                "z",
                pig.importaciones()
                        .get(0)
                        .extension()
        );

        assertEquals(
                "y",
                pig.importaciones()
                        .get(1)
                        .extension()
        );

        assertTrue(
                pig.globales()
                        .get(4)
                        .esArreglo()
        );
    }

    @Test
    void conservaPrecedenciaAritmetica() {
        String codigo = """
                VARIABILES>
                esto a : numerus 10;
                esto b : numerus 20;
                esto resultado : numerus 0;

                MAIOR>
                resultado = a + b * 2;
                FINIS;
                """;

        PAst.Programa pig =
                raiz(codigo);

        PAst.ExpresionSentencia sentencia =
                assertInstanceOf(
                        PAst.ExpresionSentencia.class,
                        pig.principal()
                                .get(0)
                );

        PAst.Asignacion asignacion =
                assertInstanceOf(
                        PAst.Asignacion.class,
                        sentencia.expresion()
                );

        PAst.Binaria suma =
                assertInstanceOf(
                        PAst.Binaria.class,
                        asignacion.valor()
                );

        assertEquals(
                PAst.OperadorBinario.SUMA,
                suma.operador()
        );

        PAst.Binaria multiplicacion =
                assertInstanceOf(
                        PAst.Binaria.class,
                        suma.derecha()
                );

        assertEquals(
                PAst.OperadorBinario.MULTIPLICACION,
                multiplicacion.operador()
        );
    }

    @Test
    void construyeObjetoYAccesoEncadenado() {
        String codigo = """
                import modelos.Persona.z

                VARIABILES>
                esto persona : novus Persona();

                MAIOR>
                persona.apellidos[0].getNombre();
                FINIS;
                """;

        PAst.Programa pig =
                raiz(codigo);

        assertInstanceOf(
                PAst.NuevoObjeto.class,
                pig.globales()
                        .get(0)
                        .inicializador()
                        .orElseThrow()
        );

        PAst.ExpresionSentencia sentencia =
                assertInstanceOf(
                        PAst.ExpresionSentencia.class,
                        pig.principal()
                                .get(0)
                );

        assertInstanceOf(
                PAst.Llamada.class,
                sentencia.expresion()
        );
    }

    @Test
    void construyeIfWhileYFor() {
        String codigo = """
                VARIABILES>
                esto x : numerus 0;

                MAIOR>
                si (x < 10) {
                    x++;
                } aliter {
                    x = 0;
                } finis;

                dum (x < 100) {
                    x++;
                } finis;

                per (esto i : numerus 0; i < 10; i++) {
                    >> i;
                }

                FINIS;
                """;

        PAst.Programa pig =
                raiz(codigo);

        assertEquals(
                3,
                pig.principal().size()
        );

        assertInstanceOf(
                PAst.Si.class,
                pig.principal().get(0)
        );

        assertInstanceOf(
                PAst.Mientras.class,
                pig.principal().get(1)
        );

        assertInstanceOf(
                PAst.Para.class,
                pig.principal().get(2)
        );
    }

    private PAst.Programa raiz(
            String codigo
    ) {
        ResultadoAnalisisArchivo resultado =
                analizar(codigo);

        assertTrue(
                resultado.esValido()
        );

        return assertInstanceOf(
                PAst.Programa.class,
                resultado.ast()
                        .orElseThrow()
                        .elementos()
                        .get(0)
        );
    }

    private ResultadoAnalisisArchivo analizar(
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Main.pig"),
                        LenguajeFuente.PIG_LATIN,
                        codigo
                );

        return analizador.analizar(
                archivo
        );
    }
}