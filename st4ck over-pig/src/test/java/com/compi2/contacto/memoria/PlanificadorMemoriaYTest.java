package com.compi2.contacto.memoria;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanificadorMemoriaYTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void asignaOffsetsDeStackYLayoutDeHeap() {
        String codigo = """
                %estructuras
                estructura Persona:
                    cadena nombre
                    entero edad
                    entero notas[3]
                %funciones
                definir prueba(
                    entero valor,
                    [] entero datos,
                    {} Persona persona
                ) -> entero:
                    entero total = valor
                    entero temporal[2]
                    Persona copia = {"Mario", 20, {1, 2, 3}}
                    si(valor > 0) entonces
                        entero interno = 1
                    retornar total
                """;

        ProgramaAst ast =
                analizar(codigo);

        PlanMemoriaY plan =
                new PlanificadorMemoriaY()
                        .planificar(
                                List.of(ast)
                        );

        MarcoStackY marco =
                plan.marco("prueba")
                        .orElseThrow();

        /*
         * P+0 retorno
         * P+1 valor
         * P+2 datos
         * P+3 persona
         * P+4 total
         * P+5 temporal
         * P+6 copia
         * P+7 interno
         */
        assertEquals(
                8,
                marco.tamano()
        );

        SlotStackY retorno =
                marco.retorno()
                        .orElseThrow();

        assertEquals(
                0,
                retorno.direccion()
                        .desplazamiento()
        );

        SlotStackY valor =
                marco.primerSlot("valor")
                        .orElseThrow();

        assertEquals(
                1,
                valor.direccion()
                        .desplazamiento()
        );

        assertFalse(
                valor.referencia()
        );

        SlotStackY datos =
                marco.primerSlot("datos")
                        .orElseThrow();

        assertEquals(
                2,
                datos.direccion()
                        .desplazamiento()
        );

        assertTrue(
                datos.referencia()
        );

        SlotStackY persona =
                marco.primerSlot("persona")
                        .orElseThrow();

        assertTrue(
                persona.referencia()
        );

        SlotStackY temporal =
                marco.primerSlot("temporal")
                        .orElseThrow();

        assertTrue(
                temporal.referencia()
        );

        SlotStackY copia =
                marco.primerSlot("copia")
                        .orElseThrow();

        assertTrue(
                copia.referencia()
        );

        LayoutEstructuraY estructura =
                plan.estructuraGlobal("Persona")
                        .orElseThrow();

        assertEquals(
                3,
                estructura.tamanoCeldas()
        );

        CampoHeapY nombre =
                estructura.campo("nombre")
                        .orElseThrow();

        assertEquals(
                0,
                nombre.desplazamiento()
        );

        assertFalse(
                nombre.referencia()
        );

        CampoHeapY edad =
                estructura.campo("edad")
                        .orElseThrow();

        assertEquals(
                1,
                edad.desplazamiento()
        );

        CampoHeapY notas =
                estructura.campo("notas")
                        .orElseThrow();

        assertEquals(
                2,
                notas.desplazamiento()
        );

        assertTrue(
                notas.referencia()
        );

        assertEquals(
                1,
                notas.dimensiones()
        );
    }

    @Test
    void reservaRetornoAunqueLaFuncionSeaVoid() {
        String codigo = """
                %funciones
                definir prueba():
                    entero x = 10
                """;

        ProgramaAst ast =
                analizar(codigo);

        PlanMemoriaY plan =
                new PlanificadorMemoriaY()
                        .planificar(
                                List.of(ast)
                        );

        MarcoStackY marco =
                plan.marco("prueba")
                        .orElseThrow();

        assertEquals(
                2,
                marco.tamano()
        );

        assertEquals(
                "void",
                marco.retorno()
                        .orElseThrow()
                        .tipoDeclarado()
        );

        assertEquals(
                1,
                marco.primerSlot("x")
                        .orElseThrow()
                        .direccion()
                        .desplazamiento()
        );
    }

    @Test
    void calculaInicioDeDatosParaArregloAplanado() {
        assertEquals(
                2,
                ConvencionMemoriaY
                        .inicioDatosArreglo(1)
        );

        assertEquals(
                3,
                ConvencionMemoriaY
                        .inicioDatosArreglo(2)
        );

        assertEquals(
                4,
                ConvencionMemoriaY
                        .inicioDatosArreglo(3)
        );
    }

    private ProgramaAst analizar(
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Memoria.y"),
                        LenguajeFuente.Y,
                        codigo
                );

        ResultadoAnalisisArchivo resultado =
                analizador.analizar(
                        archivo
                );

        assertTrue(
                resultado.esValido(),
                "El programa debe ser sintacticamente valido"
        );

        return resultado.ast()
                .orElseThrow();
    }
}