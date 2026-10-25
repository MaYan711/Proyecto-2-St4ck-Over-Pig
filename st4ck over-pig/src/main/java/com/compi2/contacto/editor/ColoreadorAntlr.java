package com.compi2.contacto.editor;

import com.compi2.contacto.antlr.piglatin.PigLatinLexer;
import com.compi2.contacto.antlr.y.YLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;

public final class ColoreadorAntlr {

    private final SimpleAttributeSet normal;
    private final SimpleAttributeSet seccion;
    private final SimpleAttributeSet reservada;
    private final SimpleAttributeSet tipo;
    private final SimpleAttributeSet booleano;
    private final SimpleAttributeSet numero;
    private final SimpleAttributeSet cadena;
    private final SimpleAttributeSet comentario;

    public ColoreadorAntlr() {
        normal = estilo(new Color(31, 41, 55), false, false);
        seccion = estilo(new Color(30, 64, 175), true, false);
        reservada = estilo(new Color(126, 34, 206), true, false);
        tipo = estilo(new Color(15, 118, 110), true, false);
        booleano = estilo(new Color(194, 65, 12), true, false);
        numero = estilo(new Color(185, 28, 28), false, false);
        cadena = estilo(new Color(21, 128, 61), false, false);
        comentario = estilo(new Color(107, 114, 128), false, true);
    }

    public void colorear(
            StyledDocument documento,
            String texto,
            LenguajeFuente lenguaje
    ) {
        documento.setCharacterAttributes(
                0,
                texto.length(),
                normal,
                true
        );

        Lexer lexer = crearLexer(texto, lenguaje);
        lexer.removeErrorListeners();

        Token token = lexer.nextToken();
        while (token.getType() != Token.EOF) {
            aplicarToken(documento, texto, lexer, token);
            token = lexer.nextToken();
        }
    }

    private Lexer crearLexer(String texto, LenguajeFuente lenguaje) {
        return switch (lenguaje) {
            case Y -> new YLexer(CharStreams.fromString(texto));
            case ZETARIANO -> new ZetarianoLexer(CharStreams.fromString(texto));
            case PIG_LATIN -> new PigLatinLexer(CharStreams.fromString(texto));
        };
    }

    private void aplicarToken(
            StyledDocument documento,
            String texto,
            Lexer lexer,
            Token token
    ) {
        int inicio = token.getStartIndex();
        int longitud = token.getStopIndex() - inicio + 1;

        if (inicio < 0
                || longitud <= 0
                || inicio + longitud > texto.length()) {
            return;
        }

        String nombre = lexer.getVocabulary()
                .getSymbolicName(token.getType());

        documento.setCharacterAttributes(
                inicio,
                longitud,
                estiloPara(nombre),
                true
        );
    }

    private SimpleAttributeSet estiloPara(String nombre) {
        if (nombre == null) {
            return normal;
        }

        if (nombre.startsWith("SECCION_") || nombre.equals("FIN_PROGRAMA")) {
            return seccion;
        }
        if (nombre.startsWith("KW_")) {
            return reservada;
        }
        if (nombre.startsWith("TYPE_")) {
            return tipo;
        }
        if (nombre.startsWith("BOOL_") || nombre.equals("NULL_VALUE")) {
            return booleano;
        }
        if (nombre.equals("ENTERO") || nombre.equals("DECIMAL")) {
            return numero;
        }
        if (nombre.equals("CADENA") || nombre.equals("CARACTER")) {
            return cadena;
        }
        if (nombre.startsWith("COMENTARIO_")) {
            return comentario;
        }

        return normal;
    }

    private SimpleAttributeSet estilo(
            Color color,
            boolean negrita,
            boolean cursiva
    ) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setForeground(atributos, color);
        StyleConstants.setBold(atributos, negrita);
        StyleConstants.setItalic(atributos, cursiva);
        StyleConstants.setFontFamily(atributos, "Monospaced");
        StyleConstants.setFontSize(atributos, 15);
        return atributos;
    }
}

