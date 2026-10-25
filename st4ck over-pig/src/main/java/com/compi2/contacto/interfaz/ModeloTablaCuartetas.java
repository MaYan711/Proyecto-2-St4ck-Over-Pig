package com.compi2.contacto.interfaz;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Objects;

public final class ModeloTablaCuartetas
        extends AbstractTableModel {

    private static final String[] COLUMNAS = {
            "#",
            "Operador",
            "Argumento 1",
            "Argumento 2",
            "Resultado"
    };

    private List<Cuarteta> cuartetas;

    public ModeloTablaCuartetas() {
        cuartetas = List.of();
    }

    public void mostrar(
            ProgramaIntermedio programa
    ) {
        Objects.requireNonNull(
                programa,
                "El programa intermedio es obligatorio"
        );

        cuartetas = List.copyOf(
                programa.cuartetas()
        );

        fireTableDataChanged();
    }

    public void limpiar() {
        cuartetas = List.of();
        fireTableDataChanged();
    }

    public Cuarteta cuartetaEn(
            int fila
    ) {
        if (fila < 0
                || fila >= cuartetas.size()) {

            throw new IndexOutOfBoundsException(
                    "Fila invalida: " + fila
            );
        }

        return cuartetas.get(
                fila
        );
    }

    @Override
    public int getRowCount() {
        return cuartetas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(
            int columna
    ) {
        return COLUMNAS[columna];
    }

    @Override
    public Class<?> getColumnClass(
            int columna
    ) {
        return columna == 0
                ? Integer.class
                : String.class;
    }

    @Override
    public boolean isCellEditable(
            int fila,
            int columna
    ) {
        return false;
    }

    @Override
    public Object getValueAt(
            int fila,
            int columna
    ) {
        Cuarteta cuarteta =
                cuartetas.get(
                        fila
                );

        return switch (columna) {
            case 0 ->
                    fila + 1;

            case 1 ->
                    cuarteta.operador()
                            .name();

            case 2 ->
                    texto(
                            cuarteta.argumento1()
                    );

            case 3 ->
                    texto(
                            cuarteta.argumento2()
                    );

            case 4 ->
                    texto(
                            cuarteta.resultado()
                    );

            default ->
                    throw new IndexOutOfBoundsException(
                            "Columna invalida: "
                                    + columna
                    );
        };
    }

    private String texto(
            String valor
    ) {
        return valor == null
                ? ""
                : valor;
    }
}