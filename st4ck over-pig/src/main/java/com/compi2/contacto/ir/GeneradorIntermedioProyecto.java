package com.compi2.contacto.ir;

import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.semantica.EnlacesPig;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeneradorIntermedioProyecto {

    private static final Pattern TEMPORAL =
            Pattern.compile(
                    "\\$t(\\d+)"
            );

    private static final Pattern ETIQUETA =
            Pattern.compile(
                    "\\$L(\\d+)"
            );

    public ProgramaIntermedio generar(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ,
            List<ProgramaAst> programasPig
    ) {
        return generar(
                programasY,
                programasZ,
                programasPig,
                new EnlacesPig()
        );
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ,
            List<ProgramaAst> programasPig,
            EnlacesPig enlaces
    ) {
        Objects.requireNonNull(
                programasY,
                "Los programas Y son obligatorios"
        );

        Objects.requireNonNull(
                programasZ,
                "Los programas Z son obligatorios"
        );

        Objects.requireNonNull(
                programasPig,
                "Los programas Pig son obligatorios"
        );

        Objects.requireNonNull(
                enlaces,
                "Los enlaces son obligatorios"
        );

        ProgramaIntermedio y =
                ProgramaIntermedio.conNombresProtegidos(
                        () ->
                                new GeneradorCuartetasY()
                                        .generar(
                                                programasY
                                        )
                );

        ProgramaIntermedio z =
                ProgramaIntermedio.conNombresProtegidos(
                        () ->
                                new GeneradorCuartetasZ()
                                        .generar(
                                                programasZ
                                        )
                );

        ProgramaIntermedio pig =
                ProgramaIntermedio.conNombresProtegidos(
                        () ->
                                new GeneradorCuartetasPig()
                                        .generar(
                                                programasPig,
                                                enlaces
                                        )
                );

        ProgramaIntermedio resultado =
                new ProgramaIntermedio();

        copiar(
                y,
                resultado,
                "y"
        );

        copiar(
                z,
                resultado,
                "z"
        );

        copiar(
                pig,
                resultado,
                "pig"
        );

        return resultado;
    }

    private void copiar(
            ProgramaIntermedio origen,
            ProgramaIntermedio destino,
            String prefijo
    ) {
        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            destino.agregar(
                    new Cuarteta(
                            cuarteta.operador(),
                            renombrar(
                                    cuarteta.argumento1(),
                                    prefijo
                            ),
                            renombrar(
                                    cuarteta.argumento2(),
                                    prefijo
                            ),
                            renombrar(
                                    cuarteta.resultado(),
                                    prefijo
                            )
                    )
            );
        }
    }

    private String renombrar(
            String valor,
            String prefijo
    ) {
        if (valor == null) {
            return null;
        }

        StringBuilder resultado =
                new StringBuilder();

        StringBuilder segmento =
                new StringBuilder();

        boolean enCadena = false;
        boolean enCaracter = false;
        boolean escapado = false;

        for (int indice = 0;
             indice < valor.length();
             indice++) {

            char caracter =
                    valor.charAt(
                            indice
                    );

            if (enCadena || enCaracter) {
                resultado.append(
                        caracter
                );

                if (escapado) {
                    escapado = false;
                    continue;
                }

                if (caracter == '\\') {
                    escapado = true;
                    continue;
                }

                if (enCadena
                        && caracter == '"') {
                    enCadena = false;
                    continue;
                }

                if (enCaracter
                        && caracter == '\'') {
                    enCaracter = false;
                }

                continue;
            }

            if (caracter == '"'
                    || caracter == '\'') {

                resultado.append(
                        renombrarSegmento(
                                segmento.toString(),
                                prefijo
                        )
                );

                segmento.setLength(
                        0
                );

                resultado.append(
                        caracter
                );

                enCadena =
                        caracter == '"';

                enCaracter =
                        caracter == '\'';

                continue;
            }

            segmento.append(
                    caracter
            );
        }

        resultado.append(
                renombrarSegmento(
                        segmento.toString(),
                        prefijo
                )
        );

        return resultado.toString();
    }

    private String renombrarSegmento(
            String valor,
            String prefijo
    ) {
        String resultado =
                reemplazar(
                        valor,
                        TEMPORAL,
                        prefijo + "_t"
                );

        return reemplazar(
                resultado,
                ETIQUETA,
                prefijo + "_L"
        );
    }

    private String reemplazar(
            String valor,
            Pattern patron,
            String prefijo
    ) {
        Matcher matcher =
                patron.matcher(
                        valor
                );

        StringBuffer resultado =
                new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(
                    resultado,
                    Matcher.quoteReplacement(
                            prefijo
                                    + matcher.group(1)
                    )
            );
        }

        matcher.appendTail(
                resultado
        );

        return resultado.toString();
    }
}
