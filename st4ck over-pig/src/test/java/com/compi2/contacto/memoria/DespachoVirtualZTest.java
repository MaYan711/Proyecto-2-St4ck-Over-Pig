package com.compi2.contacto.memoria;

import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.c3d.GeneradorCodigoC;
import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DespachoVirtualZTest {
    private static final PosicionFuente POSICION =
            new PosicionFuente(Path.of("Despacho.z"), 1, 0);

    @Test
    void conservaJerarquiaDeClasesEnPlan() {
        var plan = planConMetodos(Map.of("Hija", "Padre"),
                "Padre", "Hija");
        assertEquals("Padre", plan.padresZ().get("Hija"));
        assertEquals(2, plan.layoutsHeap().size());
    }

    @Test
    void registraTipoEnObjetoSinAlterarElTamanoDelHeap() {
        var plan = planConMetodos(Map.of("Hija", "Padre"),
                "Padre", "Hija");
        ProgramaIntermedio origen = programaPrincipalConObjeto("Hija", false);
        var cuartetas = new BajadorLlamadasProyecto().bajar(origen, plan).cuartetas();

        assertEquals(1, cuartetas.stream()
                .filter(c -> c.operador() == OperadorCuarteta.ESCRIBIR_TIPO_OBJETO)
                .count());
        assertTrue(cuartetas.stream().anyMatch(c ->
                c.operador() == OperadorCuarteta.SUMAR
                        && "H".equals(c.argumento1())
                        && "1".equals(c.argumento2())));
    }

    @Test
    void referenciaPadreEjecutaMetodoSobrescritoDeHija() {
        var plan = planConMetodos(Map.of("Hija", "Padre"),
                "Padre", "Hija");
        var resultado = new BajadorLlamadasProyecto().bajar(
                programaPrincipalConObjeto("Hija", true), plan);

        assertTrue(tieneLlamada(resultado, "Padre.mostrar()"));
        assertTrue(tieneLlamada(resultado, "Hija.mostrar()"));
        assertTrue(resultado.cuartetas().stream().anyMatch(c ->
                c.operador() == OperadorCuarteta.LEER_TIPO_OBJETO));
        assertTrue(resultado.cuartetas().stream().anyMatch(c ->
                c.operador() == OperadorCuarteta.SALTAR_SI_VERDADERO));
    }

    @Test
    void seleccionaSobrescriturasDeVariosNiveles() {
        var plan = planConMetodos(Map.of(
                "Hija", "Padre", "Nieta", "Hija"),
                "Padre", "Hija", "Nieta");
        var resultado = new BajadorLlamadasProyecto().bajar(
                programaPrincipalConObjeto("Nieta", true), plan);
        assertTrue(tieneLlamada(resultado, "Padre.mostrar()"));
        assertTrue(tieneLlamada(resultado, "Hija.mostrar()"));
        assertTrue(tieneLlamada(resultado, "Nieta.mostrar()"));
        assertEquals(2, resultado.cuartetas().stream().filter(c ->
                c.operador() == OperadorCuarteta.SALTAR_SI_VERDADERO).count());
    }

    @Test
    void codigoCIncluyeTablaDeTiposYDespacho() {
        var plan = planConMetodos(Map.of("Hija", "Padre"),
                "Padre", "Hija");
        var resultado = new BajadorLlamadasProyecto().bajar(
                programaPrincipalConObjeto("Hija", true), plan);
        String codigo = new GeneradorCodigoC().generar(resultado);
        assertTrue(codigo.contains("int TipoObjetoZ[HEAP_SIZE];"));
        assertTrue(codigo.contains("TipoObjetoZ[(int) ("));
        assertTrue(codigo.contains("call_virtual_L"));
        assertFalse(codigo.contains("No existe la funcion C"));
    }

    @Test
    void llamadaSuperUsaImplementacionDelPadre() {
        var original = planConMetodos(Map.of("Hija", "Padre"),
                "Padre", "Hija");
        var marcos = new LinkedHashMap<>(original.marcos());
        marcos.put("Hija.accion()", new MarcoStackProyecto(
                "Hija.accion()", List.of(new SlotStackProyecto(
                "this", "Hija", 1, SlotStackProyecto.Clase.THIS, POSICION))));
        var plan = new PlanMemoriaProyecto(marcos,
                original.layoutsHeap(), original.padresZ());
        var programa = new ProgramaIntermedio();
        agregar(programa, OperadorCuarteta.INICIO_FUNCION,
                "Padre.mostrar()", "void", "1");
        agregar(programa, OperadorCuarteta.FIN_FUNCION,
                "Padre.mostrar()", null, null);
        agregar(programa, OperadorCuarteta.INICIO_FUNCION,
                "Hija.mostrar()", "void", "1");
        agregar(programa, OperadorCuarteta.FIN_FUNCION,
                "Hija.mostrar()", null, null);
        agregar(programa, OperadorCuarteta.INICIO_FUNCION,
                "Hija.accion()", "void", "1");
        agregar(programa, OperadorCuarteta.LLAMAR,
                "super.mostrar()", "0", null);
        agregar(programa, OperadorCuarteta.FIN_FUNCION,
                "Hija.accion()", null, null);
        var resultado = new BajadorLlamadasProyecto().bajar(programa, plan);
        assertTrue(tieneLlamada(resultado, "Padre.mostrar()"));
        assertFalse(tieneLlamada(resultado, "Hija.mostrar()"));
        assertFalse(resultado.cuartetas().stream().anyMatch(c ->
                c.operador() == OperadorCuarteta.LEER_TIPO_OBJETO));
    }

    private boolean tieneLlamada(ProgramaIntermedio programa, String nombre) {
        return programa.cuartetas().stream().anyMatch(c ->
                c.operador() == OperadorCuarteta.LLAMAR && nombre.equals(c.argumento1()));
    }

    private ProgramaIntermedio programaPrincipalConObjeto(
            String objeto, boolean llamarMetodo) {
        var programa = new ProgramaIntermedio();
        for (String clase : List.of("Padre", "Hija", "Nieta")) {
            agregar(programa, OperadorCuarteta.INICIO_FUNCION,
                    clase + ".mostrar()", "void", "1");
            agregar(programa, OperadorCuarteta.FIN_FUNCION,
                    clase + ".mostrar()", null, null);
        }
        agregar(programa, OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0");
        agregar(programa, OperadorCuarteta.NUEVO_OBJETO, objeto, "0", "pig_t0");
        if (llamarMetodo) {
            agregar(programa, OperadorCuarteta.PARAMETRO, "pig_t0", "0", null);
            agregar(programa, OperadorCuarteta.LLAMAR, "Padre.mostrar()", "1", null);
        }
        agregar(programa, OperadorCuarteta.FIN_FUNCION, "MAIOR", null, null);
        return programa;
    }

    private void agregar(ProgramaIntermedio programa, OperadorCuarteta op,
                         String a, String b, String r) {
        programa.agregar(new Cuarteta(op, a, b, r));
    }

    private PlanMemoriaProyecto planConMetodos(Map<String, String> padres,
                                               String... clases) {
        Map<String, LayoutHeapProyecto> layouts = new LinkedHashMap<>();
        Map<String, MarcoStackProyecto> marcos = new LinkedHashMap<>();
        var atributo = new LayoutHeapProyecto.Campo("valor", "int", 0, POSICION);
        marcos.put("MAIOR", new MarcoStackProyecto("MAIOR", List.of()));
        for (String clase : clases) {
            layouts.put("Z::" + clase, new LayoutHeapProyecto(
                    "Z::" + clase, LayoutHeapProyecto.Clase.OBJETO_Z,
                    List.of(atributo)));
            marcos.put(clase + ".mostrar()", new MarcoStackProyecto(
                    clase + ".mostrar()", List.of(
                    new SlotStackProyecto("this", clase, 1,
                            SlotStackProyecto.Clase.THIS, POSICION))));
        }
        return new PlanMemoriaProyecto(marcos, layouts, padres);
    }
}
