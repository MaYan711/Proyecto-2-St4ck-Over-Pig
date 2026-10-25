package com.compi2.contacto.compilacion;

import com.compi2.contacto.c3d.GeneradorRiscV;
import com.compi2.contacto.proyecto.GestorProyecto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ExportadorLoteRiscV {

    private ExportadorLoteRiscV() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Indique la carpeta de pruebas RISC-V");
        }

        Path raiz = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isDirectory(raiz)) {
            throw new IllegalArgumentException("No existe la carpeta: " + raiz);
        }

        List<Path> carpetas;
        try (Stream<Path> directorios = Files.list(raiz)) {
            carpetas = directorios.filter(Files::isDirectory)
                    .filter(ExportadorLoteRiscV::contienePig)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        if (carpetas.isEmpty()) {
            throw new IllegalArgumentException("No hay subcarpetas con un archivo .pig");
        }

        int correctos = 0;
        int errores = 0;
        for (Path carpeta : carpetas) {
            try {
                var proyecto = new GestorProyecto().abrir(carpeta);
                ResultadoCompilacion resultado = new CompiladorProyecto().compilar(proyecto);
                if (!resultado.esValido()) {
                    throw new IllegalStateException("Diagnosticos: " + resultado.diagnosticos());
                }
                String ensamblador = new GeneradorRiscV().generar(resultado.programaMemoria());
                Files.writeString(carpeta.resolve("programa.s"), ensamblador, StandardCharsets.UTF_8);
                System.out.println("OK " + carpeta.getFileName() + " -> programa.s");
                correctos++;
            } catch (IOException | RuntimeException excepcion) {
                System.err.println("ERROR " + carpeta.getFileName() + " -> " + excepcion.getMessage());
                errores++;
            }
        }
        System.out.println("Generados: " + correctos + " | Fallidos: " + errores);
        if (errores > 0) {
            throw new IllegalStateException("No se pudieron generar todas las pruebas");
        }
    }

    private static boolean contienePig(Path carpeta) {
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase().endsWith(".pig"));
        } catch (IOException excepcion) {
            return false;
        }
    }
}
