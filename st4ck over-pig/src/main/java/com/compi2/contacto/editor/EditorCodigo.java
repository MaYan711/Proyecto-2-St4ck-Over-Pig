package com.compi2.contacto.editor;

import com.compi2.contacto.proyecto.LenguajeFuente;

import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.nio.file.Path;
import java.util.Objects;

public final class EditorCodigo extends JTextPane {

    private final Path ruta;
    private final LenguajeFuente lenguaje;
    private final ColoreadorAntlr coloreador;
    private final Timer temporizador;
    private boolean coloreando;
    private boolean modificado;

    public EditorCodigo(
            Path ruta,
            LenguajeFuente lenguaje,
            String contenido
    ) {
        super(new DefaultStyledDocument());
        this.ruta = Objects.requireNonNull(ruta);
        this.lenguaje = Objects.requireNonNull(lenguaje);
        this.coloreador = new ColoreadorAntlr();

        setFont(new Font("Monospaced", Font.PLAIN, 15));
        setBackground(Color.WHITE);
        setForeground(new Color(31, 41, 55));
        setCaretColor(new Color(37, 99, 235));
        setMargin(new java.awt.Insets(10, 12, 10, 12));
        setText(contenido == null ? "" : contenido);

        temporizador = new Timer(150, evento -> colorearAhora());
        temporizador.setRepeats(false);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evento) {
                cambioTexto();
            }

            @Override
            public void removeUpdate(DocumentEvent evento) {
                cambioTexto();
            }

            @Override
            public void changedUpdate(DocumentEvent evento) {
                // Los estilos no cambian el contenido.
            }
        });

        modificado = false;
        colorearAhora();
    }

    public Path ruta() {
        return ruta;
    }

    public LenguajeFuente lenguaje() {
        return lenguaje;
    }

    public boolean modificado() {
        return modificado;
    }

    public void marcarGuardado() {
        modificado = false;
    }

    public void colorearAhora() {
        if (coloreando) {
            return;
        }

        temporizador.stop();
        coloreando = true;

        try {
            StyledDocument documento = getStyledDocument();
            String texto = documento.getText(0, documento.getLength());
            coloreador.colorear(documento, texto, lenguaje);
        } catch (BadLocationException excepcion) {
            throw new IllegalStateException(
                    "No se pudo colorear el archivo",
                    excepcion
            );
        } finally {
            coloreando = false;
        }
    }

    private void cambioTexto() {
        if (!coloreando) {
            modificado = true;
            temporizador.restart();
        }
    }
}

