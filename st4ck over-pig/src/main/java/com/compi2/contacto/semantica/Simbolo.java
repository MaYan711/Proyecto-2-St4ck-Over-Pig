package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.PosicionFuente;

import java.util.Objects;

public record Simbolo(
        String nombre,
        CategoriaSimbolo categoria,
        TipoDato tipo,
        String tipoDeclarado,
        PosicionFuente posicion
) {
    public Simbolo {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        Objects.requireNonNull(categoria, "La categoria es obligatoria");
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        tipoDeclarado = tipoDeclarado == null ? "" : tipoDeclarado;
        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }
}

