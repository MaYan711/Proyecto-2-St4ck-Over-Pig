package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record InicializadorListaAst(
        List<ExpresionAst> valores,
        PosicionFuente posicion
) implements ExpresionAst {

    public InicializadorListaAst {
        valores = List.copyOf(
                Objects.requireNonNull(
                        valores,
                        "Los valores son obligatorios"
                )
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}