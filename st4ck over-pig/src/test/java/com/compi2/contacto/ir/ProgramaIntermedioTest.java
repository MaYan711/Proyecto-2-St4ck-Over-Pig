package com.compi2.contacto.ir;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramaIntermedioTest {

    @Test
    void generaTemporalesYEtiquetasConsecutivos() {
        ProgramaIntermedio programa = new ProgramaIntermedio();

        assertEquals("t0", programa.nuevoTemporal());
        assertEquals("t1", programa.nuevoTemporal());
        assertEquals("L0", programa.nuevaEtiqueta());
        assertEquals("L1", programa.nuevaEtiqueta());
    }

    @Test
    void normalizaCamposVaciosDeCuarteta() {
        Cuarteta cuarteta = new Cuarteta(
                OperadorCuarteta.ETIQUETA,
                null,
                "",
                "L0"
        );

        assertEquals("-", cuarteta.argumento1());
        assertEquals("-", cuarteta.argumento2());
        assertEquals("L0", cuarteta.resultado());
    }
}

