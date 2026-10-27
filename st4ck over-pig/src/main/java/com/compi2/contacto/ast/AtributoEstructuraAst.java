package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;

public record AtributoEstructuraAst(
        String nombre,
        TipoAst tipo,
        List<ExpresionAst> dimensiones,
        PosicionFuente posicion
) implements NodoAst {

    public AtributoEstructuraAst {
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        Objects.requireNonNull(tipo, "El tipo es obligatorio");

        dimensiones = List.copyOf(
                Objects.requireNonNull(
                        dimensiones,
                        "Las dimensiones son obligatorias"
                )
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public boolean esArreglo() {
        return !dimensiones.isEmpty();
    }
}