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

class AnalizadorSemanticoYTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaProgramaSemanticamenteValido() {
        String codigo = """
                %estructuras
                estructura Persona:
                    cadena nombre
                    entero edad
                    entero notas[3]
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    entero resultado = a + b
                    retornar resultado
                definir prueba({} Persona persona):
                    entero edad = persona.edad
                    entero resultado = sumar(edad, 10)
                    si(resultado > 10) entonces
                        imprimir(persona.nombre)
                """;

        ResultadoSemanticoY resultado =
                analizarSemantica(codigo);

        assertTrue(
                resultado.esValido()
        );
    }

    @Test
    void detectaVariableNoDeclarada() {
        String codigo = """
                %funciones
                definir prueba():
                    entero resultado = valor + 10
                """;

        ResultadoSemanticoY resultado =
                analizarSemantica(codigo);

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaCondicionNoBooleana() {
        String codigo = """
                %funciones
                definir prueba():
                    entero edad = 18
                    si(edad) entonces
                        imprimir("hola")
                """;

        ResultadoSemanticoY resultado =
                analizarSemantica(codigo);

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaTipoDeRetornoIncorrecto() {
        String codigo = """
                %funciones
                definir prueba() -> entero:
                    retornar "hola"
                """;

        ResultadoSemanticoY resultado =
                analizarSemantica(codigo);

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaRomperFueraDeCiclo() {
        String codigo = """
                %funciones
                definir prueba():
                    romper
                """;

        ResultadoSemanticoY resultado =
                analizarSemantica(codigo);

        assertFalse(
                resultado.esValido()
        );
    }

    private ResultadoSemanticoY analizarSemantica(
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Funciones.y"),
                        LenguajeFuente.Y,
                        codigo
                );

        ResultadoAnalisisArchivo analisis =
                analizador.analizar(
                        archivo
                );

        assertTrue(
                analisis.esValido(),
                "El codigo debe pasar primero el analisis sintactico"
        );

        ProgramaAst ast =
                analisis.ast()
                        .orElseThrow();

        return new AnalizadorSemanticoY()
                .analizar(
                        List.of(ast)
                );
    }
}