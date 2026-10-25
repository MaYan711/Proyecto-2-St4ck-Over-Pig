package com.compi2.contacto.memoria;

import com.compi2.contacto.ast.PosicionFuente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LayoutHeapProyecto(
        String nombre,
        Clase clase,
        List<Campo> campos
) {

    public LayoutHeapProyecto {
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(clase);
        campos = List.copyOf(
                Objects.requireNonNull(campos)
        );
    }

    public int tamano() {
        return campos.size();
    }

    public Optional<Campo> buscarCampo(
            String nombre
    ) {
        for (int i = campos.size() - 1; i >= 0; i--) {
            Campo campo = campos.get(i);
            if (campo.nombre().equals(nombre)) {
                return Optional.of(campo);
            }
        }
        return Optional.empty();
    }

    public enum Clase {
        ESTRUCTURA_Y,
        OBJETO_Z
    }

    public record Campo(
            String nombre,
            String tipo,
            int desplazamiento,
            PosicionFuente posicion
    ) {

        public Campo {
            Objects.requireNonNull(nombre);
            Objects.requireNonNull(tipo);
            Objects.requireNonNull(posicion);

            if (desplazamiento < 0) {
                throw new IllegalArgumentException(
                        "El desplazamiento no puede ser negativo"
                );
            }
        }
    }
}