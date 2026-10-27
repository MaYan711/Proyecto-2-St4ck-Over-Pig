package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record ParaAst(
        SentenciaAst inicializacion,
        ExpresionAst condicion,
        SentenciaAst actualizacion,
        List<SentenciaAst> cuerpo,
        PosicionFuente posicion
) implements SentenciaAst {

    public ParaAst {
        Objects.requireNonNull(
                inicializacion,
                "La inicializacion es obligatoria"
        );

        Objects.requireNonNull(
                condicion,
                "La condicion es obligatoria"
        );

        Objects.requireNonNull(
                actualizacion,
                "La actualizacion es obligatoria"
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