package com.compi2.contacto.interfaz;

import com.compi2.contacto.errores.Diagnostico;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public final class ModeloTablaDiagnosticos extends AbstractTableModel {

    private static final String[] COLUMNAS = {
            "Severidad",
            "Tipo",
            "Archivo",
            "Linea",
            "Columna",
            "Mensaje"
    };

    private List<Diagnostico> diagnosticos = List.of();

    public void actualizar(List<Diagnostico> nuevosDiagnosticos) {
        diagnosticos = List.copyOf(nuevosDiagnosticos);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return diagnosticos.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Diagnostico diagnostico = diagnosticos.get(fila);

        return switch (columna) {
            case 0 -> diagnostico.severidad();
            case 1 -> diagnostico.tipo();
            case 2 -> diagnostico.archivo() == null
                    ? "-"
                    : diagnostico.archivo().getFileName();
            case 3 -> diagnostico.linea();
            case 4 -> diagnostico.columna();
            case 5 -> diagnostico.mensaje();
            default -> "";
        };
    }
}

