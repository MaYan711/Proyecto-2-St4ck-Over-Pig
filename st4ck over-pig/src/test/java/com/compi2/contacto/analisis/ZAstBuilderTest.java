package com.compi2.contacto.analisis;

import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZAstBuilderTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void construyeClaseAtributosConstructoresYMetodos() {
        String codigo = """
                public class Persona {

                    String nombre;
                    int edad;

                    public Persona(String nombreParametro, int edadParametro) {
                        nombre = nombreParametro;
                        edad = edadParametro;
                    }

                    public Persona() {
                        nombre = "Sin nombre";
                        edad = 0;
                    }

                    public void saludar() {
                        println("Hola");
                    }

                    public int obtenerEdad() {
                        return edad;
                    }
                }
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(
                        "Persona.z",
                        codigo
                );

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
                LenguajeFuente.ZETARIANO,
                programa.lenguaje()
        );

        assertEquals(
                1,
                programa.elementos().size()
        );

        ZAst.Clase clase =
                assertInstanceOf(
                        ZAst.Clase.class,
                        programa.elementos()
                                .get(0)
                );

        assertEquals(
                "Persona",
                clase.nombre()
        );

        /*
         * 2 atributos
         * 2 constructores
         * 2 metodos
         */
        assertEquals(
                6,
                clase.miembros().size()
        );

        long atributos =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Atributo.class::isInstance
                        )
                        .count();

        long constructores =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Constructor.class::isInstance
                        )
                        .count();

        long metodos =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Metodo.class::isInstance
                        )
                        .count();

        assertEquals(
                2,
                atributos
        );

        assertEquals(
                2,
                constructores
        );

        assertEquals(
                2,
                metodos
        );
    }

    @Test
    void conservaPrecedenciaDeExpresiones() {
        String codigo = """
                public class Operaciones {

                    public int calcular(
                        int a,
                        int b
                    ) {
                        int resultado =
                            a + b * 2;

                        return resultado;
                    }
                }
                """;

        ProgramaAst programa =
                analizar(
                        "Operaciones.z",
                        codigo
                ).ast().orElseThrow();

        ZAst.Clase clase =
                assertInstanceOf(
                        ZAst.Clase.class,
                        programa.elementos()
                                .get(0)
                );

        ZAst.Metodo metodo =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Metodo.class::isInstance
                        )
                        .map(
                                ZAst.Metodo.class::cast
                        )
                        .findFirst()
                        .orElseThrow();

        ZAst.Declaracion declaracion =
                assertInstanceOf(
                        ZAst.Declaracion.class,
                        metodo.cuerpo()
                                .sentencias()
                                .get(0)
                );

        ZAst.Binaria suma =
                assertInstanceOf(
                        ZAst.Binaria.class,
                        declaracion.inicializador()
                                .orElseThrow()
                );

        assertEquals(
                ZAst.OperadorBinario.SUMA,
                suma.operador()
        );

        ZAst.Binaria multiplicacion =
                assertInstanceOf(
                        ZAst.Binaria.class,
                        suma.derecha()
                );

        assertEquals(
                ZAst.OperadorBinario.MULTIPLICACION,
                multiplicacion.operador()
        );
    }

    @Test
    void construyeNewAccesoMetodoYArreglo() {
        String codigo = """
                public class Objetos {

                    public void prueba() {

                        Persona persona =
                            new Persona("Mario", 20);

                        int[] numeros =
                            new int[5];

                        numeros[0] = 10;

                        int edad =
                            persona.obtenerEdad();
                    }
                }
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(
                        "Objetos.z",
                        codigo
                );

        assertTrue(
                resultado.esValido()
        );

        ZAst.Clase clase =
                assertInstanceOf(
                        ZAst.Clase.class,
                        resultado.ast()
                                .orElseThrow()
                                .elementos()
                                .get(0)
                );

        ZAst.Metodo metodo =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Metodo.class::isInstance
                        )
                        .map(
                                ZAst.Metodo.class::cast
                        )
                        .findFirst()
                        .orElseThrow();

        ZAst.Declaracion persona =
                assertInstanceOf(
                        ZAst.Declaracion.class,
                        metodo.cuerpo()
                                .sentencias()
                                .get(0)
                );

        assertInstanceOf(
                ZAst.NuevoObjeto.class,
                persona.inicializador()
                        .orElseThrow()
        );

        ZAst.Declaracion numeros =
                assertInstanceOf(
                        ZAst.Declaracion.class,
                        metodo.cuerpo()
                                .sentencias()
                                .get(1)
                );

        assertTrue(
                numeros.tipo()
                        .esArreglo()
        );

        assertInstanceOf(
                ZAst.NuevoArreglo.class,
                numeros.inicializador()
                        .orElseThrow()
        );
    }

    @Test
    void construyeTernario() {
        String codigo = """
                public class Ternarios {

                    public void prueba() {

                        int edad = 20;

                        String mensaje =
                            edad >= 18
                                ? "Mayor"
                                : "Menor";
                    }
                }
                """;

        ZAst.Clase clase =
                assertInstanceOf(
                        ZAst.Clase.class,
                        analizar(
                                "Ternarios.z",
                                codigo
                        ).ast()
                                .orElseThrow()
                                .elementos()
                                .get(0)
                );

        ZAst.Metodo metodo =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Metodo.class::isInstance
                        )
                        .map(
                                ZAst.Metodo.class::cast
                        )
                        .findFirst()
                        .orElseThrow();

        ZAst.Declaracion mensaje =
                assertInstanceOf(
                        ZAst.Declaracion.class,
                        metodo.cuerpo()
                                .sentencias()
                                .get(1)
                );

        assertInstanceOf(
                ZAst.Ternaria.class,
                mensaje.inicializador()
                        .orElseThrow()
        );
    }

    private ResultadoAnalisisArchivo analizar(
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