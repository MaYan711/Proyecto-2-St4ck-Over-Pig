package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.y.YLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YLexerTest {

    @Test
    void reconoceSeccionesPalabrasReservadasYTipos() {
        String codigo = "%estructuras %funciones estructura definir retornar "
                + "si entonces sino contrario elegir caso siempre romper "
                + "continuar para mientras hacer imprimir leer entero "
                + "flotante cadena caracter bool verdadero falso";

        assertEquals(
                List.of(
                        "SECCION_ESTRUCTURAS",
                        "SECCION_FUNCIONES",
                        "KW_ESTRUCTURA",
                        "KW_DEFINIR",
                        "KW_RETORNAR",
                        "KW_SI",
                        "KW_ENTONCES",
                        "KW_SINO",
                        "KW_CONTRARIO",
                        "KW_ELEGIR",
                        "KW_CASO",
                        "KW_SIEMPRE",
                        "KW_ROMPER",
                        "KW_CONTINUAR",
                        "KW_PARA",
                        "KW_MIENTRAS",
                        "KW_HACER",
                        "KW_IMPRIMIR",
                        "KW_LEER",
                        "TYPE_ENTERO",
                        "TYPE_FLOTANTE",
                        "TYPE_CADENA",
                        "TYPE_CARACTER",
                        "TYPE_BOOL",
                        "BOOL_VERDADERO",
                        "BOOL_FALSO"
                ),
                nombresTokensPrincipales(codigo)
        );
    }

    @Test
    void conservaSensibilidadAMayusculas() {
        assertEquals(
                List.of(
                        "KW_DEFINIR",
                        "IDENTIFICADOR",
                        "IDENTIFICADOR"
                ),
                nombresTokensPrincipales("definir Definir VERDADERO")
        );
    }

    @Test
    void reconoceOperadoresYDelimitadores() {
        String codigo = "-> ++ -- == != <= >= && || = + - * / ! < > "
                + ": ; , . ( ) [ ] { }";

        assertEquals(
                List.of(
                        "FLECHA",
                        "INCREMENTO",
                        "DECREMENTO",
                        "IGUALDAD",
                        "DIFERENTE",
                        "MENOR_IGUAL",
                        "MAYOR_IGUAL",
                        "AND",
                        "OR",
                        "ASIGNACION",
                        "SUMA",
                        "RESTA",
                        "MULTIPLICACION",
                        "DIVISION",
                        "NEGACION",
                        "MENOR",
                        "MAYOR",
                        "DOS_PUNTOS",
                        "PUNTO_COMA",
                        "COMA",
                        "PUNTO",
                        "PARENTESIS_ABRE",
                        "PARENTESIS_CIERRA",
                        "CORCHETE_ABRE",
                        "CORCHETE_CIERRA",
                        "LLAVE_ABRE",
                        "LLAVE_CIERRA"
                ),
                nombresTokensPrincipales(codigo)
        );
    }

    @Test
    void reconoceLiterales() {
        assertEquals(
                List.of("ENTERO", "DECIMAL", "CADENA", "CARACTER"),
                nombresTokensPrincipales("12 3.14 \"hola\\n\" 'A'")
        );
    }

    @Test
    void enviaComentariosAlCanalOcultoYConservaSangria() {
        YLexer lexer = new YLexer(CharStreams.fromString(
                "// uno\n    imprimir(\"x\")"
        ));

        Token comentario = lexer.nextToken();
        Token nuevaLinea = lexer.nextToken();

        assertTrue(comentario.getChannel() != Token.DEFAULT_CHANNEL);
        assertEquals("\n    ", nuevaLinea.getText());
    }

    private List<String> nombresTokensPrincipales(String codigo) {
        YLexer lexer = new YLexer(CharStreams.fromString(codigo));
        List<String> nombres = new ArrayList<>();
        Token token;

        do {
            token = lexer.nextToken();
            if (token.getType() != Token.EOF
                    && token.getChannel() == Token.DEFAULT_CHANNEL) {
                nombres.add(lexer.getVocabulary().getSymbolicName(
                        token.getType()
                ));
            }
        } while (token.getType() != Token.EOF);

        return nombres;
    }
}
