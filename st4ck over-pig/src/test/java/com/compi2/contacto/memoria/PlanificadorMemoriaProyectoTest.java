package com.compi2.contacto.memoria;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanificadorMemoriaProyectoTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void planificaStackYHeapDeLosTresLenguajes() {
        ProgramaAst y =
                analizar(
                        "Funciones.y",
                        LenguajeFuente.Y,
                        """
                        %estructuras
                        estructura Punto:
                            entero x
                            entero y

                        %funciones
                        definir sumar(entero a, entero b) -> entero:
                            entero resultado = a + b
                            retornar resultado
                        """
                );

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
                        VARIABILES>
                        esto persona : novus Persona("Mario", 20);
                        esto resultado : numerus 0;

                        MAIOR>
                        >> resultado;
                        FINIS;
                        """
                );

        PlanMemoriaProyecto plan =
                new PlanificadorMemoriaProyecto()
                        .planificar(
                                List.of(y),
                                List.of(z),
                                List.of(pig)
                        );

        MarcoStackProyecto sumar =
                plan.marco(
                        "sumar"
                ).orElseThrow();

        assertEquals(
                4,
                sumar.tamano()
        );

        assertEquals(
                0,
                sumar.buscar(
                                "$retorno"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                1,
                sumar.buscar(
                                "a"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                2,
                sumar.buscar(
                                "b"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                3,
                sumar.buscar(
                                "resultado"
                        ).orElseThrow()
                        .desplazamiento()
        );

        MarcoStackProyecto constructor =
                plan.marco(
                        "Persona.<init>(String,int)"
                ).orElseThrow();

        assertEquals(
                4,
                constructor.tamano()
        );

        assertEquals(
                0,
                constructor.buscar(
                                "$retorno"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                1,
                constructor.buscar(
                                "this"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                2,
                constructor.buscar(
                                "nombreParametro"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                3,
                constructor.buscar(
                                "edadParametro"
                        ).orElseThrow()
                        .desplazamiento()
        );

        MarcoStackProyecto metodo =
                plan.marco(
                        "Persona.obtenerEdad()"
                ).orElseThrow();

        assertEquals(
                2,
                metodo.tamano()
        );

        assertTrue(
                metodo.thisSlot()
                        .isPresent()
        );

        MarcoStackProyecto maior =
                plan.marco(
                        "MAIOR"
                ).orElseThrow();

        assertEquals(
                3,
                maior.tamano()
        );

        assertEquals(
                1,
                maior.buscar(
                                "persona"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                2,
                maior.buscar(
                                "resultado"
                        ).orElseThrow()
                        .desplazamiento()
        );

        LayoutHeapProyecto persona =
                plan.layout(
                        "Z::Persona"
                ).orElseThrow();

        assertEquals(
                2,
                persona.tamano()
        );

        assertEquals(
                0,
                persona.buscarCampo(
                                "nombre"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                1,
                persona.buscarCampo(
                                "edad"
                        ).orElseThrow()
                        .desplazamiento()
        );

        LayoutHeapProyecto punto =
                plan.layout(
                        "Y::Punto"
                ).orElseThrow();

        assertEquals(
                2,
                punto.tamano()
        );

        assertEquals(
                0,
                punto.buscarCampo(
                                "x"
                        ).orElseThrow()
                        .desplazamiento()
        );

        assertEquals(
                1,
                punto.buscarCampo(
                                "y"
                        ).orElseThrow()
                        .desplazamiento()
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
}