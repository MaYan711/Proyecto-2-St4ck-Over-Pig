package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.zetariano.ZAst;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZetarianoThisSuperAstTest {

    @Test
    void construyeClaseConThisYLlamadasSuper() {
        String codigo = """
                public class Hija extends Padre {
                    private int valor;

                    public Hija(int valor) {
                        super(valor);
                        this.valor = valor;
                    }

                    @Override
                    public void mostrar() {
                        super.mostrar();
                        println(this.valor);
                    }
                }
                """;

        ZetarianoLexer lexer =
                new ZetarianoLexer(CharStreams.fromString(codigo));

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        assertTrue(tokens.getTokens().stream()
                .anyMatch(token ->
                        token.getType() == ZetarianoLexer.KW_THIS));

        assertTrue(tokens.getTokens().stream()
                .anyMatch(token ->
                        token.getType() == ZetarianoLexer.KW_SUPER));

        assertFalse(tokens.getTokens().stream()
                .anyMatch(token ->
                        token.getType() == ZetarianoLexer.IDENTIFICADOR
                                && (token.getText().equals("this")
                                || token.getText().equals("super"))));

        ZetarianoParser parser = new ZetarianoParser(tokens);
        var contexto = parser.archivo();

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var programa = new ZAstBuilder(Path.of("Hija.z"))
                .construir(contexto);

        ZAst.Clase clase = (ZAst.Clase) programa.elementos().get(0);

        assertEquals("Padre", clase.padre().orElseThrow());

        ZAst.Constructor constructor =
                (ZAst.Constructor) clase.miembros().get(1);

        assertEquals(2, constructor.cuerpo().sentencias().size());

        ZAst.Metodo metodo = (ZAst.Metodo) clase.miembros().get(2);

        assertTrue(metodo.sobrescrito());
        assertEquals(2, metodo.cuerpo().sentencias().size());
    }

    @Test
    void rechazaThisComoNombreDeVariable() {
        String codigo = """
                public class Ejemplo {
                    public void probar() {
                        int this = 1;
                    }
                }
                """;

        ZetarianoLexer lexer =
                new ZetarianoLexer(CharStreams.fromString(codigo));

        ZetarianoParser parser =
                new ZetarianoParser(new CommonTokenStream(lexer));

        parser.removeErrorListeners();
        parser.archivo();

        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }
}