package com.compi2.contacto.ast;

import java.util.Objects;

public record BinariaAst(
        ExpresionAst izquierda,
        Operador operador,
        ExpresionAst derecha,
        PosicionFuente posicion
) implements ExpresionAst {

    public BinariaAst {
        Objects.requireNonNull(
                izquierda,
                "La expresion izquierda es obligatoria"
        );

        Objects.requireNonNull(
                operador,
                "El operador es obligatorio"
        );

        Objects.requireNonNull(
                derecha,
                "La expresion derecha es obligatoria"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }

    public enum Operador {
        SUMA,
        RESTA,
        MULTIPLICACION,
        DIVISION,
        IGUALDAD,
        DIFERENTE,
        MENOR,
        MAYOR,
        MENOR_IGUAL,
        MAYOR_IGUAL,
        AND,
        OR
    }
}