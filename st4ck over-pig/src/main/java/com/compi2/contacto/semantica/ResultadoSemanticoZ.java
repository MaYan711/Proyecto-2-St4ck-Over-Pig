package com.compi2.contacto.semantica;

import com.compi2.contacto.errores.Diagnostico;

import java.util.List;
import java.util.Objects;

public record ResultadoSemanticoZ(
        TablaSimbolos tablaSimbolos,
        List<Diagnostico> diagnosticos
) {

    public ResultadoSemanticoZ {
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
    }

    public boolean esValido() {
        return diagnosticos.stream()
                .noneMatch(Diagnostico::esError);
    }
}