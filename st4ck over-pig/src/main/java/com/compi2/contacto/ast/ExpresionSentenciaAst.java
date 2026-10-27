package com.compi2.contacto.ast;

import java.util.Objects;

public record ExpresionSentenciaAst(
        ExpresionAst expresion,
        PosicionFuente posicion
) implements SentenciaAst {

    public ExpresionSentenciaAst {
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