package com.compi2.contacto.ast;

import java.util.Objects;

public record TipoAst(
        String nombre,
        PosicionFuente posicion
) implements NodoAst {

    public TipoAst {
        Objects.requireNonNull(nombre, "El nombre del tipo es obligatorio");
        Objects.requireNonNull(posicion, "La posicion es obligatoria");

        if (nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del tipo no puede estar vacio"
            );
        }
    }
}