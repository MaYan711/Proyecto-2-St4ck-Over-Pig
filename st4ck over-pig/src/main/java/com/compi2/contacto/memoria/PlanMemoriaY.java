package com.compi2.contacto.memoria;

import com.compi2.contacto.ast.NodoAst;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PlanMemoriaY {

    private final Map<String, MarcoStackY> marcos;
    private final List<LayoutEstructuraY> estructuras;

    private final IdentityHashMap<NodoAst, SlotStackY>
            slotsPorNodo;

    public PlanMemoriaY() {
        marcos = new LinkedHashMap<>();
        estructuras = new ArrayList<>();
        slotsPorNodo = new IdentityHashMap<>();
    }

    public List<MarcoStackY> marcos() {
        return List.copyOf(
                marcos.values()
        );
    }

    public Optional<MarcoStackY> marco(
            String funcion
    ) {
        return Optional.ofNullable(
                marcos.get(funcion)
        );
    }

    public List<LayoutEstructuraY> estructuras() {
        return List.copyOf(estructuras);
    }

    public Optional<LayoutEstructuraY> estructuraGlobal(
            String nombre
    ) {
        return estructuras.stream()
                .filter(
                        estructura ->
                                estructura.ambito()
                                        .equals("global")
                                        && estructura.nombre()
                                        .equals(nombre)
                )
                .findFirst();
    }

    public Optional<SlotStackY> slotDe(
            NodoAst nodo
    ) {
        Objects.requireNonNull(
                nodo,
                "El nodo es obligatorio"
        );

        return Optional.ofNullable(
                slotsPorNodo.get(nodo)
        );
    }

    void agregarMarco(
            MarcoStackY marco
    ) {
        Objects.requireNonNull(
                marco,
                "El marco es obligatorio"
        );

        if (marcos.containsKey(
                marco.funcion()
        )) {
            throw new IllegalStateException(
                    "Ya existe un marco para la funcion "
                            + marco.funcion()
            );
        }

        marcos.put(
                marco.funcion(),
                marco
        );
    }

    void agregarEstructura(
            LayoutEstructuraY estructura
    ) {
        estructuras.add(
                Objects.requireNonNull(
                        estructura,
                        "El layout es obligatorio"
                )
        );
    }

    void asociar(
            NodoAst nodo,
            SlotStackY slot
    ) {
        slotsPorNodo.put(
                Objects.requireNonNull(
                        nodo,
                        "El nodo es obligatorio"
                ),
                Objects.requireNonNull(
                        slot,
                        "El slot es obligatorio"
                )
        );
    }
}