package com.compi2.contacto.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record FuncionAst(
        String nombre,
        List<ParametroAst> parametros,
        Optional<TipoAst> retorno,
        List<SentenciaAst> cuerpo,
        PosicionFuente posicion
) implements NodoAst {

    public FuncionAst {
        Objects.requireNonNull(nombre, "El nombre es obligatorio");

        parametros = List.copyOf(
                Objects.requireNonNull(
                        parametros,
                        "Los parametros son obligatorios"
                )
        );

        Objects.requireNonNull(retorno, "El retorno es obligatorio");

        cuerpo = List.copyOf(
                Objects.requireNonNull(
                        cuerpo,
                        "El cuerpo es obligatorio"
                )
        );

        Objects.requireNonNull(posicion, "La posicion es obligatoria");
    }

    public boolean tieneRetorno() {
        return retorno.isPresent();
    }
}