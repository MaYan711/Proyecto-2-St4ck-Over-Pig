package com.compi2.contacto.analisis;

import com.compi2.contacto.ast.BinariaAst;
import com.compi2.contacto.ast.DeclaracionVariableAst;
import com.compi2.contacto.ast.EstructuraAst;
import com.compi2.contacto.ast.FuncionAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.RetornarAst;
import com.compi2.contacto.ast.SiAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YAstBuilderTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void construyeAstDeEstructuraYFuncion() {
        String codigo = """
                %estructuras
                estructura Persona:
                    cadena nombre
                    entero edad
                %funciones
                definir calcular(entero a, entero b) -> entero:
                    entero total = a + b * 2
                    retornar total
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(codigo);

        assertTrue(resultado.esValido());
        assertTrue(resultado.tieneAst());

        ProgramaAst programa =
                resultado.ast().orElseThrow();

        assertEquals(
                LenguajeFuente.Y,
                programa.lenguaje()
        );

        assertEquals(
                2,
                programa.elementos().size()
        );

        EstructuraAst estructura =
                assertInstanceOf(
                        EstructuraAst.class,
                        programa.elementos().get(0)
                );

        assertEquals(
                "Persona",
                estructura.nombre()
        );

        assertEquals(
                2,
                estructura.atributos().size()
        );

        FuncionAst funcion =
                assertInstanceOf(
                        FuncionAst.class,
                        programa.elementos().get(1)
                );

        assertEquals(
                "calcular",
                funcion.nombre()
        );

        assertEquals(
                2,
                funcion.parametros().size()
        );

        assertTrue(
                funcion.tieneRetorno()
        );

        assertEquals(
                "entero",
                funcion.retorno()
                        .orElseThrow()
                        .nombre()
        );
    }

    @Test
    void conservaPrecedenciaDeExpresiones() {
        String codigo = """
                %funciones
                definir calcular(entero a, entero b) -> entero:
                    entero total = a + b * 2
                    retornar total
                """;

        ProgramaAst programa =
                analizar(codigo)
                        .ast()
                        .orElseThrow();

        FuncionAst funcion =
                assertInstanceOf(
                        FuncionAst.class,
                        programa.elementos().get(0)
                );

        DeclaracionVariableAst declaracion =
                assertInstanceOf(
                        DeclaracionVariableAst.class,
                        funcion.cuerpo().get(0)
                );

        BinariaAst suma =
                assertInstanceOf(
                        BinariaAst.class,
                        declaracion.inicializador()
                                .orElseThrow()
                );

        assertEquals(
                BinariaAst.Operador.SUMA,
                suma.operador()
        );

        BinariaAst multiplicacion =
                assertInstanceOf(
                        BinariaAst.class,
                        suma.derecha()
                );

        assertEquals(
                BinariaAst.Operador.MULTIPLICACION,
                multiplicacion.operador()
        );
    }

    @Test
    void construyeCondicionalYRetorno() {
        String codigo = """
                %funciones
                definir clasificar(entero edad) -> entero:
                    si(edad > 18) entonces
                        imprimir("mayor")
                    sino (edad == 18) entonces
                        imprimir("igual")
                    contrario
                        imprimir("menor")
                    retornar edad
                """;

        ProgramaAst programa =
                analizar(codigo)
                        .ast()
                        .orElseThrow();

        FuncionAst funcion =
                assertInstanceOf(
                        FuncionAst.class,
                        programa.elementos().get(0)
                );

        assertEquals(
                2,
                funcion.cuerpo().size()
        );

        SiAst condicion =
                assertInstanceOf(
                        SiAst.class,
                        funcion.cuerpo().get(0)
                );

        assertEquals(
                2,
                condicion.ramas().size()
        );

        assertFalse(
                condicion.contrario().isEmpty()
        );

        assertInstanceOf(
                RetornarAst.class,
                funcion.cuerpo().get(1)
        );
    }

    @Test
    void noConstruyeAstCuandoHayErrorSintactico() {
        String codigo = """
                %funciones
                definir prueba():
                imprimir("hola")
                """;

        ResultadoAnalisisArchivo resultado =
                analizar(codigo);

        assertFalse(resultado.esValido());
        assertFalse(resultado.tieneAst());
    }

    private ResultadoAnalisisArchivo analizar(
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Funciones.y"),
                        LenguajeFuente.Y,
                        codigo
                );

        return analizador.analizar(
                archivo
        );
    }
}