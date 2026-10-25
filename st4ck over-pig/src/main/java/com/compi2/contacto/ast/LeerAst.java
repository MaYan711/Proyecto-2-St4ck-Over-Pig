package com.compi2.contacto.ast;

import java.util.Objects;

public record LeerAst(
        PosicionFuente posicion
) implements ExpresionAst {

    public LeerAst {
        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}