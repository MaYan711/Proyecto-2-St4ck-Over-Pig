package com.compi2.contacto.semantica;

import com.compi2.contacto.compilacion.CompiladorProyecto;
import com.compi2.contacto.compilacion.ResultadoCompilacion;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TablaSimbolosProyectoTest {

    @Test
    void construyeTablaUnificadaDeLosTresLenguajes() {
        Path raiz =
                Path.of(
                        "proyecto-tabla"
                );

        String y = """
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    entero resultado = a + b
                    retornar resultado
                """;

        String z = """
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

        String pig = """
                import Funciones.y
                import Persona.z

                VARIABILES>
                esto persona : novus Persona(20);
                esto total : numerus 0;

                MAIOR>
                total = sumar(1, 2);
                >> total;
                FINIS;
                """;

        ProyectoCompilacion proyecto =
                new ProyectoCompilacion(
                        raiz,
                        List.of(
                                new ArchivoFuente(
                                        raiz.resolve(
                                                "Funciones.y"
                                        ),
                                        LenguajeFuente.Y,
                                        y
                                ),
                                new ArchivoFuente(
                                        raiz.resolve(
                                                "Persona.z"
                                        ),
                                        LenguajeFuente.ZETARIANO,
                                        z
                                ),
                                new ArchivoFuente(
                                        raiz.resolve(
                                                "Main.pig"
                                        ),
                                        LenguajeFuente.PIG_LATIN,
                                        pig
                                )
                        )
                );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(
                                proyecto
                        );

        TablaSimbolos tabla =
                resultado.tablaSimbolos();

        assertTrue(
                tabla.buscar(
                        "Y::funcion::sumar"
                ).isPresent()
        );

        assertTrue(
                tabla.buscar(
                        "Y::funcion::sumar::parametro::a"
                ).isPresent()
        );

        assertTrue(
                tabla.buscar(
                        "Z::clase::Persona"
                ).isPresent()
        );

        assertTrue(
                tabla.buscar(
                        "Z::clase::Persona::atributo::edad"
                ).isPresent()
        );

        assertTrue(
                tabla.buscar(
                        "Z::clase::Persona::metodo::obtenerEdad()"
                ).isPresent()
        );

        assertTrue(
                tabla.buscar(
                        "PIG::Main.pig::global::variable::persona"
                ).isPresent()
        );

        assertTrue(
                tabla.buscar(
                        "PIG::Main.pig::global::variable::total"
                ).isPresent()
        );
    }
}