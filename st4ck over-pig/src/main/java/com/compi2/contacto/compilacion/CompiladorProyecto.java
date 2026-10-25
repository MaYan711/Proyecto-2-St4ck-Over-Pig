package com.compi2.contacto.compilacion;

import com.compi2.contacto.analisis.AnalizadorArchivo;
import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.ir.GeneradorIntermedioProyecto;
import com.compi2.contacto.ir.ProgramaIntermedio;
import com.compi2.contacto.memoria.BajadorMemoriaProyecto;
import com.compi2.contacto.memoria.PlanMemoriaProyecto;
import com.compi2.contacto.memoria.PlanificadorMemoriaProyecto;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import com.compi2.contacto.semantica.AnalizadorSemanticoPig;
import com.compi2.contacto.semantica.AnalizadorSemanticoY;
import com.compi2.contacto.semantica.AnalizadorSemanticoZ;
import com.compi2.contacto.semantica.ConstructorTablaSimbolosProyecto;
import com.compi2.contacto.semantica.ResultadoSemanticoPig;
import com.compi2.contacto.semantica.ResultadoSemanticoY;
import com.compi2.contacto.semantica.ResultadoSemanticoZ;
import com.compi2.contacto.semantica.TablaSimbolos;
import com.compi2.contacto.memoria.BajadorLlamadasProyecto;
import com.compi2.contacto.memoria.BajadorCadenasProyecto;
import com.compi2.contacto.memoria.BajadorArreglosProyecto;
import com.compi2.contacto.memoria.BajadorInicializadoresProyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CompiladorProyecto {

    private final AnalizadorArchivo analizadorArchivo;

    public CompiladorProyecto() {
        analizadorArchivo =
                new AnalizadorArchivo();
    }

    public ResultadoCompilacion compilar(
            ProyectoCompilacion proyecto
    ) {
        Objects.requireNonNull(
                proyecto,
                "El proyecto es obligatorio"
        );

        List<ResultadoAnalisisArchivo> resultados =
                proyecto.archivos()
                        .stream()
                        .map(
                                analizadorArchivo::analizar
                        )
                        .toList();

        List<Diagnostico> diagnosticosProyecto =
                new ArrayList<>();

        long cantidadPig =
                proyecto.archivos()
                        .stream()
                        .filter(
                                archivo ->
                                        archivo.lenguaje()
                                                == LenguajeFuente.PIG_LATIN
                        )
                        .count();

        if (proyecto.archivos()
                .isEmpty()) {

            diagnosticosProyecto.add(
                    new Diagnostico(
                            TipoDiagnostico.PROYECTO,
                            Severidad.ERROR,
                            proyecto.raiz(),
                            1,
                            0,
                            "El proyecto no contiene archivos .y, .z o .pig"
                    )
            );

        } else if (cantidadPig == 0) {

            diagnosticosProyecto.add(
                    new Diagnostico(
                            TipoDiagnostico.PROYECTO,
                            Severidad.ERROR,
                            proyecto.raiz(),
                            1,
                            0,
                            "El proyecto necesita exactamente un archivo principal .pig"
                    )
            );

        } else if (cantidadPig > 1) {

            diagnosticosProyecto.add(
                    new Diagnostico(
                            TipoDiagnostico.PROYECTO,
                            Severidad.ERROR,
                            proyecto.raiz(),
                            1,
                            0,
                            "El proyecto debe contener exactamente un archivo principal .pig; se encontraron "
                                    + cantidadPig
                    )
            );
        }

        List<ProgramaAst> programasY =
                obtenerProgramas(
                        resultados,
                        LenguajeFuente.Y
                );

        List<ProgramaAst> programasZ =
                obtenerProgramas(
                        resultados,
                        LenguajeFuente.ZETARIANO
                );

        List<ProgramaAst> programasPig =
                obtenerProgramas(
                        resultados,
                        LenguajeFuente.PIG_LATIN
                );

        ResultadoSemanticoY resultadoY =
                new AnalizadorSemanticoY()
                        .analizar(
                                programasY
                        );

        ResultadoSemanticoZ resultadoZ =
                new AnalizadorSemanticoZ()
                        .analizar(
                                programasZ
                        );

        ResultadoSemanticoPig resultadoPig =
                new AnalizadorSemanticoPig()
                        .analizar(
                                proyecto,
                                resultados
                        );

        List<Diagnostico> diagnosticosSemanticos =
                new ArrayList<>();

        diagnosticosSemanticos.addAll(
                resultadoY.diagnosticos()
        );

        diagnosticosSemanticos.addAll(
                resultadoZ.diagnosticos()
        );

        diagnosticosSemanticos.addAll(
                resultadoPig.diagnosticos()
        );

        TablaSimbolos tablaProyecto =
                new ConstructorTablaSimbolosProyecto()
                        .construir(
                                programasY,
                                programasZ,
                                programasPig
                        );

        ProgramaIntermedio intermedio =
                new ProgramaIntermedio();

        PlanMemoriaProyecto planMemoria =
                new PlanMemoriaProyecto(
                        Map.of(),
                        Map.of()
                );

        ProgramaIntermedio programaMemoria =
                new ProgramaIntermedio();

        boolean analisisArchivosValido =
                resultados.stream()
                        .allMatch(
                                ResultadoAnalisisArchivo::esValido
                        );

        boolean proyectoValido =
                analisisArchivosValido
                        && diagnosticosProyecto.stream()
                        .noneMatch(
                                Diagnostico::esError
                        )
                        && resultadoY.esValido()
                        && resultadoZ.esValido()
                        && resultadoPig.esValido();

        if (proyectoValido) {
            intermedio =
                    new GeneradorIntermedioProyecto()
                            .generar(
                                    programasY,
                                    programasZ,
                                    programasPig,
                                    resultadoPig.enlaces()
                            );

            planMemoria =
                    new PlanificadorMemoriaProyecto()
                            .planificar(
                                    programasY,
                                    programasZ,
                                    programasPig
                            );

            programaMemoria =
                    new BajadorMemoriaProyecto()
                            .bajar(
                                    intermedio,
                                    planMemoria
                            );

            programaMemoria =
                    new BajadorArreglosProyecto()
                            .bajar(
                                    programaMemoria,
                                    planMemoria
                            );

            programaMemoria =
                    new BajadorInicializadoresProyecto()
                            .bajar(
                                    programaMemoria,
                                    planMemoria
                            );

            programaMemoria =
                    new BajadorLlamadasProyecto()
                            .bajar(
                                    programaMemoria,
                                    planMemoria
                            );

            programaMemoria =
                    new BajadorCadenasProyecto()
                            .bajar(
                                    programaMemoria
                            );
        }

        return new ResultadoCompilacion(
                proyecto,
                resultados,
                diagnosticosProyecto,
                diagnosticosSemanticos,
                tablaProyecto,
                intermedio,
                planMemoria,
                programaMemoria
        );
    }

    private List<ProgramaAst> obtenerProgramas(
            List<ResultadoAnalisisArchivo> resultados,
            LenguajeFuente lenguaje
    ) {
        return resultados.stream()
                .filter(
                        resultado ->
                                resultado.archivo()
                                        .lenguaje()
                                        == lenguaje
                )
                .filter(
                        ResultadoAnalisisArchivo::esValido
                )
                .flatMap(
                        resultado ->
                                resultado.ast()
                                        .stream()
                )
                .toList();
    }
}