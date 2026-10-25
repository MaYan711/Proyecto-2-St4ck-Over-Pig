package com.compi2.contacto.proyecto;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public enum LenguajeFuente {
    Y(".y", "Y?"),
    ZETARIANO(".z", "Zetariano"),
    PIG_LATIN(".pig", "PigLatin");

    private final String extension;
    private final String nombreVisible;

    LenguajeFuente(String extension, String nombreVisible) {
        this.extension = extension;
        this.nombreVisible = nombreVisible;
    }

    public String extension() {
        return extension;
    }

    public String nombreVisible() {
        return nombreVisible;
    }

    public static Optional<LenguajeFuente> desdeRuta(Path ruta) {
        if (ruta == null || ruta.getFileName() == null) {
            return Optional.empty();
        }

        String nombre = ruta.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        for (LenguajeFuente lenguaje : values()) {
            if (nombre.endsWith(lenguaje.extension)) {
                return Optional.of(lenguaje);
            }
        }

        return Optional.empty();
    }
}

