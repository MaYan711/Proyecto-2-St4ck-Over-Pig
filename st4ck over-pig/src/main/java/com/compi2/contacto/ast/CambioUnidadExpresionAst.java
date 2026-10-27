package com.compi2.contacto.ast;

import java.util.Objects;

public record CambioUnidadExpresionAst(
        ExpresionAst objetivo,
        Operacion operacion,
        PosicionFuente posicion
) implements ExpresionAst {

    public CambioUnidadExpresionAst {
        Objects.requireNonNull(
                objetivo,
                "El objetivo es obligatorio"
        );

        Objects.requireNonNull(
                operacion,
                "La operacion es obligatoria"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }

    public enum Operacion {
        INCREMENTO,
        DECREMENTO
    }
}