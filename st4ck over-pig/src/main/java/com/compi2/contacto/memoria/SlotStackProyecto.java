package com.compi2.contacto.memoria;

import com.compi2.contacto.ast.PosicionFuente;

import java.util.Objects;

public record SlotStackProyecto(
        String nombre,
        String tipo,
        int desplazamiento,
        Clase clase,
        PosicionFuente posicion
) {

    public SlotStackProyecto {
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(clase);
        Objects.requireNonNull(posicion);

        if (desplazamiento < 0) {
            throw new IllegalArgumentException(
                    "El desplazamiento no puede ser negativo"
            );
        }
    }

    public String direccion() {
        return "P+" + desplazamiento;
    }

    public enum Clase {
        RETORNO,
        THIS,
        PARAMETRO,
        LOCAL,
        GLOBAL_PIG
    }
}