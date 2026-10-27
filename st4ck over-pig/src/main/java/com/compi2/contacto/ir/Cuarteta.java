package com.compi2.contacto.ir;

import java.util.Objects;

public record Cuarteta(
        OperadorCuarteta operador,
        String argumento1,
        String argumento2,
        String resultado
) {
    public Cuarteta {
        Objects.requireNonNull(operador, "El operador es obligatorio");
        argumento1 = normalizar(argumento1);
        argumento2 = normalizar(argumento2);
        resultado = normalizar(resultado);
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }
}

