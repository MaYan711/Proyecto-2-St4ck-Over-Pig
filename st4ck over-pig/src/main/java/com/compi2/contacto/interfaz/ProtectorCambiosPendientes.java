package com.compi2.contacto.interfaz;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.swing.JOptionPane;

final class ProtectorCambiosPendientes {
    private ProtectorCambiosPendientes() {
    }

    static boolean continuar(boolean hayCambios, int opcion, BooleanSupplier guardarTodos) {
        Objects.requireNonNull(guardarTodos, "La accion de guardado es obligatoria");
        if (!hayCambios) {
            return true;
        }
        return switch (opcion) {
            case JOptionPane.YES_OPTION -> guardarTodos.getAsBoolean();
            case JOptionPane.NO_OPTION -> true;
            default -> false;
        };
    }
}
