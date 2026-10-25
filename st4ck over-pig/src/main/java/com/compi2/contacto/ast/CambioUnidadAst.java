package com.compi2.contacto.ast;

import java.util.Objects;

public record CambioUnidadAst(
        ExpresionAst destino,
        Operacion operacion,
        PosicionFuente posicion
) implements SentenciaAst {

    public CambioUnidadAst {
        Objects.requireNonNull(destino, "El destino es obligatorio");
        Objects.requireNonNull(operacion, "La operacion es obligatoria");
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public enum Operacion {
        INCREMENTO,
        DECREMENTO
    }
}