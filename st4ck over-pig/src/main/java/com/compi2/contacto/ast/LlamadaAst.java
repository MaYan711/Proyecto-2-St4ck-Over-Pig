package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record LlamadaAst(
        ExpresionAst objetivo,
        List<ExpresionAst> argumentos,
        PosicionFuente posicion
) implements ExpresionAst {

    public LlamadaAst {
        Objects.requireNonNull(
                objetivo,
                "El objetivo es obligatorio"
        );

        argumentos = List.copyOf(
                Objects.requireNonNull(
                        argumentos,
                        "Los argumentos son obligatorios"
                )
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}