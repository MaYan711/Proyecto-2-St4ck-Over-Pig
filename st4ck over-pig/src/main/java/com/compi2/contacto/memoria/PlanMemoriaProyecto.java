package com.compi2.contacto.memoria;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record PlanMemoriaProyecto(
        Map<String, MarcoStackProyecto> marcos,
        Map<String, LayoutHeapProyecto> layoutsHeap,
        Map<String, String> padresZ
) {

    public PlanMemoriaProyecto {
        marcos = Map.copyOf(
                Objects.requireNonNull(marcos)
        );

        layoutsHeap = Map.copyOf(
                Objects.requireNonNull(layoutsHeap)
        );
        padresZ = Map.copyOf(Objects.requireNonNull(padresZ));
    }

    public PlanMemoriaProyecto(
            Map<String, MarcoStackProyecto> marcos,
            Map<String, LayoutHeapProyecto> layoutsHeap
    ) {
        this(marcos, layoutsHeap, Map.of());
    }

    public Optional<MarcoStackProyecto> marco(
            String nombre
    ) {
        return Optional.ofNullable(
                marcos.get(nombre)
        );
    }

    public Optional<LayoutHeapProyecto> layout(
            String nombre
    ) {
        return Optional.ofNullable(
                layoutsHeap.get(nombre)
        );
    }
}