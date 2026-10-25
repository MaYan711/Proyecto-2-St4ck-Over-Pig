package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeclaracionVariableAst(
        String nombre,
        TipoAst tipo,
        List<ExpresionAst> dimensiones,
        Optional<ExpresionAst> inicializador,
        PosicionFuente posicion
) implements SentenciaAst {

    public DeclaracionVariableAst {
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        Objects.requireNonNull(tipo, "El tipo es obligatorio");

        dimensiones = List.copyOf(
                Objects.requireNonNull(
                        dimensiones,
                        "Las dimensiones son obligatorias"
                )
        );

        Objects.requireNonNull(
                inicializador,
                "El inicializador es obligatorio"
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public boolean esArreglo() {
        return !dimensiones.isEmpty();
    }
}