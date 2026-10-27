package com.compi2.contacto.c3d;

import com.compi2.contacto.compilacion.CompiladorProyecto;
import com.compi2.contacto.compilacion.ResultadoCompilacion;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Closeable;
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

        ejecutar(pig, "7\n9\n16\n");
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

        ejecutar(pig, "7\n");
    }

    private void ejecutar(String pig, String salidaEsperada) throws Exception {
        Path raiz = temporal.resolve("proyecto_" + System.nanoTime());
        ArchivoFuente archivo = new ArchivoFuente(
                raiz.resolve("main.pig"),
                LenguajeFuente.PIG_LATIN,
                pig
        );

        ResultadoCompilacion resultado = new CompiladorProyecto().compilar(
                new ProyectoCompilacion(raiz, List.of(archivo))
        );
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());

        String codigo = new GeneradorCodigoC().generar(resultado.programaMemoria());
        Assumptions.assumeTrue(gccDisponible(), "GCC no esta disponible en PATH");

        Path fuente = temporal.resolve("nombres_" + System.nanoTime() + ".c");
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path ejecutable = temporal.resolve(
                "nombres_" + System.nanoTime() + (windows ? ".exe" : "")
        );

        try {
            Files.writeString(fuente, codigo, StandardCharsets.UTF_8);

            ResultadoProceso compilacion = ejecutarProceso(
                    List.of("gcc", fuente.toString(), "-o", ejecutable.toString(), "-lm"),
                    20
            );
            assertEquals(0, compilacion.codigoSalida(), compilacion.salida());

            ResultadoProceso ejecucion = ejecutarProceso(List.of(ejecutable.toString()), 10);
            assertEquals(0, ejecucion.codigoSalida(), ejecucion.salida());
            assertEquals(normalizar(salidaEsperada), normalizar(ejecucion.salida()));
        } finally {
            try {
                eliminarTemporal(ejecutable);
            } finally {
                eliminarTemporal(fuente);
            }
        }
    }

    private ResultadoProceso ejecutarProceso(List<String> comando, int limiteSegundos)
            throws IOException, InterruptedException {
        Path archivoSalida = temporal.resolve("proceso_" + System.nanoTime() + ".log");
        Process proceso = null;

        try {
            proceso = new ProcessBuilder(comando)
                    .redirectErrorStream(true)
                    .redirectOutput(archivoSalida.toFile())
                    .start();

            boolean termino = proceso.waitFor(limiteSegundos, TimeUnit.SECONDS);
            if (!termino) {
                proceso.destroyForcibly();
                proceso.waitFor(5, TimeUnit.SECONDS);
            }

            String salida = Files.readString(archivoSalida, StandardCharsets.UTF_8);
            assertTrue(termino, "El proceso excedio el tiempo limite: " + comando + "\n" + salida);
            return new ResultadoProceso(proceso.exitValue(), salida);
        } finally {
            try {
                if (proceso != null) {
                    cerrarProceso(proceso);
                }
            } finally {
                eliminarTemporal(archivoSalida);
            }
        }
    }

    private boolean gccDisponible() {
        Process proceso = null;
        try {
            proceso = new ProcessBuilder("gcc", "--version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean termino = proceso.waitFor(5, TimeUnit.SECONDS);
            return termino && proceso.exitValue() == 0;
        } catch (IOException excepcion) {
            return false;
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (proceso != null) {
                cerrarProceso(proceso);
            }
        }
    }

    private void cerrarProceso(Process proceso) {
        if (proceso.isAlive()) {
            proceso.destroyForcibly();
            try {
                proceso.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
        }
        cerrarSilenciosamente(proceso.getOutputStream());
        cerrarSilenciosamente(proceso.getInputStream());
        cerrarSilenciosamente(proceso.getErrorStream());
    }

    private void cerrarSilenciosamente(Closeable flujo) {
        try {
            flujo.close();
        } catch (IOException ignorada) {
        }
    }

    private void eliminarTemporal(Path archivo) throws IOException {
        IOException ultimoError = null;
        for (int intento = 0; intento < 12; intento++) {
            try {
                Files.deleteIfExists(archivo);
                return;
            } catch (IOException excepcion) {
                ultimoError = excepcion;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
                throw new IOException("Limpieza interrumpida: " + archivo, excepcion);
            }
        }
        throw new IOException("No se pudo borrar el archivo temporal: " + archivo, ultimoError);
    }

    private String normalizar(String texto) {
        return texto.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private record ResultadoProceso(int codigoSalida, String salida) {
    }
}
