package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.y.YLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizadorIndentacionYTest {

    @Test
    void generaIndentYDedentParaUnBloque() {
        String codigo = "%funciones\n"
                + "definir saludo():\n"
                + "    imprimir(\"hola\")\n";

        assertEquals(
                List.of(YLexer.INDENT, YLexer.DEDENT),
                tokensDeIndentacion(codigo)
        );
    }

    @Test
    void generaBloquesAnidados() {
        String codigo = "%funciones\n"
                + "definir prueba():\n"
                + "    si(verdadero) entonces\n"
                + "        imprimir(\"x\")\n"
                + "    retornar 1\n";

        assertEquals(
                List.of(
                        YLexer.INDENT,
                        YLexer.INDENT,
                        YLexer.DEDENT,
                        YLexer.DEDENT
                ),
                tokensDeIndentacion(codigo)
        );
    }

    @Test
    void ignoraLineasVaciasYComentarios() {
        String codigo = "%funciones\n"
                + "definir prueba():\n"
                + "    // comentario\n"
                + "\n"
                + "    imprimir(\"x\")\n";

        assertEquals(
                List.of(YLexer.INDENT, YLexer.DEDENT),
                tokensDeIndentacion(codigo)
        );
    }

    @Test
    void ignoraSaltosDentroDeDelimitadores() {
        String codigo = "%funciones\n"
                + "definir sumar(\n"
                + "    entero a,\n"
                + "    entero b\n"
                + "):\n"
                + "    retornar a + b\n";

        assertEquals(
                List.of(YLexer.INDENT, YLexer.DEDENT),
                tokensDeIndentacion(codigo)
        );
    }

    @Test
    void aceptaTabulacionComoSangria() {
        String codigo = "%funciones\n"
                + "definir prueba():\n"
                + "\timprimir(\"x\")\n";

        assertEquals(
                List.of(YLexer.INDENT, YLexer.DEDENT),
                tokensDeIndentacion(codigo)
        );
    }

    @Test
    void detectaDedentQueNoCoincide() {
        String codigo = "%funciones\n"
                + "definir prueba():\n"
                + "    si(verdadero) entonces\n"
                + "        imprimir(\"x\")\n"
                + "      retornar 1\n";

        assertTrue(tokensDeIndentacion(codigo).contains(
                YLexer.ERROR_INDENTACION
        ));
    }

    @Test
    void cierraBloqueAlFinalSinSaltoDeLinea() {
        String codigo = "%funciones\n"
                + "definir prueba():\n"
                + "    retornar 1";

        List<Integer> tokens = tokensDeIndentacion(codigo);

        assertFalse(tokens.isEmpty());
        assertEquals(YLexer.INDENT, tokens.get(0));
        assertEquals(YLexer.DEDENT, tokens.get(tokens.size() - 1));
    }

    private List<Integer> tokensDeIndentacion(String codigo) {
        TokenizadorIndentacionY tokenizador = new TokenizadorIndentacionY(
                new YLexer(CharStreams.fromString(codigo))
        );
        List<Integer> tipos = new ArrayList<>();
        Token token;

        do {
            token = tokenizador.nextToken();
            if (token.getType() == YLexer.INDENT
                    || token.getType() == YLexer.DEDENT
                    || token.getType() == YLexer.ERROR_INDENTACION) {
                tipos.add(token.getType());
            }
        } while (token.getType() != Token.EOF);

        return tipos;
    }
}
