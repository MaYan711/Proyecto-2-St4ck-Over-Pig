package com.compi2.contacto.compilacion;

import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.ir.ProgramaIntermedio;
import com.compi2.contacto.memoria.PlanMemoriaProyecto;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import com.compi2.contacto.semantica.TablaSimbolos;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public record ResultadoCompilacion(
        ProyectoCompilacion proyecto,
        List<ResultadoAnalisisArchivo> analisisArchivos,
        List<Diagnostico> diagnosticosProyecto,
        List<Diagnostico> diagnosticosSemanticos,
        TablaSimbolos tablaSimbolos,
        ProgramaIntermedio programaIntermedio,
        PlanMemoriaProyecto planMemoria,
        ProgramaIntermedio programaMemoria
) {

    public ResultadoCompilacion {
        Objects.requireNonNull(
                proyecto,
                "El proyecto es obligatorio"
        );

        analisisArchivos = List.copyOf(
                Objects.requireNonNull(
                        analisisArchivos,
                        "Los analisis son obligatorios"
                )
        );

        diagnosticosProyecto = List.copyOf(
                Objects.requireNonNull(
                        diagnosticosProyecto,
                        "Los diagnosticos del proyecto son obligatorios"
                )
        );

        diagnosticosSemanticos = List.copyOf(
                Objects.requireNonNull(
                        diagnosticosSemanticos,
                        "Los diagnosticos semanticos son obligatorios"
                )
        );

        Objects.requireNonNull(
                tablaSimbolos,
                "La tabla es obligatoria"
        );

        Objects.requireNonNull(
                programaIntermedio,
                "El programa intermedio es obligatorio"
        );

        Objects.requireNonNull(
                planMemoria,
                "El plan de memoria es obligatorio"
        );

        Objects.requireNonNull(
                programaMemoria,
                "El programa de memoria es obligatorio"
        );
    }

    public ResultadoCompilacion(
            ProyectoCompilacion proyecto,
            List<ResultadoAnalisisArchivo> analisisArchivos,
            List<Diagnostico> diagnosticosProyecto,
            List<Diagnostico> diagnosticosSemanticos,
            TablaSimbolos tablaSimbolos,
            ProgramaIntermedio programaIntermedio
    ) {
        this(
                proyecto,
                analisisArchivos,
                diagnosticosProyecto,
                diagnosticosSemanticos,
                tablaSimbolos,
                programaIntermedio,
                new PlanMemoriaProyecto(
                        Map.of(),
                        Map.of()
                ),
                new ProgramaIntermedio()
        );
    }

    public ResultadoCompilacion(
            ProyectoCompilacion proyecto,
            List<ResultadoAnalisisArchivo> analisisArchivos,
            List<Diagnostico> diagnosticosProyecto,
            TablaSimbolos tablaSimbolos,
            ProgramaIntermedio programaIntermedio
    ) {
        this(
                proyecto,
                analisisArchivos,
                diagnosticosProyecto,
                List.of(),
                tablaSimbolos,
                programaIntermedio
        );
    }

    public List<Diagnostico> diagnosticos() {
        return Stream.of(
                        diagnosticosProyecto.stream(),
                        analisisArchivos.stream()
                                .flatMap(
                                        resultado ->
                                                resultado.diagnosticos()
                                                        .stream()
                                ),
                        diagnosticosSemanticos.stream()
                )
                .flatMap(
                        flujo ->
                                flujo
                )
                .toList();
    }

    public boolean esValido() {
        return diagnosticos()
                .stream()
                .noneMatch(
                        Diagnostico::esError
                );
    }
}