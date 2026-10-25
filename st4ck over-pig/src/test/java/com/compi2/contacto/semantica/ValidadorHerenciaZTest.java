package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.ZAstBuilder;
import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.errores.Diagnostico;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorHerenciaZTest {

    @Test
    void rechazaPadreInexistente() {
        comprobarError(
                "no existe",
                "class Hija extends Ausente {}"
        );
    }

    @Test
    void rechazaHerenciaCircular() {
        comprobarError(
                "Herencia circular",
                "class A extends B {}",
                "class B extends A {}"
        );
    }

    @Test
    void rechazaHerenciaDeSiMisma() {
        comprobarError(
                "Herencia circular",
                "class A extends A {}"
        );
    }

    @Test
    void exigeOverride() {
        comprobarError(
                "requiere @Override",
                "class Padre { public void mostrar() {} }",
                """
                class Hija extends Padre {
                    public void mostrar() {}
                }
                """
        );
    }

    @Test
    void rechazaOverrideSinCoincidenciaDeParametros() {
        comprobarError(
                "@Override no corresponde",
                "class Padre { public void mostrar(int valor) {} }",
                """
                class Hija extends Padre {
                    @Override
                    public void mostrar(double valor) {}
                }
                """
        );
    }

    @Test
    void rechazaReduccionDeVisibilidad() {
        comprobarError(
                "reducir la visibilidad",
                "class Padre { public void mostrar() {} }",
                """
                class Hija extends Padre {
                    @Override
                    protected void mostrar() {}
                }
                """
        );
    }

    @Test
    void rechazaRetornoPrimitivoDiferente() {
        comprobarError(
                "retorno",
                "class Padre { public int valor() { return 1; } }",
                """
                class Hija extends Padre {
                    @Override
                    public double valor() { return 1.0; }
                }
                """
        );
    }

    @Test
    void aceptaRetornoCovarianteYMayorVisibilidad() {
        assertTrue(validar(
                "class Persona {}",
                "class Profesor extends Persona {}",
                """
                class Padre {
                    protected Persona crear() { return null; }
                }
                """,
                """
                class Hija extends Padre {
                    @Override
                    public Profesor crear() { return null; }
                }
                """
        ).isEmpty());
    }

    @Test
    void metodoPrivadoNoSeSobrescribe() {
        assertTrue(validar(
                "class Padre { private void mostrar() {} }",
                """
                class Hija extends Padre {
                    public void mostrar() {}
                }
                """
        ).isEmpty());
    }

    @Test
    void rechazaOverrideDeMetodoDefault() {
        comprobarError(
                "@Override no corresponde",
                "class Padre { void mostrar() {} }",
                """
                class Hija extends Padre {
                    @Override
                    public void mostrar() {}
                }
                """
        );
    }

    private void comprobarError(
            String fragmento,
            String... fuentes
    ) {
        List<Diagnostico> diagnosticos = validar(fuentes);

        assertTrue(
                diagnosticos.stream()
                        .anyMatch(diagnostico ->
                                diagnostico.esError()
                                        && diagnostico.mensaje()
                                        .contains(fragmento)),
                () -> "No se encontro el error esperado: "
                        + fragmento + ". Diagnosticos: " + diagnosticos
        );
    }

    private List<Diagnostico> validar(String... fuentes) {
        List<ProgramaAst> programas = new ArrayList<>();

        for (int i = 0; i < fuentes.length; i++) {
            ZetarianoLexer lexer = new ZetarianoLexer(
                    CharStreams.fromString(fuentes[i])
            );

            ZetarianoParser parser = new ZetarianoParser(
                    new CommonTokenStream(lexer)
            );

            var contexto = parser.archivo();

            assertEquals(0, parser.getNumberOfSyntaxErrors());

            programas.add(
                    new ZAstBuilder(Path.of("Prueba" + i + ".z"))
                            .construir(contexto)
            );
        }

        return new ValidadorHerenciaZ().validar(programas);
    }
}