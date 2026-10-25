package com.compi2.contacto.proyecto;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ProyectoCompilacion(
        Path raiz,
        List<ArchivoFuente> archivos
) {
    public ProyectoCompilacion {
        Objects.requireNonNull(raiz, "La raiz es obligatoria");
        archivos = List.copyOf(
                Objects.requireNonNull(archivos, "Los archivos son obligatorios")
        );
    }

    public boolean contieneLenguaje(LenguajeFuente lenguaje) {
        return archivos.stream()
                .anyMatch(archivo -> archivo.lenguaje() == lenguaje);
    }
}

