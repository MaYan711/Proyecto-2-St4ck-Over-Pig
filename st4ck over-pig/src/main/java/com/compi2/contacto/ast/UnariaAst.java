package com.compi2.contacto.ast;

import java.util.Objects;

public record UnariaAst(
        Operador operador,
        ExpresionAst expresion,
        PosicionFuente posicion
) implements ExpresionAst {

    public UnariaAst {
        Objects.requireNonNull(
                operador,
                "El operador es obligatorio"
        );

        Objects.requireNonNull(
                expresion,
                "La expresion es obligatoria"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }

    public enum Operador {
        NEGACION,
        POSITIVO,
        NEGATIVO
    }
}