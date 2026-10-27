package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalizadorSemanticoPigVisibilidadTest {

    private static final String CAJA = """
            public class Caja {
                private int secreto;
                protected int protegido;
                int paquete;
                public int visible;
                public Caja() {}
                private int privado() { return 1; }
                protected int protegidoMetodo() { return 2; }
                int paqueteMetodo() { return 3; }
                public int publico() { return visible; }
            }
            """;

    @Test
    void rechazaLeerAtributoPrivado() {
        comprobarError(pig(" >> caja.secreto; "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void rechazaEscribirAtributoPrivado() {
        comprobarError(pig(" caja.secreto = 8; "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void rechazaLeerAtributoProtegidoDesdePig() {
        comprobarError(pig(" >> caja.protegido; "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void rechazaLeerAtributoDefaultDesdePig() {
        comprobarError(pig(" >> caja.paquete; "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void rechazaLlamarMetodoPrivado() {
        comprobarError(pig(" >> caja.privado(); "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void rechazaLlamarMetodoProtegido() {
        comprobarError(pig(" >> caja.protegidoMetodo(); "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void rechazaLlamarMetodoDefault() {
        comprobarError(pig(" >> caja.paqueteMetodo(); "), "no es accesible", fuente("Caja", CAJA));
    }

    @Test
    void aceptaLeerEscribirYLlamarPublicos() {
        var resultado = preparar(pig(" caja.visible = 4; >> caja.visible; >> caja.publico(); "),
                fuente("Caja", CAJA)).resultado();
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
    }

    @Test
    void aceptaAtributoPublicoHeredado() {
        var resultado = preparar("""
                import Base.z
                import Hija.z
                MAIOR>
                esto hija : novus Hija() : Hija;
                >> hija.valor;
                FINIS;
                """, fuente("Base", "public class Base { public int valor; }"),
                fuente("Hija", "public class Hija extends Base { public Hija() {} }")).resultado();
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
    }

    @Test
    void rechazaAtributoPrivadoHijoQueOcultaAtributoPublicoPadre() {
        comprobarError("""
                import Base.z
                import Hija.z
                MAIOR>
                esto hija : novus Hija() : Hija;
                >> hija.valor;
                FINIS;
                """, "no es accesible",
                fuente("Base", "public class Base { public int valor; }"),
                fuente("Hija", "public class Hija extends Base { private int valor; public Hija() {} }"));
    }

    @Test
    void aceptaSobrecargaPublicaHeredadaDePadre() {
        var prueba = preparar("""
                import Base.z
                import Hija.z
                MAIOR>
                esto hija : novus Hija() : Hija;
                >> hija.calcular(3);
                FINIS;
                """, fuente("Base", """
                public class Base { public int calcular(int numero) { return numero; } }
                """), fuente("Hija", """
                public class Hija extends Base {
                    public Hija() {}
                    public int calcular(String texto) { return 2; }
                }
                """));
        assertTrue(prueba.resultado().esValido(), () -> prueba.resultado().diagnosticos().toString());
        var pig = prueba.analisis().stream()
                .filter(r -> r.archivo().lenguaje() == LenguajeFuente.PIG_LATIN)
                .findFirst().orElseThrow();
        PAst.Programa programa = (PAst.Programa) pig.ast().orElseThrow().elementos().get(0);
        PAst.Salida salida = (PAst.Salida) programa.principal().get(1);
        PAst.Llamada llamada = (PAst.Llamada) salida.expresiones().get(0);
        assertEquals("Base.calcular(int)",
                prueba.resultado().enlaces().buscarLlamada(llamada).orElseThrow().destino());
    }

    @Test
    void rechazaConstructorPrivado() {
        comprobarError(pig(""), "no es accesible", fuente("Caja", """
                public class Caja { private Caja() {} }
                """));
    }

    @Test
    void aceptaConstructorPublico() {
        var resultado = preparar(pig(""), fuente("Caja", """
                public class Caja { public Caja() {} }
                """)).resultado();
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
    }

    private String pig(String cuerpo) {
        return "import Caja.z\nMAIOR>\nesto caja : novus Caja() : Caja;\n"
                + cuerpo + "\nFINIS;\n";
    }

    private FuenteZ fuente(String nombre, String codigo) {
        return new FuenteZ(nombre, codigo);
    }

    private void comprobarError(String pig, String mensaje, FuenteZ... fuentes) {
        var resultado = preparar(pig, fuentes).resultado();
        assertFalse(resultado.esValido());
        assertTrue(resultado.diagnosticos().stream().anyMatch(
                        d -> d.esError() && d.mensaje().contains(mensaje)),
                () -> resultado.diagnosticos().toString());
    }

    private Prueba preparar(String pig, FuenteZ... fuentes) {
        Path raiz = Path.of("pruebas-visibilidad-pig");
        List<ArchivoFuente> archivos = new ArrayList<>();
        for (FuenteZ fuente : fuentes) {
            archivos.add(new ArchivoFuente(raiz.resolve(fuente.nombre() + ".z"),
                    LenguajeFuente.ZETARIANO, fuente.codigo()));
        }
        archivos.add(new ArchivoFuente(raiz.resolve("Main.pig"),
                LenguajeFuente.PIG_LATIN, pig));
        ProyectoCompilacion proyecto = new ProyectoCompilacion(raiz, archivos);
        AnalizadorArchivo analizador = new AnalizadorArchivo();
        List<ResultadoAnalisisArchivo> resultados = proyecto.archivos().stream()
                .map(analizador::analizar).toList();
        assertTrue(resultados.stream().allMatch(ResultadoAnalisisArchivo::esValido),
                () -> resultados.stream().flatMap(r -> r.diagnosticos().stream()).toList().toString());
        return new Prueba(new AnalizadorSemanticoPig().analizar(proyecto, resultados), resultados);
    }

    private record FuenteZ(String nombre, String codigo) {}
    private record Prueba(ResultadoSemanticoPig resultado, List<ResultadoAnalisisArchivo> analisis) {}
}
