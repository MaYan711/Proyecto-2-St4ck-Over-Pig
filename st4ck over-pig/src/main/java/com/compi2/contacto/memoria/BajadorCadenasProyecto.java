package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BajadorCadenasProyecto {

    private static final String MARCA_CADENA =
            "@STR@";

    private ProgramaIntermedio destino;
    private int siguienteCadena;
    private int siguienteHeap;
    private final Set<String> referenciasCadena;
    private final Set<String> posicionesStackCadena;
    private final Set<String> posicionesHeapCadena;
    private final Map<String, String> retornosFunciones;
    private boolean retornoCadenaPendiente;

    private final RastreadorCaracteres caracteres =
            new RastreadorCaracteres();

    public BajadorCadenasProyecto() {
        referenciasCadena =
                new LinkedHashSet<>();

        posicionesStackCadena =
                new LinkedHashSet<>();

        posicionesHeapCadena =
                new LinkedHashSet<>();

        retornosFunciones =
                new LinkedHashMap<>();
    }

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        destino =
                new ProgramaIntermedio();

        siguienteCadena = 0;
        siguienteHeap = 0;
        retornoCadenaPendiente = false;

        referenciasCadena.clear();
        posicionesStackCadena.clear();
        posicionesHeapCadena.clear();
        retornosFunciones.clear();
        caracteres.iniciar(origen);

        registrarRetornos(
                origen
        );

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            procesar(
                    cuarteta
            );
        }

        return destino;
    }

    private void registrarRetornos(
            ProgramaIntermedio programa
    ) {
        for (Cuarteta cuarteta
                : programa.cuartetas()) {

            if (cuarteta.operador()
                    != OperadorCuarteta.INICIO_FUNCION) {

                continue;
            }

            retornosFunciones.put(
                    cuarteta.argumento1(),
                    cuarteta.argumento2()
            );
        }
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        caracteres.registrar(cuarteta);
        switch (cuarteta.operador()) {

            case INICIO_FUNCION -> {
                posicionesStackCadena.clear();
                posicionesHeapCadena.clear();
                retornoCadenaPendiente = false;
                copiar(
                        cuarteta
                );
            }

            case FIN_FUNCION -> {
                posicionesStackCadena.clear();
                posicionesHeapCadena.clear();
                retornoCadenaPendiente = false;
                copiar(
                        cuarteta
                );
            }

            case DECLARAR,
                 DECLARAR_PARAMETRO -> {
                if (esTipoCadena(
                        cuarteta.argumento1()
                )) {

                    registrarReferenciaCadena(
                            cuarteta.resultado()
                    );
                }

                copiar(
                        cuarteta
                );
            }

            case ASIGNAR ->
                    bajarAsignacion(
                            cuarteta
                    );

            case SUMAR ->
                    bajarSuma(
                            cuarteta
                    );

            case PARAMETRO ->
                    agregar(
                            cuarteta.operador(),
                            materializar(
                                    cuarteta.argumento1()
                            ),
                            cuarteta.argumento2(),
                            cuarteta.resultado()
                    );

            case RETORNAR ->
                    agregar(
                            cuarteta.operador(),
                            materializar(
                                    cuarteta.argumento1()
                            ),
                            cuarteta.argumento2(),
                            cuarteta.resultado()
                    );

            case ESCRIBIR_STACK ->
                    bajarEscrituraStack(
                            cuarteta
                    );

            case LEER_STACK ->
                    bajarLecturaStack(
                            cuarteta
                    );

            case ESCRIBIR_HEAP ->
                    bajarEscrituraHeap(
                            cuarteta
                    );

            case LEER_HEAP ->
                    bajarLecturaHeap(
                            cuarteta
                    );

            case LLAMAR ->
                    bajarLlamada(
                            cuarteta
                    );

            case LEER ->
                    bajarLectura(
                            cuarteta
                    );

            case IMPRIMIR ->
                    bajarImpresion(
                            cuarteta
                    );

            default ->
                    copiar(
                            cuarteta
                    );
        }
    }

    private String materializarCaracter(
            String valor
    ) {
        String referencia = "str_t" + siguienteCadena++;
        String nuevoHeap = "str_h" + siguienteHeap++;

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                "2",
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia + "+0",
                valor,
                null
        );

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia + "+1",
                "-1",
                null
        );

        registrarReferenciaCadena(referencia);

        return referencia;
    }

    private void bajarAsignacion(
            Cuarteta cuarteta
    ) {
        String valor =
                materializar(
                        cuarteta.argumento1()
                );

        if (esReferenciaCadena(
                valor
        )) {

            registrarReferenciaCadena(
                    cuarteta.resultado()
            );
        }

        agregar(
                cuarteta.operador(),
                valor,
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void bajarSuma(
            Cuarteta cuarteta
    ) {
        String izquierda = materializar(cuarteta.argumento1());
        String derecha = materializar(cuarteta.argumento2());

        boolean izquierdaCadena = esReferenciaCadena(izquierda);
        boolean derechaCadena = esReferenciaCadena(derecha);

        if (!izquierdaCadena && !derechaCadena) {
            agregar(
                    cuarteta.operador(),
                    izquierda,
                    derecha,
                    cuarteta.resultado()
            );

            return;
        }

        if (!izquierdaCadena && caracteres.esCaracter(izquierda)) {
            izquierda = materializarCaracter(izquierda);
            izquierdaCadena = true;
        }

        if (!derechaCadena && caracteres.esCaracter(derecha)) {
            derecha = materializarCaracter(derecha);
            derechaCadena = true;
        }

        registrarReferenciaCadena(cuarteta.resultado());

        agregar(
                OperadorCuarteta.SUMAR,
                izquierdaCadena ? marcarCadena(izquierda) : izquierda,
                derechaCadena ? marcarCadena(derecha) : derecha,
                cuarteta.resultado()
        );
    }

    private void bajarEscrituraStack(
            Cuarteta cuarteta
    ) {
        String valor =
                materializar(
                        cuarteta.argumento2()
                );

        if (esReferenciaCadena(
                valor
        )) {

            posicionesStackCadena.add(
                    normalizarDireccion(
                            cuarteta.argumento1()
                    )
            );
        }

        agregar(
                cuarteta.operador(),
                cuarteta.argumento1(),
                valor,
                cuarteta.resultado()
        );
    }

    private void bajarLecturaStack(
            Cuarteta cuarteta
    ) {
        String direccion =
                normalizarDireccion(
                        cuarteta.argumento1()
                );

        if (esTipoCadena(
                cuarteta.argumento2()
        )
                || posicionesStackCadena.contains(
                direccion
        )
                || (
                retornoCadenaPendiente
                        && esDireccionRetorno(
                        direccion
                )
        )) {

            registrarReferenciaCadena(
                    cuarteta.resultado()
            );
        }

        if (retornoCadenaPendiente
                && esDireccionRetorno(
                direccion
        )) {

            retornoCadenaPendiente = false;
        }

        copiar(
                cuarteta
        );
    }

    private void bajarEscrituraHeap(
            Cuarteta cuarteta
    ) {
        String valor =
                materializar(
                        cuarteta.argumento2()
                );

        if (esReferenciaCadena(
                valor
        )) {

            posicionesHeapCadena.add(
                    normalizarDireccion(
                            cuarteta.argumento1()
                    )
            );
        }

        agregar(
                cuarteta.operador(),
                cuarteta.argumento1(),
                valor,
                cuarteta.resultado()
        );
    }

    private void bajarLecturaHeap(
            Cuarteta cuarteta
    ) {
        if (esTipoCadena(
                cuarteta.argumento2()
        )
                || posicionesHeapCadena.contains(
                normalizarDireccion(
                        cuarteta.argumento1()
                )
        )) {

            registrarReferenciaCadena(
                    cuarteta.resultado()
            );
        }

        copiar(
                cuarteta
        );
    }

    private void bajarLectura(
            Cuarteta cuarteta
    ) {
        if (esTipoCadena(
                cuarteta.argumento1()
        )) {

            registrarReferenciaCadena(
                    cuarteta.resultado()
            );
        }

        copiar(
                cuarteta
        );
    }

    private void bajarLlamada(
            Cuarteta cuarteta
    ) {
        copiar(
                cuarteta
        );

        String tipoRetorno =
                retornosFunciones.get(
                        cuarteta.argumento1()
                );

        retornoCadenaPendiente =
                esTipoCadena(
                        tipoRetorno
                );
    }

    private void bajarImpresion(
            Cuarteta cuarteta
    ) {
        String valor = materializar(cuarteta.argumento1());

        if (!esReferenciaCadena(valor)
                && caracteres.esCaracter(valor)) {
            valor = materializarCaracter(valor);
        }

        if (esReferenciaCadena(valor)) {
            valor = marcarCadena(valor);
        }

        agregar(
                cuarteta.operador(),
                valor,
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private String materializar(
            String valor
    ) {
        if (!esLiteralCadena(
                valor
        )) {
            return valor;
        }

        String contenido =
                decodificar(
                        valor
                );

        byte[] bytes =
                contenido.getBytes(
                        StandardCharsets.UTF_8
                );

        String referencia =
                "str_t"
                        + siguienteCadena++;

        registrarReferenciaCadena(
                referencia
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        String nuevoHeap =
                "str_h"
                        + siguienteHeap++;

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        bytes.length + 1
                ),
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        for (int indice = 0;
             indice < bytes.length;
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + indice,
                    String.valueOf(
                            Byte.toUnsignedInt(
                                    bytes[indice]
                            )
                    ),
                    null
            );
        }

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia
                        + "+"
                        + bytes.length,
                "-1",
                null
        );

        return referencia;
    }

    private void registrarReferenciaCadena(
            String valor
    ) {
        if (valor == null
                || valor.isBlank()
                || valor.equals("-")) {

            return;
        }

        referenciasCadena.add(
                valor
        );
    }

    private boolean esReferenciaCadena(
            String valor
    ) {
        if (valor == null
                || valor.isBlank()
                || valor.equals("-")) {

            return false;
        }

        return esMarcadoCadena(
                valor
        )
                || esLiteralCadena(
                valor
        )
                || referenciasCadena.contains(
                valor
        );
    }

    private boolean esTipoCadena(
            String tipo
    ) {
        if (tipo == null) {
            return false;
        }

        String normalizado =
                tipo.trim();

        return normalizado.equalsIgnoreCase(
                "String"
        )
                || normalizado.equalsIgnoreCase(
                "cadena"
        )
                || normalizado.equalsIgnoreCase(
                "textum"
        );
    }

    private boolean esDireccionRetorno(
            String direccion
    ) {
        return "P+0".equals(
                direccion
        )
                || "P".equals(
                direccion
        );
    }

    private String normalizarDireccion(
            String direccion
    ) {
        if (direccion == null) {
            return "";
        }

        return direccion
                .replace(
                        " ",
                        ""
                );
    }

    private String marcarCadena(
            String valor
    ) {
        if (esMarcadoCadena(
                valor
        )) {
            return valor;
        }

        return MARCA_CADENA
                + valor;
    }

    private boolean esMarcadoCadena(
            String valor
    ) {
        return valor != null
                && valor.startsWith(
                MARCA_CADENA
        );
    }

    private boolean esLiteralCadena(
            String valor
    ) {
        if (valor == null) {
            return false;
        }

        String texto =
                valor.trim();

        return texto.length() >= 2
                && texto.startsWith("\"")
                && texto.endsWith("\"");
    }

    private String decodificar(
            String literal
    ) {
        String contenido =
                literal.substring(
                        1,
                        literal.length() - 1
                );

        StringBuilder resultado =
                new StringBuilder();

        for (int indice = 0;
             indice < contenido.length();
             indice++) {

            char actual =
                    contenido.charAt(
                            indice
                    );

            if (actual != '\\') {
                resultado.append(
                        actual
                );
                continue;
            }

            if (indice + 1
                    >= contenido.length()) {

                resultado.append(
                        '\\'
                );
                continue;
            }

            char siguiente =
                    contenido.charAt(
                            ++indice
                    );

            if (siguiente == 'u'
                    && indice + 4
                    < contenido.length()) {

                String hexadecimal =
                        contenido.substring(
                                indice + 1,
                                indice + 5
                        );

                try {
                    resultado.append(
                            (char) Integer.parseInt(
                                    hexadecimal,
                                    16
                            )
                    );

                    indice += 4;
                    continue;

                } catch (NumberFormatException ignorada) {
                    resultado.append(
                            'u'
                    );
                    continue;
                }
            }

            switch (siguiente) {

                case 'n' ->
                        resultado.append(
                                '\n'
                        );

                case 'r' ->
                        resultado.append(
                                '\r'
                        );

                case 't' ->
                        resultado.append(
                                '\t'
                        );

                case 'b' ->
                        resultado.append(
                                '\b'
                        );

                case 'f' ->
                        resultado.append(
                                '\f'
                        );

                case '0' ->
                        resultado.append(
                                '\0'
                        );

                case '"' ->
                        resultado.append(
                                '"'
                        );

                case '\'' ->
                        resultado.append(
                                '\''
                        );

                case '\\' ->
                        resultado.append(
                                '\\'
                        );

                default ->
                        resultado.append(
                                siguiente
                        );
            }
        }

        return resultado.toString();
    }

    private void copiar(
            Cuarteta cuarteta
    ) {
        agregar(
                cuarteta.operador(),
                cuarteta.argumento1(),
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void agregar(
            OperadorCuarteta operador,
            String argumento1,
            String argumento2,
            String resultado
    ) {
        destino.agregar(
                new Cuarteta(
                        operador,
                        argumento1,
                        argumento2,
                        resultado
                )
        );
    }
}
