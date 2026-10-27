package com.compi2.contacto.analisis;

import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ResultadoAnalisisArchivo(
        ArchivoFuente archivo,
        List<Diagnostico> diagnosticos,
        Optional<ProgramaAst> ast
) {

    public ResultadoAnalisisArchivo {
        Objects.requireNonNull(
                archivo,
                "El archivo es obligatorio"
        );

        diagnosticos = List.copyOf(
                Objects.requireNonNull(
                        diagnosticos,
                        "Los diagnosticos son obligatorios"
                )
        );

        Objects.requireNonNull(
                ast,
                "El AST es obligatorio"
        );
    }

    public ResultadoAnalisisArchivo(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        this(
                archivo,
                diagnosticos,
                Optional.empty()
        );
    }

    public boolean esValido() {
        return diagnosticos.stream()
                .noneMatch(Diagnostico::esError);
    }

    public boolean tieneAst() {
        return ast.isPresent();
    }
}