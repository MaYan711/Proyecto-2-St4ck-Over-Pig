package com.compi2.contacto.editor;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Font;

public final class NumeradorLineas extends JTextArea {

    private final EditorCodigo editor;

    public NumeradorLineas(EditorCodigo editor) {
        this.editor = editor;
        setEditable(false);
        setFocusable(false);
        setFont(new Font("Monospaced", Font.PLAIN, 15));
        setBackground(new Color(243, 244, 246));
        setForeground(new Color(107, 114, 128));
        setMargin(new java.awt.Insets(10, 8, 10, 8));

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evento) {
                actualizar();
            }

            @Override
            public void removeUpdate(DocumentEvent evento) {
                actualizar();
            }

            @Override
            public void changedUpdate(DocumentEvent evento) {
                // Los estilos no cambian la cantidad de lineas.
            }
        });

        actualizar();
    }

    private void actualizar() {
        int lineas = Math.max(1, editor.getDocument()
                .getDefaultRootElement()
                .getElementCount());

        StringBuilder texto = new StringBuilder();
        for (int linea = 1; linea <= lineas; linea++) {
            texto.append(linea).append(System.lineSeparator());
        }
        setText(texto.toString());
    }
}

