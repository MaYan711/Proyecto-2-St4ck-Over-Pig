package com.compi2.contacto.interfaz;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.nio.file.Path;

public final class RenderizadorArbolProyecto extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(
            JTree arbol,
            Object valor,
            boolean seleccionado,
            boolean expandido,
            boolean hoja,
            int fila,
            boolean tieneFoco
    ) {
        Component componente = super.getTreeCellRendererComponent(
                arbol,
                valor,
                seleccionado,
                expandido,
                hoja,
                fila,
                tieneFoco
        );

        if (valor instanceof DefaultMutableTreeNode nodo
                && nodo.getUserObject() instanceof Path ruta) {
            Path nombre = ruta.getFileName();
            setText(nombre == null ? ruta.toString() : nombre.toString());
            setToolTipText(ruta.toString());
        }

        return componente;
    }
}

