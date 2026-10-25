package com.compi2.contacto.memoria;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MarcoStackProyecto(
        String nombre,
        List<SlotStackProyecto> slots
) {

    public MarcoStackProyecto {
        Objects.requireNonNull(nombre);
        slots = List.copyOf(
                Objects.requireNonNull(slots)
        );
    }

    public int tamano() {
        return slots.size();
    }

    public Optional<SlotStackProyecto> buscar(
            String nombre
    ) {
        for (int indice = slots.size() - 1;
             indice >= 0;
             indice--) {

            SlotStackProyecto slot =
                    slots.get(indice);

            if (slot.nombre()
                    .equals(nombre)) {

                return Optional.of(
                        slot
                );
            }
        }

        return Optional.empty();
    }

    public Optional<SlotStackProyecto> retorno() {
        return slots.stream()
                .filter(
                        slot ->
                                slot.clase()
                                        == SlotStackProyecto.Clase.RETORNO
                )
                .findFirst();
    }

    public Optional<SlotStackProyecto> thisSlot() {
        return slots.stream()
                .filter(
                        slot ->
                                slot.clase()
                                        == SlotStackProyecto.Clase.THIS
                )
                .findFirst();
    }
}