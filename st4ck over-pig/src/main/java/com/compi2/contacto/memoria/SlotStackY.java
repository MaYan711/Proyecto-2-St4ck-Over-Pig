package com.compi2.contacto.memoria;

import java.util.Objects;

public record SlotStackY(
        String nombre,
        ClaseSlotStackY clase,
        String tipoDeclarado,
        DireccionMemoria direccion,
        boolean referencia
) {

    public SlotStackY {
        Objects.requireNonNull(
                nombre,
                "El nombre es obligatorio"
        );

        Objects.requireNonNull(
                clase,
                "La clase del slot es obligatoria"
        );

        Objects.requireNonNull(
                tipoDeclarado,
                "El tipo declarado es obligatorio"
        );

        Objects.requireNonNull(
                direccion,
                "La direccion es obligatoria"
        );

        if (direccion.segmento() != SegmentoMemoria.STACK) {
            throw new IllegalArgumentException(
                    "Un SlotStackY debe pertenecer al STACK"
            );
        }
    }
}