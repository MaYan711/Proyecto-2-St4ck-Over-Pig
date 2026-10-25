package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.Objects;

public final class GeneradorCodigoTresDirecciones {

    public String generar(
            ProgramaIntermedio programa
    ) {
        Objects.requireNonNull(
                programa,
                "El programa intermedio es obligatorio"
        );

        StringBuilder codigo =
                new StringBuilder();

        codigo.append("P = 0;")
                .append(System.lineSeparator());

        codigo.append("H = 0;")
                .append(System.lineSeparator());

        boolean primeraFuncion =
                true;

        boolean dentroFuncion =
                false;

        for (Cuarteta cuarteta
                : programa.cuartetas()) {

            if (cuarteta.operador()
                    .name()
                    .equals("INICIO_FUNCION")) {

                if (!primeraFuncion) {
                    codigo.append(
                            System.lineSeparator()
                    );
                }

                codigo.append(
                        "func "
                );

                codigo.append(
                        cuarteta.argumento1()
                );

                codigo.append(
                        ":"
                );

                codigo.append(
                        System.lineSeparator()
                );

                primeraFuncion = false;
                dentroFuncion = true;

                continue;
            }

            if (cuarteta.operador()
                    .name()
                    .equals("FIN_FUNCION")) {

                codigo.append(
                        "endfunc "
                );

                codigo.append(
                        cuarteta.argumento1()
                );

                codigo.append(
                        System.lineSeparator()
                );

                dentroFuncion = false;

                continue;
            }

            String linea =
                    generarLinea(
                            cuarteta
                    );

            if (linea.isBlank()) {
                continue;
            }

            if (dentroFuncion
                    && cuarteta.operador()
                    .name()
                    .equals("ETIQUETA")) {

                codigo.append(
                        linea
                );

            } else if (dentroFuncion) {

                codigo.append(
                        "    "
                );

                codigo.append(
                        linea
                );

            } else {

                codigo.append(
                        linea
                );
            }

            codigo.append(
                    System.lineSeparator()
            );
        }

        return codigo.toString();
    }

    private String generarLinea(
            Cuarteta cuarteta
    ) {
        return switch (cuarteta.operador()) {

            case DECLARAR ->
                    declaracion(
                            cuarteta
                    );

            case DECLARAR_PARAMETRO ->
                    declaracionParametro(
                            cuarteta
                    );

            case ASIGNAR ->
                    cuarteta.resultado()
                            + " = "
                            + cuarteta.argumento1()
                            + ";";

            case INICIALIZAR_COMPUESTO ->
                    cuarteta.resultado()
                            + " = init("
                            + cuarteta.argumento1()
                            + ");";

            case SUMAR ->
                    binaria(
                            cuarteta,
                            "+"
                    );

            case RESTAR ->
                    binaria(
                            cuarteta,
                            "-"
                    );

            case MULTIPLICAR ->
                    binaria(
                            cuarteta,
                            "*"
                    );

            case DIVIDIR ->
                    binaria(
                            cuarteta,
                            "/"
                    );

            case MODULO ->
                    binaria(
                            cuarteta,
                            "%"
                    );

            case NEGATIVO ->
                    cuarteta.resultado()
                            + " = -"
                            + cuarteta.argumento1()
                            + ";";

            case NEGAR ->
                    cuarteta.resultado()
                            + " = !"
                            + cuarteta.argumento1()
                            + ";";

            case IGUALDAD ->
                    binaria(
                            cuarteta,
                            "=="
                    );

            case DIFERENTE ->
                    binaria(
                            cuarteta,
                            "!="
                    );

            case MENOR ->
                    binaria(
                            cuarteta,
                            "<"
                    );

            case MAYOR ->
                    binaria(
                            cuarteta,
                            ">"
                    );

            case MENOR_IGUAL ->
                    binaria(
                            cuarteta,
                            "<="
                    );

            case MAYOR_IGUAL ->
                    binaria(
                            cuarteta,
                            ">="
                    );

            case AND ->
                    binaria(
                            cuarteta,
                            "&&"
                    );

            case OR ->
                    binaria(
                            cuarteta,
                            "||"
                    );

            case COMPARAR ->
                    cuarteta.resultado()
                            + " = compare("
                            + cuarteta.argumento1()
                            + ", "
                            + cuarteta.argumento2()
                            + ");";

            case ETIQUETA ->
                    cuarteta.resultado()
                            + ":";

            case SALTAR ->
                    "goto "
                            + cuarteta.resultado()
                            + ";";

            case SALTAR_SI_FALSO ->
                    "ifFalse "
                            + cuarteta.argumento1()
                            + " goto "
                            + cuarteta.resultado()
                            + ";";

            case SALTAR_SI_VERDADERO ->
                    "ifTrue "
                            + cuarteta.argumento1()
                            + " goto "
                            + cuarteta.resultado()
                            + ";";

            case PARAMETRO ->
                    "param["
                            + cuarteta.argumento2()
                            + "] = "
                            + cuarteta.argumento1()
                            + ";";

            case LLAMAR ->
                    "call "
                            + cuarteta.argumento1()
                            + ", "
                            + cuarteta.argumento2()
                            + ";";

            case RETORNAR ->
                    retorno(
                            cuarteta
                    );

            case NUEVO_OBJETO ->
                    cuarteta.resultado()
                            + " = newobj "
                            + cuarteta.argumento1()
                            + ", "
                            + cuarteta.argumento2()
                            + ";";

            case NUEVO_ARREGLO ->
                    cuarteta.resultado()
                            + " = newarr "
                            + cuarteta.argumento1()
                            + ", "
                            + cuarteta.argumento2()
                            + ";";

            case LEER ->
                    lectura(
                            cuarteta
                    );

            case LEER_STACK ->
                    cuarteta.resultado()
                            + " = Stack[(int)("
                            + cuarteta.argumento1()
                            + ")];";

            case ESCRIBIR_STACK ->
                    "Stack[(int)("
                            + cuarteta.argumento1()
                            + ")] = "
                            + cuarteta.argumento2()
                            + ";";

            case LEER_HEAP ->
                    cuarteta.resultado()
                            + " = Heap[(int)("
                            + cuarteta.argumento1()
                            + ")];";

            case ESCRIBIR_HEAP ->
                    "Heap[(int)("
                            + cuarteta.argumento1()
                            + ")] = "
                            + cuarteta.argumento2()
                            + ";";

            case LEER_TIPO_OBJETO ->
                    cuarteta.resultado() + " = TipoObjetoZ[(int)("
                            + cuarteta.argumento1() + ")];";

            case ESCRIBIR_TIPO_OBJETO ->
                    "TipoObjetoZ[(int)(" + cuarteta.argumento1()
                            + ")] = " + cuarteta.argumento2() + ";";

            case IMPRIMIR ->
                    imprimir(
                            cuarteta
                    );

            case INICIO_FUNCION,
                 FIN_FUNCION ->
                    "";
        };
    }

