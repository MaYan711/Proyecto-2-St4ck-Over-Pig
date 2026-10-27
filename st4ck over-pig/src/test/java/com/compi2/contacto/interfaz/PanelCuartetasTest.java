package com.compi2.contacto.interfaz;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PanelCuartetasTest {

    @Test
    void recibeProgramaIntermedio() {
        ProgramaIntermedio programa =
                new ProgramaIntermedio();

        programa.agregar(
                new Cuarteta(
                        OperadorCuarteta.LLAMAR,
                        "sumar",
                        "2",
                        "pig_t0"
                )
        );

        PanelCuartetas panel =
                new PanelCuartetas();

        panel.mostrar(
                programa
        );

        assertEquals(
                1,
                panel.modelo()
                        .getRowCount()
        );

        assertEquals(
                "LLAMAR",
                panel.modelo()
                        .getValueAt(
                                0,
                                1
                        )
        );

        assertEquals(
                "sumar",
                panel.modelo()
                        .getValueAt(
                                0,
                                2
                        )
        );

        assertFalse(
                panel.tabla()
                        .isCellEditable(
                                0,
                                0
                        )
        );
    }
}