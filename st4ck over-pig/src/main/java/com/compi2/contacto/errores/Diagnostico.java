package com.compi2.contacto.errores;

import java.nio.file.Path;
import java.util.Objects;

public record Diagnostico(
        TipoDiagnostico tipo,
        Severidad severidad,
        Path archivo,
        int linea,
        int columna,
        String mensaje
) {
    public Diagnostico {
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        Objects.requireNonNull(severidad, "La severidad es obligatoria");
        Objects.requireNonNull(mensaje, "El mensaje es obligatorio");

        if (linea < 0 || columna < 0) {
            throw new IllegalArgumentException(
                    "La linea y la columna no pueden ser negativas"
            );
        }
    }

    public boolean esError() {
        return severidad == Severidad.ERROR;
    }
}

