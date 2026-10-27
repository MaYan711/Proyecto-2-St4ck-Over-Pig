package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.zetariano.ZAst;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EnlacesZ {

    private final Map<ZAst.Llamada, String> llamadas;
    private final Map<ZAst.NuevoObjeto, String> constructores;
    private final Map<ZAst.Expresion, String> tiposExpresion;

    public EnlacesZ() {
        llamadas = new IdentityHashMap<>();
        constructores = new IdentityHashMap<>();
        tiposExpresion = new IdentityHashMap<>();
    }

    public void registrarLlamada(
            ZAst.Llamada llamada,
            String destino
    ) {
        Objects.requireNonNull(llamada);
        Objects.requireNonNull(destino);

        llamadas.put(
                llamada,
                destino
        );
    }

    public void registrarConstructor(
            ZAst.NuevoObjeto nuevo,
            String destino
    ) {
        Objects.requireNonNull(nuevo);
        Objects.requireNonNull(destino);

        constructores.put(
                nuevo,
                destino
        );
    }

    public Optional<String> buscarLlamada(
            ZAst.Llamada llamada
    ) {
        return Optional.ofNullable(
                llamadas.get(
                        llamada
                )
        );
    }

    public void registrarTipo(
            ZAst.Expresion expresion,
            String tipo
    ) {
        Objects.requireNonNull(expresion);
        Objects.requireNonNull(tipo);

        tiposExpresion.put(
                expresion,
                tipo
        );
    }

    public Optional<String> buscarTipo(
            ZAst.Expresion expresion
    ) {
        return Optional.ofNullable(
                tiposExpresion.get(
                        expresion
                )
        );
    }

    public Optional<String> buscarConstructor(
            ZAst.NuevoObjeto nuevo
    ) {
        return Optional.ofNullable(
                constructores.get(
                        nuevo
                )
        );
    }
}
