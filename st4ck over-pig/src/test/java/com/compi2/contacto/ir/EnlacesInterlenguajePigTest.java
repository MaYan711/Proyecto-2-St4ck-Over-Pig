package com.compi2.contacto.ir;

import com.compi2.contacto.compilacion.CompiladorProyecto;
import com.compi2.contacto.compilacion.ResultadoCompilacion;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnlacesInterlenguajePigTest {

    @Test
    void enlazaFuncionYConstructorYMetodoZ() {
        Path raiz =
                Path.of(
                        "proyecto-enlaces"
                );

        String y = """
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    retornar a + b
                """;

        String z = """
                public class Persona {

                    String nombre;
                    int edad;

                    public Persona(String nombreParametro, int edadParametro) {
                        nombre = nombreParametro;
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
                esto persona : novus Persona("Mario", 20);
                esto resultado : numerus 0;

                MAIOR>
                resultado = sumar(10, 20);
                resultado = persona.obtenerEdad();
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

        assertTrue(
                resultado.esValido()
        );

        ProgramaIntermedio programa =
                resultado.programaIntermedio();

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.NUEVO_OBJETO
                                                && cuarteta.argumento1()
                                                .equals(
                                                        "Persona.<init>(String,int)"
                                                )
                        )
        );

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LLAMAR
                                                && cuarteta.argumento1()
                                                .equals(
                                                        "sumar"
                                                )
                                                && cuarteta.argumento2()
                                                .equals(
                                                        "2"
                                                )
                        )
        );

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.LLAMAR
                                                && cuarteta.argumento1()
                                                .equals(
                                                        "Persona.obtenerEdad()"
                                                )
                                                && cuarteta.argumento2()
                                                .equals(
                                                        "1"
                                                )
                        )
        );

        assertTrue(
                programa.cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        cuarteta.operador()
                                                == OperadorCuarteta.PARAMETRO
                                                && cuarteta.argumento1()
                                                .equals(
                                                        "persona"
                                                )
                                                && cuarteta.argumento2()
                                                .equals(
                                                        "0"
                                                )
                        )
        );
    }
}