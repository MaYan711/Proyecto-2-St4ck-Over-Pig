package com.compi2.contacto.interfaz;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModeloTablaCuartetasTest {

    @Test
    void muestraCuartetasDelPrograma() {
        ProgramaIntermedio programa =
                new ProgramaIntermedio();

        programa.agregar(
                new Cuarteta(
                        OperadorCuarteta.SUMAR,
                        "a",
                        "b",
                        "t0"
                )
        );

        programa.agregar(
                new Cuarteta(
                        OperadorCuarteta.ASIGNAR,
                        "t0",
                        null,
                        "resultado"
                )
        );

        ModeloTablaCuartetas modelo =
                new ModeloTablaCuartetas();

        modelo.mostrar(
                programa
        );

        assertEquals(
                2,
                modelo.getRowCount()
        );

        assertEquals(
                5,
                modelo.getColumnCount()
        );

        assertEquals(
                1,
                modelo.getValueAt(
                        0,
                        0
                )
        );

        assertEquals(
                "SUMAR",
                modelo.getValueAt(
                        0,
                        1
                )
        );

        assertEquals(
                "a",
                modelo.getValueAt(
                        0,
                        2
                )
        );

        assertEquals(
                "b",
                modelo.getValueAt(
                        0,
                        3
                )
        );

        assertEquals(
                "t0",
                modelo.getValueAt(
                        0,
                        4
                )
        );

        assertEquals(
                "-",
                modelo.getValueAt(
                        1,
                        3
                )
        );

        assertEquals(
                "resultado",
                modelo.getValueAt(
                        1,
                        4
                )
        );
    }

    @Test
    void permiteLimpiarResultados() {
        ProgramaIntermedio programa =
                new ProgramaIntermedio();

        programa.agregar(
                new Cuarteta(
                        OperadorCuarteta.IMPRIMIR,
                        "resultado",
                        null,
                        null
                )
        );

        ModeloTablaCuartetas modelo =
                new ModeloTablaCuartetas();

        modelo.mostrar(
                programa
        );

        assertEquals(
                1,
                modelo.getRowCount()
        );

        modelo.limpiar();

        assertEquals(
                0,
                modelo.getRowCount()
        );
    }
}