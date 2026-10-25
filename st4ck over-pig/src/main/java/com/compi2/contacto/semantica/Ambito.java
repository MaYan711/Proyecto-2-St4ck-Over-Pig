package com.compi2.contacto.semantica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Ambito {

    private final String nombre;
    private final Ambito padre;
    private final Map<String, Simbolo> simbolos;
    private final List<Ambito> hijos;

    public Ambito(String nombre, Ambito padre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        this.nombre = nombre;
        this.padre = padre;
        this.simbolos = new LinkedHashMap<>();
        this.hijos = new ArrayList<>();
    }

    public String nombre() {
        return nombre;
    }

    public Ambito padre() {
        return padre;
    }

    public Map<String, Simbolo> simbolos() {
        return Collections.unmodifiableMap(simbolos);
    }

    public List<Ambito> hijos() {
        return List.copyOf(hijos);
    }

    public boolean declarar(Simbolo simbolo) {
        if (simbolo == null || simbolos.containsKey(simbolo.nombre())) {
            return false;
        }

        simbolos.put(simbolo.nombre(), simbolo);
        return true;
    }

    public Optional<Simbolo> buscarLocal(String nombreSimbolo) {
        return Optional.ofNullable(simbolos.get(nombreSimbolo));
    }

    public Optional<Simbolo> buscar(String nombreSimbolo) {
        Ambito recorrido = this;

        while (recorrido != null) {
            Simbolo simbolo = recorrido.simbolos.get(nombreSimbolo);
            if (simbolo != null) {
                return Optional.of(simbolo);
            }
            recorrido = recorrido.padre;
        }

        return Optional.empty();
    }

    public Ambito crearHijo(String nombreHijo) {
        Ambito hijo = new Ambito(nombreHijo, this);
        hijos.add(hijo);
        return hijo;
    }
}

