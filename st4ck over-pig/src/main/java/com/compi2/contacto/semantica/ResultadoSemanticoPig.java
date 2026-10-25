package com.compi2.contacto.semantica;

import com.compi2.contacto.errores.Diagnostico;

import java.util.List;
import java.util.Objects;

public record ResultadoSemanticoPig(
        TablaSimbolos tablaSimbolos,
        List<Diagnostico> diagnosticos,
        EnlacesPig enlaces
) {

    public ResultadoSemanticoPig {
        Objects.requireNonNull(
                tablaSimbolos,
                "La tabla de simbolos es obligatoria"
        );

        diagnosticos = List.copyOf(
                Objects.requireNonNull(
                        diagnosticos,
                        "Los diagnosticos son obligatorios"
                )
        );

        Objects.requireNonNull(
                enlaces,
                "Los enlaces son obligatorios"
        );
    }

    public ResultadoSemanticoPig(
            TablaSimbolos tablaSimbolos,
            List<Diagnostico> diagnosticos
    ) {
        this(
                tablaSimbolos,
                diagnosticos,
                new EnlacesPig()
        );
    }

    public boolean esValido() {
        return diagnosticos.stream()
                .noneMatch(
                        Diagnostico::esError
                );
    }
}