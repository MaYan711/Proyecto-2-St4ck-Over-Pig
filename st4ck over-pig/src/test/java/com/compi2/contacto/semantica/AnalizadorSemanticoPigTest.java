package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalizadorSemanticoPigTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaImportsEIntegracionDeLosTresLenguajes() {
        String y = """
                %estructuras
                estructura Punto:
                    entero x
                    entero y
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    retornar a + b
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
                esto punto : Punto {10, 20};
                esto persona : novus Persona(25);
                esto resultado : numerus 0;

                MAIOR>
                resultado = sumar(10, 20);
                resultado = persona.obtenerEdad();
                >> punto.x;
                >> resultado;
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        y,
                        z,
                        pig
                );

        assertTrue(
                resultado.esValido()
        );
    }

    @Test
    void detectaArchivoImportadoInexistente() {
        String pig = """
                import NoExiste.y

                MAIOR>
                >> "Hola";
                FINIS;
                """;

        ProyectoCompilacion proyecto =
                proyecto(
                        null,
                        null,
                        pig
                );

        ResultadoSemanticoPig resultado =
                analizarProyecto(
                        proyecto
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaEstructuraNoImportada() {
        String y = """
                %estructuras
                estructura Punto:
                    entero x
                    entero y
                %funciones
                definir prueba():
                    imprimir("ok")
                """;

        String pig = """
                MAIOR>
                esto punto : Punto {10, 20};
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        y,
                        null,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaFuncionYNoImportada() {
        String y = """
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    retornar a + b
                """;

        String pig = """
                MAIOR>
                esto resultado : numerus 0;
                resultado = sumar(10, 20);
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        y,
                        null,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaClaseZNoImportada() {
        String z = """
                public class Persona {

                    public Persona() {
                    }
                }
                """;

        String pig = """
                MAIOR>
                esto persona : novus Persona();
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        null,
                        z,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaParametroIncorrectoEnFuncionY() {
        String y = """
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    retornar a + b
                """;

        String pig = """
                import Funciones.y

                MAIOR>
                esto resultado : numerus 0;
                resultado = sumar("hola", 20);
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        y,
                        null,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaConstructorZIncorrecto() {
        String z = """
                public class Persona {

                    public Persona(int edad) {
                    }
                }
                """;

        String pig = """
                import Persona.z

                MAIOR>
                esto persona : novus Persona("Mario");
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        null,
                        z,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaCondicionNoBooleana() {
        String pig = """
                VARIABILES>
                esto edad : numerus 20;

                MAIOR>
                si (edad) {
                    >> "Hola";
                } finis;
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        null,
                        null,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    @Test
    void detectaInterrumpeFueraDeCiclo() {
        String pig = """
                MAIOR>
                interrumpe;
                FINIS;
                """;

        ResultadoSemanticoPig resultado =
                analizar(
                        null,
                        null,
                        pig
                );

        assertFalse(
                resultado.esValido()
        );
    }

    private ResultadoSemanticoPig analizar(
            String y,
            String z,
            String pig
    ) {
        return analizarProyecto(
                proyecto(
                        y,
                        z,
                        pig
                )
        );
    }

    private ResultadoSemanticoPig analizarProyecto(
            ProyectoCompilacion proyecto
    ) {
        List<ResultadoAnalisisArchivo> resultados =
                proyecto.archivos()
                        .stream()
                        .map(
                                analizador::analizar
                        )
                        .toList();

        return new AnalizadorSemanticoPig()
                .analizar(
                        proyecto,
                        resultados
                );
    }

    private ProyectoCompilacion proyecto(
            String y,
            String z,
            String pig
    ) {
        Path raiz =
                Path.of(
                        "proyecto-prueba-pig"
                );

        List<ArchivoFuente> archivos =
                new java.util.ArrayList<>();

        if (y != null) {
            archivos.add(
                    new ArchivoFuente(
                            raiz.resolve(
                                    "Funciones.y"
                            ),
                            LenguajeFuente.Y,
                            y
                    )
            );
        }

        if (z != null) {
            archivos.add(
                    new ArchivoFuente(
                            raiz.resolve(
                                    "Persona.z"
                            ),
                            LenguajeFuente.ZETARIANO,
                            z
                    )
            );
        }

        if (pig != null) {
            archivos.add(
                    new ArchivoFuente(
                            raiz.resolve(
                                    "Main.pig"
                            ),
                            LenguajeFuente.PIG_LATIN,
                            pig
                    )
            );
        }

        return new ProyectoCompilacion(
                raiz,
                archivos
        );
    }
}