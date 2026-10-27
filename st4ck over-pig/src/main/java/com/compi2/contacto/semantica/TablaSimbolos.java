package com.compi2.contacto.semantica;

import java.util.Objects;
import java.util.Optional;

public final class TablaSimbolos {

    private final Ambito global;
    private Ambito actual;

    public TablaSimbolos() {
        this.global = new Ambito("proyecto", null);
        this.actual = global;
    }

    public Ambito global() {
        return global;
    }

    public Ambito actual() {
        return actual;
    }

    public boolean declarar(Simbolo simbolo) {
        return actual.declarar(simbolo);
    }

    public Optional<Simbolo> buscar(String nombre) {
        return actual.buscar(nombre);
    }

    public Ambito entrar(String nombre) {
        actual = actual.crearHijo(nombre);
        return actual;
    }

    public void salir() {
        if (actual.padre() == null) {
            throw new IllegalStateException(
                    "No se puede salir del ambito global"
            );
        }
        actual = Objects.requireNonNull(actual.padre());
    }
}

