package com.compi2.contacto.interfaz;

import com.compi2.contacto.editor.EditorCodigo;
import com.compi2.contacto.editor.NumeradorLineas;
import com.compi2.contacto.proyecto.ArchivoFuente;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public final class PanelEditor extends JPanel {

    private final EditorCodigo editor;

    public PanelEditor(ArchivoFuente archivo) {
        super(new BorderLayout());
        editor = new EditorCodigo(
                archivo.ruta(),
                archivo.lenguaje(),
                archivo.contenido()
        );

        JScrollPane desplazamiento = new JScrollPane(editor);
        desplazamiento.setRowHeaderView(new NumeradorLineas(editor));
        add(desplazamiento, BorderLayout.CENTER);
    }

    public EditorCodigo editor() {
        return editor;
    }
}

