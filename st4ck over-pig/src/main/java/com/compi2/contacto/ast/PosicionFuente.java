package com.compi2.contacto.ast;

import java.nio.file.Path;

public record PosicionFuente(
        Path archivo,
        int linea,
        int columna
) {
    public PosicionFuente {
        if (linea < 1 || columna < 0) {
            throw new IllegalArgumentException("Posicion de fuente invalida");
        }
    }
}

