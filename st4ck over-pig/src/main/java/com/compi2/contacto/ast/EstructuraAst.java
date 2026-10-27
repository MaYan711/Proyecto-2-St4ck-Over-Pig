package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record EstructuraAst(
        String nombre,
        List<AtributoEstructuraAst> atributos,
        PosicionFuente posicion
) implements SentenciaAst {

    public EstructuraAst {
        Objects.requireNonNull(nombre, "El nombre es obligatorio");

        atributos = List.copyOf(
                Objects.requireNonNull(
                        atributos,
                        "Los atributos son obligatorios"
                )
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }
}