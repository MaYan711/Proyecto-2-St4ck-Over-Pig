package com.compi2.contacto.memoria;

import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanificadorHerenciaHeapZTest {

    private static final PosicionFuente POSICION =
            new PosicionFuente(Path.of("Herencia.z"), 1, 0);

    @Test
    void ordenaCamposDelPadreAntesDeLosDeLaHijaYSuNieta() {
        PlanMemoriaProyecto plan = planificar(
                clase("Nieta", "Hija", "campoNieta"),
                clase("Hija", "Padre", "campoHija"),
                clase("Padre", null, "campoPadre")
        );

        LayoutHeapProyecto padre = plan.layout("Z::Padre").orElseThrow();
        LayoutHeapProyecto hija = plan.layout("Z::Hija").orElseThrow();
        LayoutHeapProyecto nieta = plan.layout("Z::Nieta").orElseThrow();

        assertEquals(1, padre.tamano());
        assertEquals(2, hija.tamano());
        assertEquals(3, nieta.tamano());
        assertEquals(0, nieta.buscarCampo("campoPadre").orElseThrow().desplazamiento());
        assertEquals(1, nieta.buscarCampo("campoHija").orElseThrow().desplazamiento());
        assertEquals(2, nieta.buscarCampo("campoNieta").orElseThrow().desplazamiento());
    }

    @Test
    void conservaCampoDelPadreAunqueSeaPrivado() {
        PlanMemoriaProyecto plan = planificar(
                clase("Hija", "Padre", "otro"),
                clase("Padre", null, "secreto")
        );
        LayoutHeapProyecto hija = plan.layout("Z::Hija").orElseThrow();
        assertEquals(2, hija.tamano());
        assertEquals(0, hija.buscarCampo("secreto").orElseThrow().desplazamiento());
        assertEquals(1, hija.buscarCampo("otro").orElseThrow().desplazamiento());
    }

    @Test
    void atributoOcultoPorLaHijaTieneOtraCelda() {
        PlanMemoriaProyecto plan = planificar(
                clase("Padre", null, "dato"),
                clase("Hija", "Padre", "dato")
        );
        LayoutHeapProyecto hija = plan.layout("Z::Hija").orElseThrow();
        assertEquals(2, hija.tamano());
        assertEquals(0, hija.campos().get(0).desplazamiento());
        assertEquals(1, hija.buscarCampo("dato").orElseThrow().desplazamiento());
        assertEquals(0, plan.layout("Z::Padre").orElseThrow()
                .buscarCampo("dato").orElseThrow().desplazamiento());
    }

    @Test
    void lasClasesHermanasNoModificanElLayoutDelPadre() {
        PlanMemoriaProyecto plan = planificar(
                clase("HijaA", "Padre", "a"),
                clase("HijaB", "Padre", "b"),
                clase("Padre", null, "base")
        );
        assertEquals(1, plan.layout("Z::Padre").orElseThrow().tamano());
        assertEquals(2, plan.layout("Z::HijaA").orElseThrow().tamano());
        assertEquals(2, plan.layout("Z::HijaB").orElseThrow().tamano());
        assertTrue(plan.layout("Z::HijaA").orElseThrow().buscarCampo("b").isEmpty());
    }

    @Test
    void reservaEspacioParaTodosLosAtributosHeredadosEnHeap() {
        PlanMemoriaProyecto layouts = planificar(
                clase("Nieta", "Hija", "c"),
                clase("Hija", "Padre", "b"),
                clase("Padre", null, "a")
        );
        PlanMemoriaProyecto plan = new PlanMemoriaProyecto(
                Map.of("MAIOR", new MarcoStackProyecto("MAIOR", List.of())),
                layouts.layoutsHeap()
        );
        ProgramaIntermedio cuartetas = new ProgramaIntermedio();
        cuartetas.agregar(new Cuarteta(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        cuartetas.agregar(new Cuarteta(OperadorCuarteta.NUEVO_OBJETO, "Nieta", "0", "referencia"));
        cuartetas.agregar(new Cuarteta(OperadorCuarteta.FIN_FUNCION, "MAIOR", null, null));

        var resultado = new BajadorLlamadasProyecto().bajar(cuartetas, plan).cuartetas();
        assertTrue(resultado.stream().anyMatch(c ->
                c.operador() == OperadorCuarteta.SUMAR && "H".equals(c.argumento1())
                        && "3".equals(c.argumento2())));
        assertEquals(3, resultado.stream()
                .filter(c -> c.operador() == OperadorCuarteta.ESCRIBIR_HEAP).count());
    }

    private PlanMemoriaProyecto planificar(ZAst.Clase... clases) {
        ProgramaAst programa = new ProgramaAst(
                LenguajeFuente.ZETARIANO,
                List.of(clases),
                POSICION
        );
        return new PlanificadorMemoriaProyecto().planificar(
                List.of(), List.of(programa), List.of()
        );
    }

    private ZAst.Clase clase(String nombre, String padre, String atributo) {
        ZAst.Miembro campo = new ZAst.Atributo(
                new ZAst.Tipo("int", 0, POSICION),
                atributo,
                Optional.empty(),
                POSICION
        );
        return new ZAst.Clase(
                nombre,
                List.of(campo),
                POSICION,
                Optional.ofNullable(padre)
        );
    }
}
