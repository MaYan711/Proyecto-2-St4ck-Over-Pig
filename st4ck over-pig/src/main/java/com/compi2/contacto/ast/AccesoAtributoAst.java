package com.compi2.contacto.ast;

import java.util.Objects;

public record AccesoAtributoAst(
        ExpresionAst objetivo,
        String atributo,
        PosicionFuente posicion
) implements ExpresionAst {

    public AccesoAtributoAst {
        Objects.requireNonNull(
                objetivo,
                "El objetivo es obligatorio"
        );

        Objects.requireNonNull(
                atributo,
                "El atributo es obligatorio"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}