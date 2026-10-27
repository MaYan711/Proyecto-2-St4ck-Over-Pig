package com.compi2.contacto;

import com.compi2.contacto.interfaz.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class Aplicacion {

    private Aplicacion() {
    }

    public static void main(String[] args) {
        configurarApariencia();
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }

    private static void configurarApariencia() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException
                 | UnsupportedLookAndFeelException excepcion) {
            System.err.println(
                    "No se pudo aplicar la apariencia del sistema: "
                            + excepcion.getMessage()
            );
        }
    }
}
