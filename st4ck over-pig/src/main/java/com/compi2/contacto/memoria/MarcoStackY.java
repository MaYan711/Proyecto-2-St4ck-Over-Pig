package com.compi2.contacto.memoria;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MarcoStackY {

    private final String funcion;
    private final List<SlotStackY> slots;

    private int siguienteDesplazamiento;

    public MarcoStackY(
            String funcion
    ) {
        if (funcion == null || funcion.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la funcion es obligatorio"
            );
        }

        this.funcion = funcion;
        this.slots = new ArrayList<>();
        this.siguienteDesplazamiento = 0;
    }

    public String funcion() {
        return funcion;
    }

    public List<SlotStackY> slots() {
        return List.copyOf(slots);
    }

    public int tamano() {
        return siguienteDesplazamiento;
    }

    public SlotStackY reservar(
            String nombre,
            ClaseSlotStackY clase,
            String tipoDeclarado,
            boolean referencia
    ) {
        Objects.requireNonNull(
                clase,
                "La clase es obligatoria"
        );

        SlotStackY slot =
                new SlotStackY(
                        nombre,
                        clase,
                        tipoDeclarado,
                        new DireccionMemoria(
                                SegmentoMemoria.STACK,
                                siguienteDesplazamiento
                        ),
                        referencia
                );

        slots.add(slot);

        siguienteDesplazamiento++;

        return slot;
    }

    public Optional<SlotStackY> primerSlot(
            String nombre
    ) {
        return slots.stream()
                .filter(
                        slot ->
                                slot.nombre()
                                        .equals(nombre)
                )
                .findFirst();
    }

    public List<SlotStackY> slots(
            String nombre
    ) {
        return slots.stream()
                .filter(
                        slot ->
                                slot.nombre()
                                        .equals(nombre)
                )
                .toList();
    }

    public Optional<SlotStackY> retorno() {
        return slots.stream()
                .filter(
                        slot ->
                                slot.clase()
                                        == ClaseSlotStackY.RETORNO
                )
                .findFirst();
    }
}