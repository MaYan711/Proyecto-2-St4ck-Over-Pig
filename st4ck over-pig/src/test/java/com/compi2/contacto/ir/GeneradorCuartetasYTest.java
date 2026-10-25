package com.compi2.contacto.ir;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.semantica.AnalizadorSemanticoY;
import com.compi2.contacto.semantica.ResultadoSemanticoY;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorCuartetasYTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void generaCuartetasDeExpresionYCondicional() {
        String codigo = """
                %funciones
                definir calcular(entero a, entero b) -> entero:
                    entero total = a + b * 2
                    si(total > 10) entonces
                        imprimir(total)
                    retornar total
                """;

        ProgramaIntermedio programa =
                generar(codigo);

        List<OperadorCuarteta> operadores =
                programa.cuartetas()
                        .stream()
                        .map(Cuarteta::operador)
                        .toList();

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.INICIO_FUNCION
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.MULTIPLICAR
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.SUMAR
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.MAYOR
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.SALTAR_SI_FALSO
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.IMPRIMIR
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.RETORNAR
                )
        );

        assertTrue(
                operadores.contains(
                        OperadorCuarteta.FIN_FUNCION
                )
        );
    }

    @Test
    void generaCuartetasDeCiclo() {
        String codigo = """
                %funciones
                definir prueba():
                    entero i = 0
                    mientras(i < 5) hacer
                        i++
                        si(i == 3) entonces
                            continuar
                """;

        ProgramaIntermedio programa =
                generar(codigo);

        assertFalse(
                programa.cuartetas().isEmpty()
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
                etiquetas >= 2
        );

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.SALTAR
                        )
        );
    }

    @Test
    void generaCuartetasDeLlamadaFuncion() {
        String codigo = """
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    retornar a + b
                definir prueba():
                    entero resultado = sumar(10, 20)
                    imprimir(resultado)
                """;

        ProgramaIntermedio programa =
                generar(codigo);

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
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LLAMAR
                                                && cuarteta.argumento1()
                                                .equals("sumar")
                        )
        );
    }

    private ProgramaIntermedio generar(
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
                "El codigo debe ser sintacticamente valido"
        );

        ProgramaAst ast =
                analisis.ast()
                        .orElseThrow();

        ResultadoSemanticoY semantica =
                new AnalizadorSemanticoY()
                        .analizar(
                                List.of(ast)
                        );

        assertTrue(
                semantica.esValido(),
                "El codigo debe ser semanticamente valido"
        );

        return new GeneradorCuartetasY()
                .generar(
                        List.of(ast)
                );
    }
}