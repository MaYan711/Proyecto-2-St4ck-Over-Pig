package com.compi2.contacto.compilacion;

import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.c3d.GeneradorCodigoC;
import com.compi2.contacto.ir.GeneradorCuartetasZ;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import com.compi2.contacto.semantica.EnlacesZ;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructoresHerenciaGeneracionTest {

    @Test
    void conservaDestinoCalificadoDeSuper() {
        PosicionFuente posicion = new PosicionFuente(Path.of("Profesor.z"), 1, 0);
        ZAst.Llamada llamada = new ZAst.Llamada(
                new ZAst.Identificador("super", posicion), List.of(), posicion
        );
        ZAst.Constructor constructor = new ZAst.Constructor(
                "Profesor", List.of(),
                new ZAst.Bloque(List.of(new ZAst.ExpresionSentencia(llamada, posicion)), posicion),
                posicion
        );
        ProgramaAst fuente = new ProgramaAst(
                LenguajeFuente.ZETARIANO,
                List.of(new ZAst.Clase("Profesor", List.of(constructor), posicion, Optional.of("Persona"))),
                posicion
        );
        EnlacesZ enlaces = new EnlacesZ();
        enlaces.registrarLlamada(llamada, "Persona.<init>()");

        var llamadas = new GeneradorCuartetasZ()
                .generar(List.of(fuente), enlaces)
                .cuartetas().stream()
                .filter(c -> c.operador() == OperadorCuarteta.LLAMAR)
                .toList();

        assertEquals(1, llamadas.size());
        assertEquals("Persona.<init>()", llamadas.get(0).argumento1());
    }

    @Test
    void generaCodigoParaObjetoConTipoExplicito() {
        ResultadoCompilacion resultado = compilarEjemplo("01_objeto_tipo_explicito");
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
        assertTrue(resultado.programaMemoria().cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LLAMAR
                        && "Persona.<init>()".equals(c.argumento1())
                        && "1".equals(c.argumento2())));
        assertTrue(resultado.programaMemoria().cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LEER_STACK
                        && "P+1".equals(c.argumento1())));
        assertFalse(new GeneradorCodigoC().generar(resultado.programaMemoria()).isBlank());
    }

    @Test
    void generaCodigoParaNullYComparaciones() {
        ResultadoCompilacion resultado = compilarEjemplo("02_null_y_comparaciones");
        assertTrue(resultado.esValido(), () -> resultado.diagnosticos().toString());
        assertFalse(new GeneradorCodigoC().generar(resultado.programaMemoria()).isBlank());
    }

    private ResultadoCompilacion compilarEjemplo(String caso) {
        Path raiz = Path.of("casos-integrales", caso);
        String codigoPrincipal = switch (caso) {
            case "01_objeto_tipo_explicito" -> """
                    import Persona.z
                    import Profesor.z
                    MAIOR>
                    esto base : novus Profesor() : Persona;
                    esto otro : null : Persona;
                    base = otro;
                    FINIS;
                    """;
            case "02_null_y_comparaciones" -> """
                    import Persona.z
                    import Profesor.z
                    MAIOR>
                    esto base : null : Persona;
                    si (base == null) {
                        base = novus Profesor();
                    } finis;
                    FINIS;
                    """;
            default -> throw new IllegalArgumentException("Caso desconocido: " + caso);
        };

        return new CompiladorProyecto().compilar(new ProyectoCompilacion(
                raiz,
                List.of(
                        fuente(raiz, "Persona.z", LenguajeFuente.ZETARIANO, """
                                public class Persona {
                                    public Persona() {}
                                }
                                """),
                        fuente(raiz, "Profesor.z", LenguajeFuente.ZETARIANO, """
                                public class Profesor extends Persona {
                                    public Profesor() { super(); }
                                }
                                """),
                        fuente(raiz, "main.pig", LenguajeFuente.PIG_LATIN, codigoPrincipal)
                )
        ));
    }

    private ArchivoFuente fuente(
            Path raiz,
            String nombre,
            LenguajeFuente lenguaje,
            String contenido
    ) {
        return new ArchivoFuente(raiz.resolve(nombre), lenguaje, contenido);
    }
}
