package com.compi2.contacto.ast;

import java.util.Objects;

public record RetornarAst(
        ExpresionAst expresion,
        PosicionFuente posicion
) implements SentenciaAst {

    public RetornarAst {
        Objects.requireNonNull(
                expresion,
                "La expresion es obligatoria"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}