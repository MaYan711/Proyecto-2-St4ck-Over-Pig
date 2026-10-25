package com.compi2.contacto.ast;

import java.util.Objects;

public record LiteralAst(
        String lexema,
        Tipo tipo,
        PosicionFuente posicion
) implements ExpresionAst {

    public LiteralAst {
        Objects.requireNonNull(lexema, "El lexema es obligatorio");
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public enum Tipo {
        ENTERO,
        DECIMAL,
        CADENA,
        CARACTER,
        BOOLEANO
    }
}