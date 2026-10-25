package com.compi2.contacto.ast;

import java.util.Objects;

public record ContinuarAst(
        PosicionFuente posicion
) implements SentenciaAst {

    public ContinuarAst {
        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}