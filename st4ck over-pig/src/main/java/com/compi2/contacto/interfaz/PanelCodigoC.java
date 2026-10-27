package com.compi2.contacto.interfaz;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Objects;

public final class PanelCodigoC extends JPanel {

    private final JTextArea areaCodigo;

    public PanelCodigoC() {
        super(
                new BorderLayout()
        );

        areaCodigo =
                new JTextArea();

        areaCodigo.setEditable(
                false
        );

        areaCodigo.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14
                )
        );

        areaCodigo.setTabSize(
                4
        );

        areaCodigo.setLineWrap(
                false
        );

        JScrollPane scroll =
                new JScrollPane(
                        areaCodigo
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
            String codigo
    ) {
        areaCodigo.setText(
                Objects.requireNonNull(
                        codigo
                )
        );

        areaCodigo.setCaretPosition(
                0
        );
    }

    public void limpiar() {
        areaCodigo.setText(
                ""
        );
    }
}