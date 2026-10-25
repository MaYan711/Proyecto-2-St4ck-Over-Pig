package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeneradorCodigoC {

    private static final Pattern TEMPORAL =
            Pattern.compile(
                    "\\b(?:y_t|z_t|pig_t|mem_t|call_t|obj_t|str_t|str_h|arr_t|init_h)\\d+\\b"
            );

    private static final String MARCA_CADENA =
            "@STR@";

    public String generar(
            ProgramaIntermedio programa
    ) {
        Objects.requireNonNull(
                programa,
                "El programa intermedio es obligatorio"
        );

        Map<String, List<Cuarteta>> funciones =
                separarFunciones(
                        programa
                );

        Map<String, String> nombres =
                construirNombres(
                        funciones.keySet()
                );

        StringBuilder codigo =
                new StringBuilder();

        cabecera(
                codigo
        );

        prototipos(
                codigo,
                funciones,
                nombres
        );

        for (Map.Entry<String, List<Cuarteta>> entrada
                : funciones.entrySet()) {

            generarFuncion(
                    codigo,
                    entrada.getKey(),
                    entrada.getValue(),
                    nombres
            );
        }

        generarMain(
                codigo,
                nombres
        );

        return codigo.toString();
    }

    private Map<String, List<Cuarteta>> separarFunciones(
            ProgramaIntermedio programa
    ) {
        Map<String, List<Cuarteta>> funciones =
                new LinkedHashMap<>();

        String actual =
                null;

        for (Cuarteta cuarteta
                : programa.cuartetas()) {

            if (cuarteta.operador()
                    == OperadorCuarteta.INICIO_FUNCION) {

                actual =
                        cuarteta.argumento1();

                funciones.put(
                        actual,
                        new ArrayList<>()
                );

                continue;
            }

            if (cuarteta.operador()
                    == OperadorCuarteta.FIN_FUNCION) {

                actual =
                        null;

                continue;
            }

            if (actual != null) {

                funciones.get(
                        actual
                ).add(
                        cuarteta
                );
            }
        }

        return funciones;
    }

    private Map<String, String> construirNombres(
            Set<String> funciones
    ) {
        Map<String, String> resultado =
                new LinkedHashMap<>();

        for (String funcion
                : funciones) {

            resultado.put(
                    funcion,
                    sanitizarFuncion(
                            funcion
                    )
            );
        }

        return resultado;
    }

    private void cabecera(
            StringBuilder codigo
    ) {
        codigo.append(
                "#include <stdio.h>\n"
        );

        codigo.append(
                "#include <stdlib.h>\n"
        );

        codigo.append(
                "#include <math.h>\n"
        );

        codigo.append(
                "#include <stdbool.h>\n"
        );

        codigo.append(
                "#include <string.h>\n\n"
        );

        codigo.append(
                "#define STACK_SIZE 100000\n"
        );

        codigo.append(
                "#define HEAP_SIZE 1000000\n\n"
        );

        codigo.append(
                "double Stack[STACK_SIZE];\n"
        );

        codigo.append(
                "double Heap[HEAP_SIZE];\n"
        );

        codigo.append(
                "int TipoObjetoZ[HEAP_SIZE];\n"
        );

        codigo.append(
                "int P = 0;\n"
        );

        codigo.append(
                "int H = 0;\n\n"
        );

        codigo.append(
                "static int runtime_tipo_objeto_z(double referencia) {\n"
        );
        codigo.append(
                "    int indice = (int) referencia;\n"
        );
        codigo.append(
                "    if (indice < 0 || indice >= HEAP_SIZE || TipoObjetoZ[indice] == 0) {\n"
        );
        codigo.append(
                "        fprintf(stderr, \"Error: referencia nula o invalida en llamada virtual\\n\");\n"
        );
        codigo.append(
                "        exit(1);\n"
        );
        codigo.append(
                "    }\n"
        );
        codigo.append(
                "    return TipoObjetoZ[indice];\n"
        );
        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static void runtime_print_string(double referencia, int salto) {\n"
        );

        codigo.append(
                "    int posicion = (int) referencia;\n"
        );

        codigo.append(
                "    while (posicion >= 0 && posicion < HEAP_SIZE && Heap[posicion] != -1) {\n"
        );

        codigo.append(
                "        putchar((unsigned char) ((int) Heap[posicion]));\n"
        );

        codigo.append(
                "        posicion++;\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "    if (salto) {\n"
        );

        codigo.append(
                "        putchar('\\n');\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static void runtime_append_string(double referencia) {\n"
        );

        codigo.append(
                "    int posicion = (int) referencia;\n"
        );

        codigo.append(
                "    if (posicion < 0) {\n"
        );

        codigo.append(
                "        const char *texto = \"null\";\n"
        );

        codigo.append(
                "        for (int i = 0; texto[i] != '\\0'; i++) {\n"
        );

        codigo.append(
                "            if (H < HEAP_SIZE) Heap[H++] = (unsigned char) texto[i];\n"
        );

        codigo.append(
                "        }\n"
        );

        codigo.append(
                "        return;\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "    while (posicion >= 0 && posicion < HEAP_SIZE && Heap[posicion] != -1) {\n"
        );

        codigo.append(
                "        if (H < HEAP_SIZE) Heap[H++] = Heap[posicion];\n"
        );

        codigo.append(
                "        posicion++;\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static void runtime_append_number(double valor) {\n"
        );

        codigo.append(
                "    char buffer[128];\n"
        );

        codigo.append(
                "    int cantidad = snprintf(buffer, sizeof(buffer), \"%.15g\", valor);\n"
        );

        codigo.append(
                "    if (cantidad < 0) return;\n"
        );

        codigo.append(
                "    for (int i = 0; i < cantidad && i < (int) sizeof(buffer); i++) {\n"
        );

        codigo.append(
                "        if (H < HEAP_SIZE) Heap[H++] = (unsigned char) buffer[i];\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_concat_string_string(double izquierda, double derecha) {\n"
        );

        codigo.append(
                "    double inicio = H;\n"
        );

        codigo.append(
                "    runtime_append_string(izquierda);\n"
        );

        codigo.append(
                "    runtime_append_string(derecha);\n"
        );

        codigo.append(
                "    if (H < HEAP_SIZE) Heap[H++] = -1;\n"
        );

        codigo.append(
                "    return inicio;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_concat_string_number(double izquierda, double derecha) {\n"
        );

        codigo.append(
                "    double inicio = H;\n"
        );

        codigo.append(
                "    runtime_append_string(izquierda);\n"
        );

        codigo.append(
                "    runtime_append_number(derecha);\n"
        );

        codigo.append(
                "    if (H < HEAP_SIZE) Heap[H++] = -1;\n"
        );

        codigo.append(
                "    return inicio;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_concat_number_string(double izquierda, double derecha) {\n"
        );

        codigo.append(
                "    double inicio = H;\n"
        );

        codigo.append(
                "    runtime_append_number(izquierda);\n"
        );

        codigo.append(
                "    runtime_append_string(derecha);\n"
        );

        codigo.append(
                "    if (H < HEAP_SIZE) Heap[H++] = -1;\n"
        );

        codigo.append(
                "    return inicio;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static int runtime_read_line(char *buffer, int capacidad) {\n"
        );

        codigo.append(
                "    if (buffer == NULL || capacidad <= 0) return 0;\n"
        );

        codigo.append(
                "    fflush(stdout);\n"
        );

        codigo.append(
                "    if (fgets(buffer, capacidad, stdin) == NULL) return 0;\n"
        );

        codigo.append(
                "    int longitud = (int) strlen(buffer);\n"
        );

        codigo.append(
                "    while (longitud > 0 && (buffer[longitud - 1] == '\\n' || buffer[longitud - 1] == '\\r')) {\n"
        );

        codigo.append(
                "        buffer[--longitud] = '\\0';\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "    return 1;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_read_number(void) {\n"
        );

        codigo.append(
                "    char buffer[4096];\n"
        );

        codigo.append(
                "    if (!runtime_read_line(buffer, sizeof(buffer))) return 0;\n"
        );

        codigo.append(
                "    char *fin = NULL;\n"
        );

        codigo.append(
                "    double valor = strtod(buffer, &fin);\n"
        );

        codigo.append(
                "    if (fin == buffer) return 0;\n"
        );

        codigo.append(
                "    return valor;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_read_string(void) {\n"
        );

        codigo.append(
                "    char buffer[4096];\n"
        );

        codigo.append(
                "    if (!runtime_read_line(buffer, sizeof(buffer))) return -1;\n"
        );

        codigo.append(
                "    double inicio = H;\n"
        );

        codigo.append(
                "    for (int i = 0; buffer[i] != '\\0'; i++) {\n"
        );

        codigo.append(
                "        if (H >= HEAP_SIZE - 1) break;\n"
        );

        codigo.append(
                "        Heap[H++] = (unsigned char) buffer[i];\n"
        );

        codigo.append(
                "    }\n"
        );

        codigo.append(
                "    if (H < HEAP_SIZE) Heap[H++] = -1;\n"
        );

        codigo.append(
                "    return inicio;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_read_char(void) {\n"
        );

        codigo.append(
                "    char buffer[4096];\n"
        );

        codigo.append(
                "    if (!runtime_read_line(buffer, sizeof(buffer))) return 0;\n"
        );

        codigo.append(
                "    if (buffer[0] == '\\0') return 0;\n"
        );

        codigo.append(
                "    return (unsigned char) buffer[0];\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static double runtime_read_boolean(void) {\n"
        );

        codigo.append(
                "    char buffer[4096];\n"
        );

        codigo.append(
                "    if (!runtime_read_line(buffer, sizeof(buffer))) return 0;\n"
        );

        codigo.append(
                "    if (strcmp(buffer, \"1\") == 0 || strcmp(buffer, \"true\") == 0 || strcmp(buffer, \"verum\") == 0) return 1;\n"
        );

        codigo.append(
                "    if (strcmp(buffer, \"0\") == 0 || strcmp(buffer, \"false\") == 0 || strcmp(buffer, \"falsus\") == 0) return 0;\n"
        );

        codigo.append(
                "    return strtod(buffer, NULL) != 0 ? 1 : 0;\n"
        );

        codigo.append(
                "}\n\n"
        );

        codigo.append(
                "static void runtime_discard_input(void) {\n"
        );

        codigo.append(
                "    char buffer[4096];\n"
        );

        codigo.append(
                "    runtime_read_line(buffer, sizeof(buffer));\n"
        );

        codigo.append(
                "}\n\n"
        );
    }

    private void prototipos(
            StringBuilder codigo,
            Map<String, List<Cuarteta>> funciones,
            Map<String, String> nombres
    ) {
        for (String funcion
                : funciones.keySet()) {

            codigo.append(
                    "static void "
            );

            codigo.append(
                    nombres.get(
                            funcion
                    )
            );

            codigo.append(
                    "(void);\n"
            );
        }

        codigo.append(
                "\n"
        );
    }

    private void generarFuncion(
            StringBuilder codigo,
            String nombre,
            List<Cuarteta> cuartetas,
            Map<String, String> nombres
    ) {
        codigo.append(
                "static void "
        );

        codigo.append(
                nombres.get(
                        nombre
                )
        );

        codigo.append(
                "(void) {\n"
        );

        Set<String> temporales =
                recolectarTemporales(
                        cuartetas
                );

        if (!temporales.isEmpty()) {

            codigo.append(
                    "    double "
            );

            codigo.append(
                    String.join(
                            ", ",
                            temporales
                    )
            );

            codigo.append(
                    ";\n"
            );
        }

        for (int indice = 0;
             indice < cuartetas.size();
             indice++) {

            Cuarteta cuarteta =
                    cuartetas.get(
                            indice
                    );

            generarCuarteta(
                    codigo,
                    cuarteta,
                    nombres,
                    nombre,
                    indice + 1
            );
        }

        codigo.append(
                "}\n\n"
        );
    }

    private Set<String> recolectarTemporales(
            List<Cuarteta> cuartetas
    ) {
        Set<String> resultado =
                new LinkedHashSet<>();

        for (Cuarteta cuarteta
                : cuartetas) {

            buscarTemporales(
                    cuarteta.argumento1(),
                    resultado
            );

            buscarTemporales(
                    cuarteta.argumento2(),
                    resultado
            );

            buscarTemporales(
                    cuarteta.resultado(),
                    resultado
            );
        }

        return resultado;
    }

    private void buscarTemporales(
            String texto,
            Set<String> resultado
    ) {
        if (texto == null) {
            return;
        }

        Matcher matcher =
                TEMPORAL.matcher(
                        texto
                );

        while (matcher.find()) {

            resultado.add(
                    matcher.group()
            );
        }
    }

    private void generarCuarteta(
            StringBuilder codigo,
            Cuarteta cuarteta,
            Map<String, String> nombres,
            String funcionActual,
            int indiceCuarteta
    ) {
        switch (cuarteta.operador()) {

            case DECLARAR,
                 DECLARAR_PARAMETRO -> {
            }

            case ASIGNAR ->
                    asignar(
                            codigo,
                            cuarteta
                    );

            case SUMAR ->
                    binaria(
                            codigo,
                            cuarteta,
                            "+"
                    );

            case RESTAR ->
                    binaria(
                            codigo,
                            cuarteta,
                            "-"
                    );

            case MULTIPLICAR ->
                    binaria(
                            codigo,
                            cuarteta,
                            "*"
                    );

            case DIVIDIR ->
                    binaria(
                            codigo,
                            cuarteta,
                            "/"
                    );

            case MODULO ->
                    modulo(
                            codigo,
                            cuarteta
                    );

            case NEGATIVO ->
                    unaria(
                            codigo,
                            cuarteta,
                            "-"
                    );

            case NEGAR ->
                    unaria(
                            codigo,
                            cuarteta,
                            "!"
                    );

            case IGUALDAD ->
                    binaria(
                            codigo,
                            cuarteta,
                            "=="
                    );

            case DIFERENTE ->
                    binaria(
                            codigo,
                            cuarteta,
                            "!="
                    );

            case MENOR ->
                    binaria(
                            codigo,
                            cuarteta,
                            "<"
                    );

            case MAYOR ->
                    binaria(
                            codigo,
                            cuarteta,
                            ">"
                    );

            case MENOR_IGUAL ->
                    binaria(
                            codigo,
                            cuarteta,
                            "<="
                    );

            case MAYOR_IGUAL ->
                    binaria(
                            codigo,
                            cuarteta,
                            ">="
                    );

            case AND ->
                    binaria(
                            codigo,
                            cuarteta,
                            "&&"
                    );

            case OR ->
                    binaria(
                            codigo,
                            cuarteta,
                            "||"
                    );

            case COMPARAR ->
                    comparar(
                            codigo,
                            cuarteta
                    );

            case ETIQUETA ->
                    etiqueta(
                            codigo,
                            cuarteta.resultado()
                    );

            case SALTAR ->
                    salto(
                            codigo,
                            cuarteta.resultado()
                    );

            case SALTAR_SI_FALSO ->
                    saltoCondicional(
                            codigo,
                            cuarteta,
                            false
                    );

            case SALTAR_SI_VERDADERO ->
                    saltoCondicional(
                            codigo,
                            cuarteta,
                            true
                    );

            case LLAMAR ->
                    llamada(
                            codigo,
                            cuarteta,
                            nombres
                    );

            case RETORNAR ->
                    linea(
                            codigo,
                            "return;"
                    );

            case LEER ->
                    lectura(
                            codigo,
                            cuarteta
                    );

            case LEER_STACK ->
                    leerStack(
                            codigo,
                            cuarteta
                    );

            case ESCRIBIR_STACK ->
                    escribirStack(
                            codigo,
                            cuarteta
                    );

            case LEER_TIPO_OBJETO ->
                    linea(codigo, cuarteta.resultado() + " = runtime_tipo_objeto_z("
                            + expresion(cuarteta.argumento1()) + ");");

            case ESCRIBIR_TIPO_OBJETO ->
                    linea(codigo, "TipoObjetoZ[(int) (" + expresion(cuarteta.argumento1())
                            + ")] = (int) (" + expresion(cuarteta.argumento2()) + ");");

            case LEER_HEAP ->
                    leerHeap(
                            codigo,
                            cuarteta
                    );

            case ESCRIBIR_HEAP ->
                    escribirHeap(
                            codigo,
                            cuarteta
                    );

            case IMPRIMIR ->
                    imprimir(
                            codigo,
                            cuarteta
                    );

            case PARAMETRO,
                 NUEVO_OBJETO,
                 NUEVO_ARREGLO,
                 INICIALIZAR_COMPUESTO ->
                    throw new IllegalStateException(
                            "La operacion "
                                    + cuarteta.operador()
                                    + " llego sin bajar al generador C"
                                    + " | funcion="
                                    + funcionActual
                                    + " | cuarteta="
                                    + indiceCuarteta
                                    + " | arg1="
                                    + textoDiagnostico(
                                    cuarteta.argumento1()
                            )
                                    + " | arg2="
                                    + textoDiagnostico(
                                    cuarteta.argumento2()
                            )
                                    + " | resultado="
                                    + textoDiagnostico(
                                    cuarteta.resultado()
                            )
                    );

            case INICIO_FUNCION,
                 FIN_FUNCION -> {
            }
        }
    }

    private void asignar(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        String destino =
                cuarteta.resultado();

        String valor =
                expresion(
                        cuarteta.argumento1()
                );

        if ("P".equals(
                destino
        )
                || "H".equals(
                destino
        )) {

            linea(
                    codigo,
                    destino
                            + " = (int) ("
                            + valor
                            + ");"
            );

            return;
        }

        linea(
                codigo,
                destino
                        + " = "
                        + valor
                        + ";"
        );
    }

    private void binaria(
            StringBuilder codigo,
            Cuarteta cuarteta,
            String operador
    ) {
        boolean izquierdaCadena =
                esMarcadoCadena(
                        cuarteta.argumento1()
                );

        boolean derechaCadena =
                esMarcadoCadena(
                        cuarteta.argumento2()
                );

        if ("+".equals(
                operador
        )
                && (
                izquierdaCadena
                        || derechaCadena
        )) {

            String izquierda =
                    expresion(
                            quitarMarcaCadena(
                                    cuarteta.argumento1()
                            )
                    );

            String derecha =
                    expresion(
                            quitarMarcaCadena(
                                    cuarteta.argumento2()
                            )
                    );

            String funcion;

            if (izquierdaCadena
                    && derechaCadena) {

                funcion =
                        "runtime_concat_string_string";

            } else if (izquierdaCadena) {

                funcion =
                        "runtime_concat_string_number";

            } else {

                funcion =
                        "runtime_concat_number_string";
            }

            linea(
                    codigo,
                    cuarteta.resultado()
                            + " = "
                            + funcion
                            + "("
                            + izquierda
                            + ", "
                            + derecha
                            + ");"
            );

            return;
        }

        linea(
                codigo,
                cuarteta.resultado()
                        + " = "
                        + expresion(
                        cuarteta.argumento1()
                )
                        + " "
                        + operador
                        + " "
                        + expresion(
                        cuarteta.argumento2()
                )
                        + ";"
        );
    }

    private void modulo(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        linea(
                codigo,
                cuarteta.resultado()
                        + " = fmod("
                        + expresion(
                        cuarteta.argumento1()
                )
                        + ", "
                        + expresion(
                        cuarteta.argumento2()
                )
                        + ");"
        );
    }

    private void unaria(
            StringBuilder codigo,
            Cuarteta cuarteta,
            String operador
    ) {
        linea(
                codigo,
                cuarteta.resultado()
                        + " = "
                        + operador
                        + "("
                        + expresion(
                        cuarteta.argumento1()
                )
                        + ");"
        );
    }

    private void comparar(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        String izquierda =
                expresion(
                        cuarteta.argumento1()
                );

        String derecha =
                expresion(
                        cuarteta.argumento2()
                );

        linea(
                codigo,
                cuarteta.resultado()
                        + " = (("
                        + izquierda
                        + " > "
                        + derecha
                        + ") - ("
                        + izquierda
                        + " < "
                        + derecha
                        + "));"
        );
    }

    private void etiqueta(
            StringBuilder codigo,
            String etiqueta
    ) {
        codigo.append(sanitizarEtiqueta(etiqueta));
        codigo.append(": ;\n");
    }

    private void salto(
            StringBuilder codigo,
            String etiqueta
    ) {
        linea(
                codigo,
                "goto "
                        + sanitizarEtiqueta(
                        etiqueta
                )
                        + ";"
        );
    }

    private void saltoCondicional(
            StringBuilder codigo,
            Cuarteta cuarteta,
            boolean verdadero
    ) {
        String condicion =
                expresion(
                        cuarteta.argumento1()
                );

        String etiqueta =
                sanitizarEtiqueta(
                        cuarteta.resultado()
                );

        if (verdadero) {

            linea(
                    codigo,
                    "if ("
                            + condicion
                            + ") goto "
                            + etiqueta
                            + ";"
            );

        } else {

            linea(
                    codigo,
                    "if (!("
                            + condicion
                            + ")) goto "
                            + etiqueta
                            + ";"
            );
        }
    }

    private void llamada(
            StringBuilder codigo,
            Cuarteta cuarteta,
            Map<String, String> nombres
    ) {
        String nombre =
                nombres.get(
                        cuarteta.argumento1()
                );

        if (nombre == null) {

            throw new IllegalStateException(
                    "No existe la funcion C para "
                            + cuarteta.argumento1()
            );
        }

        linea(
                codigo,
                nombre
                        + "();"
        );
    }

    private void lectura(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        String tipo =
                cuarteta.argumento1() == null
                        ? ""
                        : cuarteta.argumento1()
                        .trim();

        if (tipo.equalsIgnoreCase(
                "descartar"
        )) {

            linea(
                    codigo,
                    "runtime_discard_input();"
            );

            return;
        }

        if (vacio(
                cuarteta.resultado()
        )) {
            return;
        }

        String funcion;

        if (esTipoCadena(
                tipo
        )) {

            funcion =
                    "runtime_read_string()";

        } else if (tipo.equalsIgnoreCase(
                "char"
        )
                || tipo.equalsIgnoreCase(
                "caracter"
        )
                || tipo.equalsIgnoreCase(
                "littera"
        )) {

            funcion =
                    "runtime_read_char()";

        } else if (tipo.equalsIgnoreCase(
                "boolean"
        )
                || tipo.equalsIgnoreCase(
                "bool"
        )) {

            funcion =
                    "runtime_read_boolean()";

        } else {

            funcion =
                    "runtime_read_number()";
        }

        linea(
                codigo,
                cuarteta.resultado()
                        + " = "
                        + funcion
                        + ";"
        );
    }

    private boolean esTipoCadena(
            String tipo
    ) {
        if (tipo == null) {
            return false;
        }

        return tipo.equalsIgnoreCase(
                "String"
        )
                || tipo.equalsIgnoreCase(
                "cadena"
        )
                || tipo.equalsIgnoreCase(
                "textum"
        );
    }

    private void leerStack(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        linea(
                codigo,
                cuarteta.resultado()
                        + " = Stack[(int) ("
                        + expresion(
                        cuarteta.argumento1()
                )
                        + ")];"
        );
    }

    private void escribirStack(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        linea(
                codigo,
                "Stack[(int) ("
                        + expresion(
                        cuarteta.argumento1()
                )
                        + ")] = "
                        + expresion(
                        cuarteta.argumento2()
                )
                        + ";"
        );
    }

    private void leerHeap(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        linea(
                codigo,
                cuarteta.resultado()
                        + " = Heap[(int) ("
                        + expresion(
                        cuarteta.argumento1()
                )
                        + ")];"
        );
    }

    private void escribirHeap(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        linea(
                codigo,
                "Heap[(int) ("
                        + expresion(
                        cuarteta.argumento1()
                )
                        + ")] = "
                        + expresion(
                        cuarteta.argumento2()
                )
                        + ";"
        );
    }

    private void imprimir(
            StringBuilder codigo,
            Cuarteta cuarteta
    ) {
        boolean referenciaCadena =
                pareceReferenciaCadena(
                        cuarteta.argumento1()
                );

        String valor =
                expresion(
                        quitarMarcaCadena(
                                cuarteta.argumento1()
                        )
                );

        String modo =
                cuarteta.argumento2() == null
                        ? ""
                        : cuarteta.argumento2()
                        .trim()
                        .toLowerCase();

        boolean salto =
                modo.startsWith(
                        "println"
                );

        boolean caracter =
                modo.endsWith(
                        ":char"
                );

        boolean booleano =
                modo.endsWith(
                        ":boolean"
                );

        if (referenciaCadena) {

            linea(
                    codigo,
                    "runtime_print_string("
                            + valor
                            + ", "
                            + (
                            salto
                                    ? "1"
                                    : "0"
                    )
                            + ");"
            );

            return;
        }

        if (caracter) {
            linea(
                    codigo,
                    "putchar((unsigned char) ((int) ("
                            + valor
                            + ")));"
            );

        } else if (booleano) {
            linea(
                    codigo,
                    "printf(\"%s\", (("
                            + valor
                            + ") != 0) ? \"true\" : \"false\");"
            );

        } else {
            linea(
                    codigo,
                    "printf(\"%.15g\", (double) ("
                            + valor
                            + "));"
            );
        }

        if (salto) {

            linea(
                    codigo,
                    "putchar('\\n');"
            );
        }
    }

    private boolean pareceReferenciaCadena(
            String valor
    ) {
        if (valor == null) {
            return false;
        }

        return esMarcadoCadena(
                valor
        )
                || quitarMarcaCadena(
                valor
        ).matches(
                "str_t\\d+"
        );
    }

    private boolean esMarcadoCadena(
            String valor
    ) {
        return valor != null
                && valor.startsWith(
                MARCA_CADENA
        );
    }

    private String quitarMarcaCadena(
            String valor
    ) {
        if (!esMarcadoCadena(
                valor
        )) {
            return valor;
        }

        return valor.substring(
                MARCA_CADENA.length()
        );
    }

    private String expresion(
            String valor
    ) {
        valor =
                quitarMarcaCadena(
                        valor
                );

        if (valor == null
                || valor.isBlank()
                || valor.equals("-")) {

            return "0";
        }

        return valor
                .replace(
                        "verdadero",
                        "1"
                )
                .replace(
                        "falso",
                        "0"
                )
                .replace(
                        "verum",
                        "1"
                )
                .replace(
                        "falsus",
                        "0"
                )
                .replace(
                        "null",
                        "-1"
                );
    }

    private String sanitizarFuncion(
            String nombre
    ) {
        StringBuilder resultado =
                new StringBuilder(
                        "fn_"
                );

        for (int indice = 0;
             indice < nombre.length();
             indice++) {

            char caracter =
                    nombre.charAt(
                            indice
                    );

            if (Character.isLetterOrDigit(
                    caracter
            )
                    || caracter == '_') {

                resultado.append(
                        caracter
                );

            } else {

                resultado.append(
                        '_'
                );
            }
        }

        resultado.append(
                "_"
        );

        resultado.append(
                Integer.toUnsignedString(
                        nombre.hashCode(),
                        16
                )
        );

        return resultado.toString();
    }

    private String sanitizarEtiqueta(
            String etiqueta
    ) {
        if (etiqueta == null
                || etiqueta.isBlank()) {

            return "L_invalida";
        }

        StringBuilder resultado =
                new StringBuilder(
                        "lbl_"
                );

        for (int indice = 0;
             indice < etiqueta.length();
             indice++) {

            char caracter =
                    etiqueta.charAt(
                            indice
                    );

            if (Character.isLetterOrDigit(
                    caracter
            )
                    || caracter == '_') {

                resultado.append(
                        caracter
                );

            } else {

                resultado.append(
                        '_'
                );
            }
        }

        return resultado.toString();
    }

    private void generarMain(
            StringBuilder codigo,
            Map<String, String> nombres
    ) {
        String maior =
                nombres.get(
                        "MAIOR"
                );

        if (maior == null) {

            throw new IllegalStateException(
                    "No existe la funcion MAIOR"
            );
        }

        codigo.append(
                "int main(void) {\n"
        );

        codigo.append(
                "    P = 0;\n"
        );

        codigo.append(
                "    H = 0;\n"
        );

        codigo.append(
                "    "
        );

        codigo.append(
                maior
        );

        codigo.append(
                "();\n"
        );

        codigo.append(
                "    return 0;\n"
        );

        codigo.append(
                "}\n"
        );
    }

    private void linea(
            StringBuilder codigo,
            String linea
    ) {
        codigo.append(
                "    "
        );

        codigo.append(
                linea
        );

        codigo.append(
                "\n"
        );
    }

    private String textoDiagnostico(
            String valor
    ) {
        return valor == null
                ? "<null>"
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