package com.compi2.contacto.proyecto;

import java.nio.file.Path;
import java.util.Objects;

public record ArchivoFuente(
        Path ruta,
        LenguajeFuente lenguaje,
        String contenido
) {
    public ArchivoFuente {
        Objects.requireNonNull(ruta, "La ruta es obligatoria");
        Objects.requireNonNull(lenguaje, "El lenguaje es obligatorio");
        Objects.requireNonNull(contenido, "El contenido es obligatorio");
    }

    public String nombre() {
        return ruta.getFileName().toString();
    }
}

