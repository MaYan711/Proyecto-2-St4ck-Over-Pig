package com.compi2.contacto.memoria;

import java.util.Objects;

public record DireccionMemoria(
        SegmentoMemoria segmento,
        int desplazamiento
) {
    public DireccionMemoria {
        Objects.requireNonNull(segmento, "El segmento es obligatorio");
        if (desplazamiento < 0) {
            throw new IllegalArgumentException(
                    "El desplazamiento no puede ser negativo"
            );
        }
    }
}

