package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PigLatinObjetosTipadosTest {

    private static final String PERSONA = """
            public class Persona {
                public Persona() {}
            }
            """;

    private static final String PROFESOR = """
            public class Profesor extends Persona {
                public Profesor() { super(); }
            }
            """;

    private static final String ALUMNO = """
            public class Alumno extends Persona {
                public Alumno() { super(); }
            }
            """;

    @Test
    void aceptaObjetosConTipoExplicitoYNull() {
        var validacion = compilar("""
                import Persona.z
                import Profesor.z
                import Alumno.z
                MAIOR>
                esto persona : novus Profesor() : Persona;
                esto ausente : null : Persona;
                persona = null;
                FINIS;
                """);
        assertTrue(validacion.esValido(), () -> validacion.diagnosticos().toString());
    }

    @Test
    void conservaElTipoExplicitoEnAst() {
        var resultado = analizarPig("""
                MAIOR>
                esto persona : novus Profesor() : Persona;
                esto ausente : null : Persona;
                FINIS;
                """);
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
        PAst.Programa programa = (PAst.Programa) resultado.ast().orElseThrow().elementos().get(0);
        PAst.Declaracion declaracion = (PAst.Declaracion) programa.principal().get(0);
        assertEquals("Persona", declaracion.tipo().nombreBase());
        PAst.NuevoObjeto instancia = (PAst.NuevoObjeto) declaracion.inicializador().orElseThrow();
        assertEquals("Profesor", instancia.tipo());
        PAst.Declaracion nula = (PAst.Declaracion) programa.principal().get(1);
        assertEquals(PAst.TipoLiteral.NULO,
                ((PAst.Literal) nula.inicializador().orElseThrow()).tipo());
    }

    @Test
    void aceptaComparacionesDeNullConObjetos() {
        var resultado = compilar("""
                import Persona.z
                import Profesor.z
                import Alumno.z
                MAIOR>
                esto persona : null : Persona;
                si (persona == null) {
                    persona = novus Profesor();
                } finis;
                FINIS;
                """);
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
    }

    @Test
    void rechazaPadreAsignadoAHija() {
        var resultado = compilar("""
                import Persona.z
                import Profesor.z
                import Alumno.z
                MAIOR>
                esto profesor : novus Persona() : Profesor;
                FINIS;
                """);
        assertFalse(resultado.esValido());
        assertTrue(resultado.diagnosticos().stream().anyMatch(d -> d.mensaje().contains("No se puede asignar")));
    }

    @Test
    void rechazaHermanasComoTiposCompatibles() {
        var resultado = compilar("""
                import Persona.z
                import Profesor.z
                import Alumno.z
                MAIOR>
                esto alumno : novus Profesor() : Alumno;
                FINIS;
                """);
        assertFalse(resultado.esValido());
    }

    @Test
    void rechazaNullEnPrimitivos() {
        var resultado = compilar("""
                import Persona.z
                import Profesor.z
                import Alumno.z
                MAIOR>
                esto cantidad : numerus 1;
                cantidad = null;
                FINIS;
                """);
        assertFalse(resultado.esValido());
    }

    @Test
    void rechazaReferenciaNullDeClaseNoImportada() {
        var resultado = compilar("""
                MAIOR>
                esto desconocido : null : Persona;
                FINIS;
                """);
        assertFalse(resultado.esValido());
    }

    @Test
    void mantieneCompatibleLaSintaxisAnterior() {
        var resultado = compilar("""
                import Persona.z
                import Profesor.z
                import Alumno.z
                MAIOR>
                esto persona : novus Persona();
                esto profesor : novus Profesor();
                FINIS;
                """);
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
    }

    private ResultadoAnalisisArchivo analizarPig(String pig) {
        return new AnalizadorArchivo().analizar(
                new ArchivoFuente(Path.of("Main.pig"), LenguajeFuente.PIG_LATIN, pig));
    }

    private ResultadoSemanticoPig compilar(String pig) {
        Path raiz = Path.of("prueba-objetos-tipados");
        ProyectoCompilacion proyecto = new ProyectoCompilacion(raiz, List.of(
                new ArchivoFuente(raiz.resolve("Persona.z"), LenguajeFuente.ZETARIANO, PERSONA),
                new ArchivoFuente(raiz.resolve("Profesor.z"), LenguajeFuente.ZETARIANO, PROFESOR),
                new ArchivoFuente(raiz.resolve("Alumno.z"), LenguajeFuente.ZETARIANO, ALUMNO),
                new ArchivoFuente(raiz.resolve("Main.pig"), LenguajeFuente.PIG_LATIN, pig)
        ));
        var resultados = proyecto.archivos().stream().map(new AnalizadorArchivo()::analizar).toList();
        assertTrue(resultados.stream().allMatch(ResultadoAnalisisArchivo::esValido),
                () -> resultados.stream().flatMap(r -> r.diagnosticos().stream()).toList().toString());
        return new AnalizadorSemanticoPig().analizar(proyecto, resultados);
    }
}
