package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZetarianoModificadoresParserTest {

    @Test
    void reconoceVisibilidadYAnotacionOverride() {
        String codigo = """
                public class Cuenta {
                    private double saldo;
                    protected String titular;
                    int movimientos;

                    public Cuenta() {
                        saldo = 0.0;
                    }

                    protected void ajustarSaldo(double monto) {
                        saldo += monto;
                    }

                    private double consultarSaldo() {
                        return saldo;
                    }

                    void registrar() {
                        movimientos++;
                    }

                    @Override
                    public void describir() {
                        println(titular);
                    }
                }
                """;

        ZetarianoLexer lexer =
                new ZetarianoLexer(CharStreams.fromString(codigo));

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        long erroresLexicos = tokens.getTokens().stream()
                .filter(token -> token.getType() == ZetarianoLexer.IDENTIFICADOR)
                .filter(token -> token.getText().equals("private")
                        || token.getText().equals("protected"))
                .count();

        assertEquals(0, erroresLexicos);

        ZetarianoParser parser = new ZetarianoParser(tokens);
        parser.archivo();

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        long anotaciones = tokens.getTokens().stream()
                .filter(token -> token.getType() == ZetarianoLexer.KW_OVERRIDE)
                .count();

        assertEquals(1, anotaciones);
    }
}