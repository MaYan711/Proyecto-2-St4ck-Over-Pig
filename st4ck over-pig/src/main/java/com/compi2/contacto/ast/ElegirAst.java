package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record ElegirAst(
        ExpresionAst expresion,
        List<Caso> casos,
        List<SentenciaAst> siempre,
        PosicionFuente posicion
) implements SentenciaAst {

    public ElegirAst {
        Objects.requireNonNull(
                expresion,
                "La expresion es obligatoria"
        );

        casos = List.copyOf(
                Objects.requireNonNull(
                        casos,
                        "Los casos son obligatorios"
                )
        );

        siempre = List.copyOf(
                Objects.requireNonNull(
                        siempre,
                        "El caso siempre es obligatorio"
                )
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public record Caso(
            ExpresionAst valor,
            List<SentenciaAst> cuerpo,
            PosicionFuente posicion
    ) implements NodoAst {

        public Caso {
            Objects.requireNonNull(valor, "El valor es obligatorio");

            cuerpo = List.copyOf(
                    Objects.requireNonNull(
                            cuerpo,
                            "El cuerpo es obligatorio"
                    )
            );

            Objects.requireNonNull(
                    posicion,
                    "La posicion es obligatoria"
            );
        }
    }
}