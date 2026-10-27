package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.ZAstBuilder;
import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.ProgramaAst;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HerenciaIntegracionSemanticaZTest {

    @Test
    void thisDistingueParametroDeAtributo() {
        comprobar(true,
                "class A { private int valor; public A(int valor) { this.valor = valor; } public int obtener() { return this.valor; } }");
    }

    @Test
    void atributoProtegidoHeredado() {
        comprobar(true,
                "class A { protected int valor; }",
                "class B extends A { public int obtener() { return valor; } }");
    }

    @Test
    void metodoHeredado() {
        comprobar(true,
                "class A { public int obtener() { return 5; } }",
                "class B extends A { public int probar() { return obtener(); } }");
    }

    @Test
    void superMetodo() {
        comprobar(true,
                "class A { protected int obtener() { return 5; } }",
                "class B extends A { public int probar() { return super.obtener(); } }");
    }

    @Test
    void superExplicito() {
        comprobar(true,
                "class A { protected A(int valor) {} }",
                "class B extends A { public B() { super(5); } }");
    }

    @Test
    void superImplicito() {
        comprobar(true,
                "class A { public A() {} }",
                "class B extends A { public B() {} }");
    }

    @Test
    void padreSinConstructores() {
        comprobar(true,
                "class A {}",
                "class B extends A {}");
    }

    @Test
    void asignacionSubtipo() {
        comprobar(true,
                "class A {}",
                "class B extends A {}",
                "class C { public void probar() { A a = new B(); } }");
    }

    @Test
    void retornoCovarianteConThis() {
        comprobar(true,
                "class A { public A obtener() { return this; } }",
                "class B extends A { @Override public B obtener() { return this; } }");
    }

    @Test
    void atributoPrivadoNoAccesible() {
        comprobar(false,
                "class A { private int valor; }",
                "class B extends A { public int obtener() { return this.valor; } }");
    }

    @Test
    void atributoDefaultAccesibleEnMismoPaquete() {
        comprobar(true,
                "class A { int valor; }",
                "class B extends A { public int obtener() { return valor; } }");
    }

    @Test
    void metodoProtegidoAccesibleEnMismoPaquete() {
        comprobar(true,
                "class A { protected void probar() {} }",
                "class B { public void ejecutar(A a) { a.probar(); } }");
    }

    @Test
    void privadoDesdeOtraInstanciaDeMismaClase() {
        comprobar(true,
                "class A { private int valor; public int obtener(A otro) { return otro.valor; } }");
    }

    @Test
    void constructorPrivadoDesdeAjena() {
        comprobar(false,
                "class A { private A() {} }",
                "class B { public void probar() { A a = new A(); } }");
    }

    @Test
    void superFueraDeConstructor() {
        comprobar(false,
                "class A {}",
                "class B extends A { public void probar() { super(); } }");
    }

    @Test
    void superNoPrimero() {
        comprobar(false,
                "class A {}",
                "class B extends A { public B() { int x = 0; super(); } }");
    }

    @Test
    void superSinPadre() {
        comprobar(false,
                "class A { public A() { super(); } }");
    }

    @Test
    void faltaSuperConParametros() {
        comprobar(false,
                "class A { public A(int x) {} }",
                "class B extends A { public B() {} }");
    }

    @Test
    void constructorImplicitoInvalido() {
        comprobar(false,
                "class A { public A(int x) {} }",
                "class B extends A {}");
    }

    @Test
    void superPrivado() {
        comprobar(false,
                "class A { private A() {} }",
                "class B extends A { public B() { super(); } }");
    }

    @Test
    void noAsignaThis() {
        comprobar(false,
                "class A { public void probar() { this = new A(); } }");
    }

    @Test
    void noAsignaPadreAHija() {
        comprobar(false,
                "class A {}",
                "class B extends A {}",
                "class C { public void probar() { B b = new A(); } }");
    }

    @Test
    void sobrecargaHeredada() {
        comprobar(true,
                "class A { public int valor(int x) { return x; } }",
                "class B extends A { public int valor(double x) { return 2; } public int probar() { return valor(1); } }");
    }

    @Test
    void atributosOcultos() {
        comprobar(true,
                "class A { protected int valor; }",
                "class B extends A { private double valor; public int obtener() { return super.valor; } }");
    }

    @Test
    void herenciaProfunda() {
        comprobar(true,
                "class A { protected int valor; public A() {} }",
                "class B extends A {}",
                "class C extends B { public int probar() { return this.valor; } }");
    }

    private void comprobar(boolean valido, String... fuentes) {
        List<ProgramaAst> programas = new ArrayList<>();
        for (int i = 0; i < fuentes.length; i++) {
            var lexer = new ZetarianoLexer(CharStreams.fromString(fuentes[i]));
            var parser = new ZetarianoParser(new CommonTokenStream(lexer));
            var contexto = parser.archivo();
            assertEquals(0, parser.getNumberOfSyntaxErrors());
            programas.add(new ZAstBuilder(Path.of("Caso" + i + ".z")).construir(contexto));
        }
        var resultado = new AnalizadorSemanticoZ().analizar(programas);
        assertEquals(valido, resultado.esValido(), () -> resultado.diagnosticos().toString());
    }
}
