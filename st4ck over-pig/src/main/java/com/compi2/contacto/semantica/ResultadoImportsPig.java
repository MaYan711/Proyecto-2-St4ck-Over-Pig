package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.EstructuraAst;
import com.compi2.contacto.ast.FuncionAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.errores.Diagnostico;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ResultadoImportsPig(
        Map<String, EstructuraAst> estructurasY,
        Map<String, FuncionAst> funcionesY,
        Map<String, ZAst.Clase> clasesZ,
        List<Diagnostico> diagnosticos
) {

    public ResultadoImportsPig {
        estructurasY = Map.copyOf(
                Objects.requireNonNull(
                        estructurasY,
                        "Las estructuras son obligatorias"
                )
        );

        funcionesY = Map.copyOf(
                Objects.requireNonNull(
                        funcionesY,
                        "Las funciones son obligatorias"
                )
        );

        clasesZ = Map.copyOf(
                Objects.requireNonNull(
                        clasesZ,
                        "Las clases son obligatorias"
                )
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