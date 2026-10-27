package com.compi2.contacto.interfaz;

import com.compi2.contacto.ir.ProgramaIntermedio;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;

public final class PanelCuartetas
        extends JPanel {

    private final ModeloTablaCuartetas modelo;
    private final JTable tabla;

    public PanelCuartetas() {
        super(
                new BorderLayout()
        );

        modelo =
                new ModeloTablaCuartetas();

        tabla =
                new JTable(
                        modelo
                );

        configurarTabla();

        JScrollPane scroll =
                new JScrollPane(
                        tabla
                );

        scroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        add(
                scroll,
                BorderLayout.CENTER
        );
    }

    public void mostrar(
            ProgramaIntermedio programa
    ) {
        Objects.requireNonNull(
                programa,
                "El programa intermedio es obligatorio"
        );

        modelo.mostrar(
                programa
        );
    }

    public void limpiar() {
        modelo.limpiar();
    }

    public ModeloTablaCuartetas modelo() {
        return modelo;
    }

    public JTable tabla() {
        return tabla;
    }

    private void configurarTabla() {
        tabla.setAutoCreateRowSorter(
                true
        );

        tabla.setFillsViewportHeight(
                true
        );

        tabla.setRowSelectionAllowed(
                true
        );

        tabla.setColumnSelectionAllowed(
                false
        );

        tabla.getTableHeader()
                .setReorderingAllowed(
                        false
                );

        tabla.setPreferredScrollableViewportSize(
                new Dimension(
                        800,
                        180
                )
        );

        tabla.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(
                        45
                );

        tabla.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(
                        180
                );

        tabla.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(
                        220
                );

        tabla.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(
                        220
                );

        tabla.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(
                        220
                );
    }
}