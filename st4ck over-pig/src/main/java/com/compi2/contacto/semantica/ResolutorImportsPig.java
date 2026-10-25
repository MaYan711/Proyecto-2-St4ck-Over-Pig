package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.EstructuraAst;
import com.compi2.contacto.ast.FuncionAst;
import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ResolutorImportsPig {

    public ResultadoImportsPig resolver(
            ProyectoCompilacion proyecto,
            PAst.Programa programa,
            List<ResultadoAnalisisArchivo> resultados
    ) {
        Objects.requireNonNull(
                proyecto,
                "El proyecto es obligatorio"
        );

        Objects.requireNonNull(
                programa,
                "El programa Pig es obligatorio"
        );

        Objects.requireNonNull(
                resultados,
                "Los resultados son obligatorios"
        );

        Map<Path, ResultadoAnalisisArchivo> porRuta =
                construirIndice(resultados);

        Map<String, EstructuraAst> estructuras =
                new LinkedHashMap<>();

        Map<String, FuncionAst> funciones =
                new LinkedHashMap<>();

        Map<String, ZAst.Clase> clases =
                new LinkedHashMap<>();

        List<Diagnostico> diagnosticos =
                new ArrayList<>();

        Set<Path> procesados =
                new LinkedHashSet<>();

        for (PAst.Importacion importacion
                : programa.importaciones()) {

            procesarImportacion(
                    proyecto,
                    importacion,
                    porRuta,
                    procesados,
                    estructuras,
                    funciones,
                    clases,
                    diagnosticos
            );
        }

        return new ResultadoImportsPig(
                estructuras,
                funciones,
                clases,
                diagnosticos
        );
    }

    private Map<Path, ResultadoAnalisisArchivo> construirIndice(
            List<ResultadoAnalisisArchivo> resultados
    ) {
        Map<Path, ResultadoAnalisisArchivo> indice =
                new LinkedHashMap<>();

        for (ResultadoAnalisisArchivo resultado
                : resultados) {

            indice.put(
                    normalizar(
                            resultado.archivo()
                                    .ruta()
                    ),
                    resultado
            );
        }

        return indice;
    }

    private void procesarImportacion(
            ProyectoCompilacion proyecto,
            PAst.Importacion importacion,
            Map<Path, ResultadoAnalisisArchivo> porRuta,
            Set<Path> procesados,
            Map<String, EstructuraAst> estructuras,
            Map<String, FuncionAst> funciones,
            Map<String, ZAst.Clase> clases,
            List<Diagnostico> diagnosticos
    ) {
        String extension =
                importacion.extension();

        if (!extension.equals("y")
                && !extension.equals("z")) {

            error(
                    importacion,
                    "Pig Latin solo puede importar archivos .y o .z",
                    diagnosticos
            );

            return;
        }

        Path esperada =
                resolverRuta(
                        proyecto.raiz(),
                        importacion
                );

        ResultadoAnalisisArchivo resultado =
                porRuta.get(
                        esperada
                );

        if (resultado == null) {
            error(
                    importacion,
                    "No existe el archivo importado '"
                            + importacion.ruta()
                            + "'",
                    diagnosticos
            );

            return;
        }

        LenguajeFuente lenguajeEsperado =
                extension.equals("y")
                        ? LenguajeFuente.Y
                        : LenguajeFuente.ZETARIANO;

        if (resultado.archivo()
                .lenguaje()
                != lenguajeEsperado) {

            error(
                    importacion,
                    "El archivo '"
                            + importacion.ruta()
                            + "' no corresponde al lenguaje esperado",
                    diagnosticos
            );

            return;
        }

        if (!resultado.esValido()
                || resultado.ast()
                .isEmpty()) {

            error(
                    importacion,
                    "El archivo importado '"
                            + importacion.ruta()
                            + "' contiene errores",
                    diagnosticos
            );

            return;
        }

        if (!procesados.add(
                esperada
        )) {
            return;
        }

        if (lenguajeEsperado
                == LenguajeFuente.Y) {

            cargarY(
                    resultado,
                    importacion,
                    estructuras,
                    funciones,
                    clases,
                    diagnosticos
            );

        } else {

            cargarZ(
                    resultado,
                    importacion,
                    estructuras,
                    clases,
                    diagnosticos
            );
        }
    }

    private void cargarY(
            ResultadoAnalisisArchivo resultado,
            PAst.Importacion importacion,
            Map<String, EstructuraAst> estructuras,
            Map<String, FuncionAst> funciones,
            Map<String, ZAst.Clase> clases,
            List<Diagnostico> diagnosticos
    ) {
        for (NodoAst nodo
                : resultado.ast()
                .orElseThrow()
                .elementos()) {

            if (nodo
                    instanceof EstructuraAst estructura) {

                if (clases.containsKey(
                        estructura.nombre()
                )) {
                    error(
                            importacion,
                            "El tipo '"
                                    + estructura.nombre()
                                    + "' es ambiguo porque existe como estructura Y? y clase Zetariano",
                            diagnosticos
                    );

                    continue;
                }

                EstructuraAst anterior =
                        estructuras.putIfAbsent(
                                estructura.nombre(),
                                estructura
                        );

                if (anterior != null) {
                    error(
                            importacion,
                            "La estructura importada '"
                                    + estructura.nombre()
                                    + "' ya estaba disponible",
                            diagnosticos
                    );
                }

                continue;
            }

            if (nodo
                    instanceof FuncionAst funcion) {

                FuncionAst anterior =
                        funciones.putIfAbsent(
                                funcion.nombre(),
                                funcion
                        );

                if (anterior != null) {
                    error(
                            importacion,
                            "La funcion importada '"
                                    + funcion.nombre()
                                    + "' ya estaba disponible",
                            diagnosticos
                    );
                }
            }
        }
    }

    private void cargarZ(
            ResultadoAnalisisArchivo resultado,
            PAst.Importacion importacion,
            Map<String, EstructuraAst> estructuras,
            Map<String, ZAst.Clase> clases,
            List<Diagnostico> diagnosticos
    ) {
        for (NodoAst nodo
                : resultado.ast()
                .orElseThrow()
                .elementos()) {

            if (!(nodo
                    instanceof ZAst.Clase clase)) {
                continue;
            }

            if (estructuras.containsKey(
                    clase.nombre()
            )) {
                error(
                        importacion,
                        "El tipo '"
                                + clase.nombre()
                                + "' es ambiguo porque existe como estructura Y? y clase Zetariano",
                        diagnosticos
                );

                continue;
            }

            ZAst.Clase anterior =
                    clases.putIfAbsent(
                            clase.nombre(),
                            clase
                    );

            if (anterior != null) {
                error(
                        importacion,
                        "La clase importada '"
                                + clase.nombre()
                                + "' ya estaba disponible",
                        diagnosticos
                );
            }
        }
    }

    private Path resolverRuta(
            Path raiz,
            PAst.Importacion importacion
    ) {
        String ruta =
                importacion.ruta();

        String extension =
                importacion.extension();

        String sufijo =
                "." + extension;

        String base =
                ruta.endsWith(sufijo)
                        ? ruta.substring(
                        0,
                        ruta.length()
                                - sufijo.length()
                )
                        : ruta;

        String[] segmentos =
                base.split("\\.");

        Path relativa =
                Path.of("");

        for (int indice = 0;
             indice < segmentos.length - 1;
             indice++) {

            relativa =
                    relativa.resolve(
                            segmentos[indice]
                    );
        }

        String archivo =
                segmentos[
                        segmentos.length - 1
                        ]
                        + "."
                        + extension;

        relativa =
                relativa.resolve(
                        archivo
                );

        return normalizar(
                raiz.resolve(
                        relativa
                )
        );
    }

    private Path normalizar(
            Path ruta
    ) {
        return ruta.toAbsolutePath()
                .normalize();
    }

    private void error(
            PAst.Importacion importacion,
            String mensaje,
            List<Diagnostico> diagnosticos
    ) {
        diagnosticos.add(
                new Diagnostico(
                        TipoDiagnostico.SEMANTICO,
                        Severidad.ERROR,
                        importacion.posicion()
                                .archivo(),
                        importacion.posicion()
                                .linea(),
                        importacion.posicion()
                                .columna(),
                        mensaje
                )
        );
    }
}