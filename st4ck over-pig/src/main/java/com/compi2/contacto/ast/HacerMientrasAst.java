package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record HacerMientrasAst(
        List<SentenciaAst> cuerpo,
        ExpresionAst condicion,
        PosicionFuente posicion
) implements SentenciaAst {

    public HacerMientrasAst {
        cuerpo = List.copyOf(
                Objects.requireNonNull(
                        cuerpo,
                        "El cuerpo es obligatorio"
                )
        );

        Objects.requireNonNull(
                condicion,
                "La condicion es obligatoria"
        );

        Objects.requireNonNull(
                posicion,
                "La posicion es obligatoria"
        );
    }
}