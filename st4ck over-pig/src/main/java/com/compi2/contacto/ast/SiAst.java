package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record SiAst(
        List<Rama> ramas,
        List<SentenciaAst> contrario,
        PosicionFuente posicion
) implements SentenciaAst {

    public SiAst {
        ramas = List.copyOf(
                Objects.requireNonNull(
                        ramas,
                        "Las ramas son obligatorias"
                )
        );

        contrario = List.copyOf(
                Objects.requireNonNull(
                        contrario,
                        "El bloque contrario es obligatorio"
                )
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public record Rama(
            ExpresionAst condicion,
            List<SentenciaAst> cuerpo,
            PosicionFuente posicion
    ) implements NodoAst {

        public Rama {
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

            Objects.requireNonNull(
                    posicion,
                    "La posicion es obligatoria"
            );
        }
    }
}