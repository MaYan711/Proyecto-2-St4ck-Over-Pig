package com.compi2.contacto.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class ProgramaIntermedio {

    private static final ThreadLocal<Integer> NIVEL_NOMBRES_PROTEGIDOS =
            ThreadLocal.withInitial(
                    () -> 0
            );

    private final List<Cuarteta> cuartetas;
    private final boolean nombresProtegidos;
    private int siguienteTemporal;
    private int siguienteEtiqueta;

    public ProgramaIntermedio() {
        this.cuartetas = new ArrayList<>();
        this.nombresProtegidos =
                NIVEL_NOMBRES_PROTEGIDOS.get() > 0;
    }

    static <T> T conNombresProtegidos(
            Supplier<T> accion
    ) {
        Objects.requireNonNull(
                accion,
                "La accion es obligatoria"
        );

        int nivelAnterior =
                NIVEL_NOMBRES_PROTEGIDOS.get();

        NIVEL_NOMBRES_PROTEGIDOS.set(
                nivelAnterior + 1
        );

        try {
            return accion.get();
        } finally {
            if (nivelAnterior == 0) {
                NIVEL_NOMBRES_PROTEGIDOS.remove();
            } else {
                NIVEL_NOMBRES_PROTEGIDOS.set(
                        nivelAnterior
                );
            }
        }
    }

    public void agregar(Cuarteta cuarteta) {
        if (cuarteta == null) {
            throw new IllegalArgumentException(
                    "La cuarteta es obligatoria"
            );
        }

        cuartetas.add(
                cuarteta
        );
    }

    public List<Cuarteta> cuartetas() {
        return List.copyOf(
                cuartetas
        );
    }

    public String nuevoTemporal() {
        String prefijo =
                nombresProtegidos
                        ? "$t"
                        : "t";

        return prefijo
                + siguienteTemporal++;
    }

    public String nuevaEtiqueta() {
        String prefijo =
                nombresProtegidos
                        ? "$L"
                        : "L";

        return prefijo
                + siguienteEtiqueta++;
    }
}
