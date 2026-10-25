package com.compi2.contacto.memoria;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ir.GeneradorIntermedioProyecto;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BajadorMemoriaProyectoTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void bajaVariablesYACcesosStack() {
        ProgramaAst y =
                analizar(
                        "Funciones.y",
                        LenguajeFuente.Y,
                        """
                        %funciones
                        definir sumar(entero a, entero b) -> entero:
                            entero resultado = a + b
                            retornar resultado
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
                        resultado = 10;
                        >> resultado;
                        FINIS;
                        """
                );

        PlanMemoriaProyecto plan =
                new PlanificadorMemoriaProyecto()
                        .planificar(
                                List.of(y),
                                List.of(),
                                List.of(pig)
                        );

        ProgramaIntermedio simbolico =
                new GeneradorIntermedioProyecto()
                        .generar(
                                List.of(y),
                                List.of(),
                                List.of(pig)
                        );

        ProgramaIntermedio memoria =
                new BajadorMemoriaProyecto()
                        .bajar(
                                simbolico,
                                plan
                        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LEER_STACK
                                                && cuarteta.argumento1()
                                                .equals("P+1")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LEER_STACK
                                                && cuarteta.argumento1()
                                                .equals("P+2")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ESCRIBIR_STACK
                                                && cuarteta.argumento1()
                                                .equals("P+3")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ESCRIBIR_STACK
                                                && cuarteta.argumento1()
                                                .equals("P+0")
                        )
        );
    }

    @Test
    void bajaAtributosZAlHeap() {
        ProgramaAst z =
                analizar(
                        "Persona.z",
                        LenguajeFuente.ZETARIANO,
                        """
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

                            public int obtenerEdad() {
                                return edad;
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
                        FINIS;
                        """
                );

        PlanMemoriaProyecto plan =
                new PlanificadorMemoriaProyecto()
                        .planificar(
                                List.of(),
                                List.of(z),
                                List.of(pig)
                        );

        ProgramaIntermedio simbolico =
                new GeneradorIntermedioProyecto()
                        .generar(
                                List.of(),
                                List.of(z),
                                List.of(pig)
                        );

        ProgramaIntermedio memoria =
                new BajadorMemoriaProyecto()
                        .bajar(
                                simbolico,
                                plan
                        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ESCRIBIR_HEAP
                                                && cuarteta.argumento1()
                                                .endsWith("+0")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ESCRIBIR_HEAP
                                                && cuarteta.argumento1()
                                                .endsWith("+1")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LEER_HEAP
                                                && cuarteta.argumento1()
                                                .endsWith("+1")
                        )
        );
    }

    @Test
    void bajaVariablesPigAlStack() {
        ProgramaAst pig =
                analizar(
                        "Main.pig",
                        LenguajeFuente.PIG_LATIN,
                        """
                        VARIABILES>
                        esto personaEdad : numerus 0;
                        esto resultado : numerus 0;

                        MAIOR>
                        resultado = 20;
                        personaEdad = resultado;
                        >> personaEdad;
                        FINIS;
                        """
                );

        PlanMemoriaProyecto plan =
                new PlanificadorMemoriaProyecto()
                        .planificar(
                                List.of(),
                                List.of(),
                                List.of(pig)
                        );

        ProgramaIntermedio simbolico =
                new GeneradorIntermedioProyecto()
                        .generar(
                                List.of(),
                                List.of(),
                                List.of(pig)
                        );

        ProgramaIntermedio memoria =
                new BajadorMemoriaProyecto()
                        .bajar(
                                simbolico,
                                plan
                        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ESCRIBIR_STACK
                                                && cuarteta.argumento1()
                                                .equals("P+1")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.ESCRIBIR_STACK
                                                && cuarteta.argumento1()
                                                .equals("P+2")
                        )
        );

        assertTrue(
                memoria.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LEER_STACK
                        )
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
                                Path.of(nombre),
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
}