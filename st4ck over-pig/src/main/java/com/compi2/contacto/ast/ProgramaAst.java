package com.compi2.contacto.ast;

import com.compi2.contacto.proyecto.LenguajeFuente;

import java.util.List;
import java.util.Objects;

public record ProgramaAst(
        LenguajeFuente lenguaje,
        List<NodoAst> elementos,
        PosicionFuente posicion
) implements NodoAst {

    public ProgramaAst {
        Objects.requireNonNull(lenguaje, "El lenguaje es obligatorio");
        elementos = List.copyOf(
                Objects.requireNonNull(elementos, "Los elementos son obligatorios")
        );
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }
}

