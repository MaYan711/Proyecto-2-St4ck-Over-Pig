package com.compi2.contacto.proyecto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GestorProyecto {

    public ProyectoCompilacion abrir(Path raiz) throws IOException {
        if (raiz == null || !Files.isDirectory(raiz)) {
            throw new IllegalArgumentException(
                    "La ruta del proyecto debe ser una carpeta existente"
            );
        }

        List<ArchivoFuente> archivos = new ArrayList<>();

        try (var rutas = Files.walk(raiz)) {
            rutas.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(ruta -> LenguajeFuente.desdeRuta(ruta)
                            .ifPresent(lenguaje -> archivos.add(
                                    leerArchivo(ruta, lenguaje)
                            )));
        }

        return new ProyectoCompilacion(raiz, archivos);
    }

    public void guardar(Path ruta, String contenido) throws IOException {
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }

        Files.writeString(
                ruta,
                contenido == null ? "" : contenido,
                StandardCharsets.UTF_8
        );
    }

    private ArchivoFuente leerArchivo(
            Path ruta,
            LenguajeFuente lenguaje
    ) {
        try {
            return new ArchivoFuente(
                    ruta,
                    lenguaje,
                    Files.readString(ruta, StandardCharsets.UTF_8)
            );
        } catch (IOException excepcion) {
            throw new ArchivoProyectoException(ruta, excepcion);
        }
    }

    private static final class ArchivoProyectoException
            extends RuntimeException {

        private ArchivoProyectoException(
                Path ruta,
                IOException causa
        ) {
            super("No se pudo leer el archivo: " + ruta, causa);
        }
    }
}