    private String declaracion(
            Cuarteta cuarteta
    ) {
        StringBuilder resultado =
                new StringBuilder();

        resultado.append(
                "decl "
        );

        resultado.append(
                cuarteta.argumento1()
        );

        resultado.append(
                " "
        );

        resultado.append(
                cuarteta.resultado()
        );

        if (!vacio(
                cuarteta.argumento2()
        )) {

            String dimensiones =
                    cuarteta.argumento2()
                            .replace(
                                    ",",
                                    "]["
                            );

            resultado.append(
                    "["
            );

            resultado.append(
                    dimensiones
            );

            resultado.append(
                    "]"
            );
        }

        resultado.append(
                ";"
        );

        return resultado.toString();
    }

    private String declaracionParametro(
            Cuarteta cuarteta
    ) {
        return "paramdecl "
                + cuarteta.argumento1()
                + " "
                + cuarteta.resultado()
                + ";";
    }

    private String binaria(
            Cuarteta cuarteta,
            String operador
    ) {
        return cuarteta.resultado()
                + " = "
                + cuarteta.argumento1()
                + " "
                + operador
                + " "
                + cuarteta.argumento2()
                + ";";
    }

    private String retorno(
            Cuarteta cuarteta
    ) {
        if (vacio(
                cuarteta.argumento1()
        )) {

            return "return;";
        }

        return "return "
                + cuarteta.argumento1()
                + ";";
    }

    private String lectura(
            Cuarteta cuarteta
    ) {
        if (!vacio(
                cuarteta.resultado()
        )) {

            return cuarteta.resultado()
                    + " = read("
                    + valorSeguro(
                    cuarteta.argumento1()
            )
                    + ");";
        }

        return "read("
                + valorSeguro(
                cuarteta.argumento1()
        )
                + ");";
    }

    private String imprimir(
            Cuarteta cuarteta
    ) {
        String funcion =
                "print";

        if ("println".equalsIgnoreCase(
                cuarteta.argumento2()
        )) {

            funcion =
                    "println";
        }

        return funcion
                + "("
                + cuarteta.argumento1()
                + ");";
    }

    private String valorSeguro(
            String valor
    ) {
        return vacio(
                valor
        )
                ? ""
                : valor;
    }

    private boolean vacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
    }
}