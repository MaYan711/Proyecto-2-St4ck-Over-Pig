package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record MientrasAst(
        ExpresionAst condicion,
        List<SentenciaAst> cuerpo,
        PosicionFuente posicion
) implements SentenciaAst {

    public MientrasAst {
        Objects.requireNonNull(
                condicion,
                "La condicion es obligatoria"
        );

        cuerpo = List.copyOf(
                Objects.requireNonNull(
                        cuerpo,
                        "El cuerpo es obligatorio"
                )
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }
}