package com.compi2.contacto.ast;

import java.util.Objects;

public record ParametroAst(
        String nombre,
        TipoAst tipo,
        Modo modo,
        PosicionFuente posicion
) implements NodoAst {

    public ParametroAst {
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        Objects.requireNonNull(modo, "El modo es obligatorio");
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public enum Modo {
        VALOR,
        REFERENCIA_ARREGLO,
        REFERENCIA_ESTRUCTURA
    }
}