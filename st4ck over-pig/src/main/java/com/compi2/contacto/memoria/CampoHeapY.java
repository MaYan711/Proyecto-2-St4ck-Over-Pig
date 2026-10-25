package com.compi2.contacto.memoria;

import java.util.Objects;

public record CampoHeapY(
        String nombre,
        int desplazamiento,
        String tipoDeclarado,
        boolean referencia,
        int dimensiones
) {

    public CampoHeapY {
        Objects.requireNonNull(
                nombre,
                "El nombre es obligatorio"
        );

        Objects.requireNonNull(
                tipoDeclarado,
                "El tipo es obligatorio"
        );

        if (desplazamiento < 0) {
            throw new IllegalArgumentException(
                    "El desplazamiento no puede ser negativo"
            );
        }

        if (dimensiones < 0) {
            throw new IllegalArgumentException(
                    "La cantidad de dimensiones no puede ser negativa"
            );
        }
    }

    public boolean esArreglo() {
        return dimensiones > 0;
    }
}