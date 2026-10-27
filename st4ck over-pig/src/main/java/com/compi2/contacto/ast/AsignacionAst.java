package com.compi2.contacto.ast;

import java.util.Objects;

public record AsignacionAst(
        ExpresionAst destino,
        ExpresionAst valor,
        PosicionFuente posicion
) implements SentenciaAst {

    public AsignacionAst {
        Objects.requireNonNull(destino, "El destino es obligatorio");
        Objects.requireNonNull(valor, "El valor es obligatorio");
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }
}