package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.zetariano.Visibilidad;
import com.compi2.contacto.ast.zetariano.ZAst;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZetarianoModificadoresAstTest {

    @Test
    void conservaVisibilidadYOverrideEnElAst() {
        String codigo = """
                public class Cuenta {
                    private double saldo;
                    protected String titular;
                    int movimientos;

                    public Cuenta() {
                    }

                    @Override
                    public void describir() {
                    }

                    void registrar() {
                    }
                }
                """;

        ZetarianoLexer lexer =
                new ZetarianoLexer(CharStreams.fromString(codigo));

        ZetarianoParser parser =
                new ZetarianoParser(new CommonTokenStream(lexer));

        var contexto = parser.archivo();

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var programa = new ZAstBuilder(Path.of("Cuenta.z"))
                .construir(contexto);

        ZAst.Clase clase = (ZAst.Clase) programa.elementos().get(0);

        ZAst.Atributo saldo = (ZAst.Atributo) clase.miembros().get(0);
        ZAst.Atributo titular = (ZAst.Atributo) clase.miembros().get(1);
        ZAst.Atributo movimientos = (ZAst.Atributo) clase.miembros().get(2);
        ZAst.Constructor constructor =
                (ZAst.Constructor) clase.miembros().get(3);
        ZAst.Metodo describir = (ZAst.Metodo) clase.miembros().get(4);
        ZAst.Metodo registrar = (ZAst.Metodo) clase.miembros().get(5);

        assertEquals(Visibilidad.PRIVADA, saldo.visibilidad());
        assertEquals(Visibilidad.PROTEGIDA, titular.visibilidad());
        assertEquals(Visibilidad.DEFAULT, movimientos.visibilidad());
        assertEquals(Visibilidad.PUBLICA, constructor.visibilidad());
        assertEquals(Visibilidad.PUBLICA, describir.visibilidad());
        assertTrue(describir.sobrescrito());
        assertEquals(Visibilidad.DEFAULT, registrar.visibilidad());
        assertFalse(registrar.sobrescrito());
    }
}