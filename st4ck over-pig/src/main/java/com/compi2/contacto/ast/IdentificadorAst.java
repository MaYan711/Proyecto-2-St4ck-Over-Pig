package com.compi2.contacto.ast;

import java.util.Objects;

public record IdentificadorAst(
        String nombre,
        PosicionFuente posicion
) implements ExpresionAst {

    public IdentificadorAst {
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }
}