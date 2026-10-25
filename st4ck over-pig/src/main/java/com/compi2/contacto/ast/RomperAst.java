package com.compi2.contacto.ast;

import java.util.Objects;

public record RomperAst(
        PosicionFuente posicion
) implements SentenciaAst {

    public RomperAst {
        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}