package com.compi2.contacto.memoria;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LayoutEstructuraY {

    private final String nombre;
    private final String ambito;
    private final List<CampoHeapY> campos;

    public LayoutEstructuraY(
            String nombre,
            String ambito,
            List<CampoHeapY> campos
    ) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio"
            );
        }

        if (ambito == null || ambito.isBlank()) {
            throw new IllegalArgumentException(
                    "El ambito es obligatorio"
            );
        }

        this.nombre = nombre;
        this.ambito = ambito;

        this.campos = List.copyOf(
                Objects.requireNonNull(
                        campos,
                        "Los campos son obligatorios"
                )
        );
    }

    public String nombre() {
        return nombre;
    }

    public String ambito() {
        return ambito;
    }

    public List<CampoHeapY> campos() {
        return campos;
    }

    public int tamanoCeldas() {
        return campos.size();
    }

    public Optional<CampoHeapY> campo(
            String nombreCampo
    ) {
        return campos.stream()
                .filter(
                        campo ->
                                campo.nombre()
                                        .equals(nombreCampo)
                )
                .findFirst();
    }

    @Override
    public String toString() {
        return ambito
                + "::"
                + nombre
                + " ["
                + tamanoCeldas()
                + " celdas]";
    }
}
