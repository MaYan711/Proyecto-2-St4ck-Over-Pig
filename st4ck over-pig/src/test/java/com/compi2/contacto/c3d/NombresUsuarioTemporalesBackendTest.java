package com.compi2.contacto.c3d;

import com.compi2.contacto.compilacion.CompiladorProyecto;
import com.compi2.contacto.compilacion.ResultadoCompilacion;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NombresUsuarioTemporalesBackendTest {

    @TempDir
    Path temporal;

    @Test
    void variablesT0YL0SeLeenDesdeStack() throws Exception {
        String pig = """
                VARIABILES>
                esto t0 : numerus 7;
                esto L0 : numerus 9;
                esto suma : numerus 0;

                MAIOR>
                suma = t0 + L0;
                >> t0;
                >> "\\n";
                >> L0;
                >> "\\n";
                >> suma;
                >> "\\n";
                FINIS;
                """;

        ejecutar(
                pig,
                "7\n9\n16\n"
        );
    }

    @Test
    void variableT0PuedeUsarseComoIndiceDeArreglo() throws Exception {
        String pig = """
                VARIABILES>
                esto t0 : numerus 2;
                series datos[3] : numerus {5, 6, 7};

                MAIOR>
                >> datos[t0];
                >> "\\n";
                FINIS;
                """;

        ejecutar(
                pig,
                "7\n"
        );
    }

    private void ejecutar(
            String pig,
            String salidaEsperada
    ) throws Exception {
        Path raiz =
                temporal.resolve(
                        "proyecto_"
                                + System.nanoTime()
                );

        ArchivoFuente archivo =
                new ArchivoFuente(
                        raiz.resolve(
                                "main.pig"
                        ),
                        LenguajeFuente.PIG_LATIN,
                        pig
                );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(
                                new ProyectoCompilacion(
                                        raiz,
                                        List.of(
                                                archivo
                                        )
                                )
                        );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos()
                        .toString()
        );

        String codigo =
                new GeneradorCodigoC()
                        .generar(
                                resultado.programaMemoria()
                        );

        Assumptions.assumeTrue(
                gccDisponible(),
                "GCC no esta disponible en PATH"
        );

        Path fuente =
                temporal.resolve(
                        "nombres_"
                                + System.nanoTime()
                                + ".c"
                );

        boolean windows =
                System.getProperty(
                                "os.name"
                        )
                        .toLowerCase()
                        .contains(
                                "win"
                        );

        Path ejecutable =
                temporal.resolve(
                        "nombres_"
                                + System.nanoTime()
                                + (
                                windows
                                        ? ".exe"
                                        : ""
                        )
                );

        Files.writeString(
                fuente,
                codigo,
                StandardCharsets.UTF_8
        );

        Process compilacion =
                new ProcessBuilder(
                        "gcc",
                        fuente.toString(),
                        "-o",
                        ejecutable.toString(),
                        "-lm"
                )
                        .redirectErrorStream(
                                true
                        )
                        .start();

        String salidaCompilacion =
                new String(
                        compilacion
                                .getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        assertTrue(
                compilacion.waitFor(
                        20,
                        TimeUnit.SECONDS
                ),
                "GCC excedio el tiempo limite"
        );

        assertEquals(
                0,
                compilacion.exitValue(),
                salidaCompilacion
        );

        Process ejecucion =
                new ProcessBuilder(
                        ejecutable.toString()
                )
                        .redirectErrorStream(
                                true
                        )
                        .start();

        String salida =
                new String(
                        ejecucion
                                .getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        assertTrue(
                ejecucion.waitFor(
                        10,
                        TimeUnit.SECONDS
                ),
                "El ejecutable excedio el tiempo limite"
        );

        assertEquals(
                0,
                ejecucion.exitValue(),
                salida
        );

        assertEquals(
                normalizar(
                        salidaEsperada
                ),
                normalizar(
                        salida
                )
        );
    }

    private boolean gccDisponible() {
        try {
            Process proceso =
                    new ProcessBuilder(
                            "gcc",
                            "--version"
                    )
                            .redirectErrorStream(
                                    true
                            )
                            .start();

            proceso
                    .getInputStream()
                    .readAllBytes();

            return proceso.waitFor(
                    5,
                    TimeUnit.SECONDS
            )
                    && proceso.exitValue() == 0;

        } catch (IOException excepcion) {
            return false;

        } catch (InterruptedException excepcion) {
            Thread.currentThread()
                    .interrupt();

            return false;
        }
    }

    private String normalizar(
            String texto
    ) {
        return texto
                .replace(
                        "\r\n",
                        "\n"
                )
                .replace(
                        '\r',
                        '\n'
                )
                .strip();
    }
}
