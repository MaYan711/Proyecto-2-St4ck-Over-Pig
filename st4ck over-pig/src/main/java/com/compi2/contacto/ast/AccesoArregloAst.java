package com.compi2.contacto.ast;

import java.util.Objects;

public record AccesoArregloAst(
        ExpresionAst objetivo,
        ExpresionAst indice,
        PosicionFuente posicion
) implements ExpresionAst {

    public AccesoArregloAst {
        Objects.requireNonNull(
                objetivo,
                "El objetivo es obligatorio"
        );

        Objects.requireNonNull(
                indice,
                "El indice es obligatorio"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}