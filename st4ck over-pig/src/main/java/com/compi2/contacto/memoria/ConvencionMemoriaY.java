package com.compi2.contacto.memoria;

public final class ConvencionMemoriaY {

    public static final int OFFSET_RETORNO = 0;

    public static final int OFFSET_CANTIDAD_DIMENSIONES = 0;

    public static final int OFFSET_PRIMERA_DIMENSION = 1;

    private ConvencionMemoriaY() {
    }

    public static int inicioDatosArreglo(
            int dimensiones
    ) {
        if (dimensiones <= 0) {
            throw new IllegalArgumentException(
                    "Un arreglo debe tener al menos una dimension"
            );
        }

        return 1 + dimensiones;
    }
}