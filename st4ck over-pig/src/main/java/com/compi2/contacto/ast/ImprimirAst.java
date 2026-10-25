package com.compi2.contacto.ast;

import java.util.Objects;

public record ImprimirAst(
        ExpresionAst expresion,
        PosicionFuente posicion
) implements SentenciaAst {

    public ImprimirAst {
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