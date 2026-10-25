package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.zetariano.ZAst;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZetarianoHerenciaAstTest {

    @Test
    void conservaElNombreDelPadre() {
        ZAst.Clase clase = construir(
                "CuentaAhorro",
                """
                public class CuentaAhorro extends Cuenta {
                    private double tasa;

                    @Override
                    public void finDeMes() {
                    }
                }
                """
        );

        assertEquals("CuentaAhorro", clase.nombre());
        assertEquals("Cuenta", clase.padre().orElseThrow());

        ZAst.Metodo metodo = (ZAst.Metodo) clase.miembros().get(1);

        assertTrue(metodo.sobrescrito());
    }

    @Test
    void aceptaClaseSinPadreYSinPublic() {
        ZAst.Clase clase = construir(
                "Cuenta",
                """
                class Cuenta {
                    public Cuenta() {
                    }
                }
                """
        );

        assertEquals("Cuenta", clase.nombre());
        assertTrue(clase.padre().isEmpty());
    }

    private ZAst.Clase construir(String nombre, String codigo) {
        ZetarianoLexer lexer =
                new ZetarianoLexer(CharStreams.fromString(codigo));

        ZetarianoParser parser =
                new ZetarianoParser(new CommonTokenStream(lexer));

        var contexto = parser.archivo();

        assertEquals(0, parser.getNumberOfSyntaxErrors());

        var programa = new ZAstBuilder(Path.of(nombre + ".z"))
                .construir(contexto);

        return (ZAst.Clase) programa.elementos().get(0);
    }
}