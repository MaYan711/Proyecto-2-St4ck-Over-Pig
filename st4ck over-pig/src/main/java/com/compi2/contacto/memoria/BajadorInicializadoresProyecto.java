package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BajadorInicializadoresProyecto {

    private ProgramaIntermedio destino;
    private PlanMemoriaProyecto plan;
    private MarcoStackProyecto marcoActual;
    private String funcionActual;
    private String claseZActual;
    private int siguienteTemporalHeap;

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen
    ) {
        return bajar(
                origen,
                null
        );
    }

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen,
            PlanMemoriaProyecto plan
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        destino =
                new ProgramaIntermedio();

        this.plan = plan;
        marcoActual = null;
        funcionActual = null;
        claseZActual = null;
        siguienteTemporalHeap = 0;

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            procesar(
                    cuarteta
            );
        }

        return destino;
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        switch (cuarteta.operador()) {

            case INICIO_FUNCION ->
                    iniciarFuncion(
                            cuarteta
                    );

            case FIN_FUNCION ->
                    finalizarFuncion(
                            cuarteta
                    );

            case INICIALIZAR_COMPUESTO ->
                    bajarInicializador(
                            cuarteta
                    );

            default ->
                    copiar(
                            cuarteta
                    );
        }
    }

    private void iniciarFuncion(
            Cuarteta cuarteta
    ) {
        funcionActual =
                cuarteta.argumento1();

        marcoActual =
                plan == null
                        ? null
                        : plan.marco(
                        funcionActual
                ).orElse(null);

        claseZActual =
                extraerClaseZ(
                        funcionActual
                );

        copiar(
                cuarteta
        );
    }

    private void finalizarFuncion(
            Cuarteta cuarteta
    ) {
        copiar(
                cuarteta
        );

        marcoActual = null;
        funcionActual = null;
        claseZActual = null;
    }

    private void bajarInicializador(
            Cuarteta cuarteta
    ) {
        if (esVacio(
                cuarteta.argumento1()
        )
                || esVacio(
                cuarteta.resultado()
        )) {

            copiar(
                    cuarteta
            );

            return;
        }

        Optional<Lista> raiz =
                new ParserLista(
                        cuarteta.argumento1()
                ).parsear();

        if (raiz.isEmpty()) {

            copiar(
                    cuarteta
            );

            return;
        }

        String tipo =
                normalizarTipo(
                        cuarteta.argumento2()
                );

        if (!esVacio(tipo)
                && !esTipoArreglo(tipo)) {

            Optional<LayoutHeapProyecto> layout =
                    buscarLayoutEstructuraY(
                            tipo
                    );

            if (layout.isPresent()) {

                if (!estructuraCompatible(
                        layout.orElseThrow(),
                        raiz.orElseThrow()
                )) {

                    copiar(
                            cuarteta
                    );

                    return;
                }

                emitirEstructura(
                        layout.orElseThrow(),
                        raiz.orElseThrow(),
                        cuarteta.resultado()
                );

                return;
            }
        }

        bajarArreglo(
                cuarteta,
                raiz.orElseThrow(),
                tipo
        );
    }

    private void bajarArreglo(
            Cuarteta cuarteta,
            Lista raiz,
            String tipo
    ) {
        Optional<List<Integer>> dimensiones =
                calcularDimensiones(
                        raiz
                );

        if (dimensiones.isEmpty()) {

            copiar(
                    cuarteta
            );

            return;
        }

        if (!esVacio(tipo)
                && esTipoArreglo(tipo)) {

            int cantidadDimensionesTipo =
                    cantidadDimensionesTipo(
                            tipo
                    );

            if (cantidadDimensionesTipo
                    != dimensiones.orElseThrow()
                    .size()) {

                copiar(
                        cuarteta
                );

                return;
            }
        }

        emitirArreglo(
                raiz,
                cuarteta.resultado()
        );
    }

    private String emitirEstructura(
            LayoutHeapProyecto layout,
            Lista lista,
            String referenciaSolicitada
    ) {
        String referencia =
                esVacio(
                        referenciaSolicitada
                )
                        ? nuevoTemporal()
                        : referenciaSolicitada;

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        String siguienteHeap =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        Math.max(
                                1,
                                layout.tamano()
                        )
                ),
                siguienteHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                siguienteHeap,
                null,
                "H"
        );

        for (int indice = 0;
             indice < layout.campos().size();
             indice++) {

            LayoutHeapProyecto.Campo campo =
                    layout.campos()
                            .get(indice);

            Elemento elemento =
                    lista.elementos()
                            .get(indice);

            String valor =
                    materializarElementoCampo(
                            campo,
                            elemento
                    );

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + campo.desplazamiento(),
                    valor,
                    null
            );
        }

        return referencia;
    }

    private String materializarElementoCampo(
            LayoutHeapProyecto.Campo campo,
            Elemento elemento
    ) {
        if (elemento instanceof Valor valor) {
            return materializarValor(
                    valor.texto()
            );
        }

        Lista lista =
                (Lista) elemento;

        if (esTipoArreglo(
                campo.tipo()
        )) {

            return emitirArreglo(
                    lista,
                    null
            );
        }

        Optional<LayoutHeapProyecto> layoutAnidado =
                buscarLayoutEstructuraY(
                        campo.tipo()
                );

        if (layoutAnidado.isPresent()) {
            return emitirEstructura(
                    layoutAnidado.orElseThrow(),
                    lista,
                    null
            );
        }

        return "0";
    }

    private String emitirArreglo(
            Lista raiz,
            String referenciaSolicitada
    ) {
        List<Integer> dimensiones =
                calcularDimensiones(
                        raiz
                ).orElseThrow();

        List<String> valores =
                new ArrayList<>();

        aplanar(
                raiz,
                valores
        );

        String referencia =
                esVacio(
                        referenciaSolicitada
                )
                        ? nuevoTemporal()
                        : referenciaSolicitada;

        int inicioDatos =
                1
                        + dimensiones.size();

        int totalCeldas =
                inicioDatos
                        + valores.size();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        String siguienteHeap =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        totalCeldas
                ),
                siguienteHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                siguienteHeap,
                null,
                "H"
        );

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia + "+0",
                String.valueOf(
                        dimensiones.size()
                ),
                null
        );

        for (int indice = 0;
             indice < dimensiones.size();
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + (
                            indice + 1
                    ),
                    String.valueOf(
                            dimensiones.get(
                                    indice
                            )
                    ),
                    null
            );
        }

        for (int indice = 0;
             indice < valores.size();
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + (
                            inicioDatos + indice
                    ),
                    materializarValor(
                            valores.get(
                                    indice
                            )
                    ),
                    null
            );
        }

        return referencia;
    }

    private boolean estructuraCompatible(
            LayoutHeapProyecto layout,
            Lista lista
    ) {
        if (layout.campos().size()
                != lista.elementos().size()) {

            return false;
        }

        for (int indice = 0;
             indice < layout.campos().size();
             indice++) {

            LayoutHeapProyecto.Campo campo =
                    layout.campos()
                            .get(indice);

            Elemento elemento =
                    lista.elementos()
                            .get(indice);

            if (!(elemento instanceof Lista interna)) {
                continue;
            }

            if (esTipoArreglo(
                    campo.tipo()
            )) {

                Optional<List<Integer>> dimensiones =
                        calcularDimensiones(
                                interna
                        );

                if (dimensiones.isEmpty()
                        || dimensiones.orElseThrow()
                        .size()
                        != cantidadDimensionesTipo(
                        campo.tipo()
                )) {

                    return false;
                }

                String base =
                        tipoBase(
                                campo.tipo()
                        );

                if (buscarLayoutEstructuraY(
                        base
                ).isPresent()) {

                    return false;
                }

                continue;
            }

            Optional<LayoutHeapProyecto> layoutAnidado =
                    buscarLayoutEstructuraY(
                            campo.tipo()
                    );

            if (layoutAnidado.isEmpty()
                    || !estructuraCompatible(
                    layoutAnidado.orElseThrow(),
                    interna
            )) {

                return false;
            }
        }

        return true;
    }

    private Optional<LayoutHeapProyecto> buscarLayoutEstructuraY(
            String tipo
    ) {
        if (plan == null
                || esVacio(tipo)
                || esTipoArreglo(tipo)) {

            return Optional.empty();
        }

        String base =
                tipoBase(
                        tipo
                );

        if (funcionActual != null
                && !funcionActual.equals("MAIOR")
                && claseZActual == null) {

            Optional<LayoutHeapProyecto> local =
                    plan.layout(
                            "Y::"
                                    + funcionActual
                                    + "::"
                                    + base
                    );

            if (local.isPresent()
                    && local.orElseThrow()
                    .clase()
                    == LayoutHeapProyecto.Clase.ESTRUCTURA_Y) {

                return local;
            }
        }

        Optional<LayoutHeapProyecto> global =
                plan.layout(
                        "Y::"
                                + base
                );

        if (global.isPresent()
                && global.orElseThrow()
                .clase()
                == LayoutHeapProyecto.Clase.ESTRUCTURA_Y) {

            return global;
        }

        return Optional.empty();
    }

    private String materializarValor(
            String valor
    ) {
        if (esVacio(valor)
                || esLiteral(valor)
                || marcoActual == null) {

            return valor;
        }

        Optional<SlotStackProyecto> slot =
                marcoActual.buscar(
                        valor
                );

        if (slot.isPresent()) {
            return leerStack(
                    slot.orElseThrow()
            );
        }

        Optional<String> campoActual =
                leerCampoActualZ(
                        valor
                );

        return campoActual.orElse(
                valor
        );
    }

    private Optional<String> leerCampoActualZ(
            String nombre
    ) {
        if (plan == null
                || marcoActual == null
                || claseZActual == null
                || esVacio(nombre)
                || nombre.contains(".")
                || nombre.contains("[")
                || nombre.contains("(")) {

            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                plan.layout(
                        "Z::"
                                + claseZActual
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto.Campo> campo =
                layout.orElseThrow()
                        .buscarCampo(
                                nombre
                        );

        if (campo.isEmpty()) {
            return Optional.empty();
        }

        Optional<SlotStackProyecto> thisSlot =
                marcoActual.thisSlot();

        if (thisSlot.isEmpty()) {
            return Optional.empty();
        }

        String referenciaThis =
                leerStack(
                        thisSlot.orElseThrow()
                );

        String resultado =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_HEAP,
                referenciaThis
                        + "+"
                        + campo.orElseThrow()
                        .desplazamiento(),
                null,
                resultado
        );

        return Optional.of(
                resultado
        );
    }

    private String leerStack(
            SlotStackProyecto slot
    ) {
        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_STACK,
                slot.direccion(),
                null,
                temporal
        );

        return temporal;
    }

    private String nuevoTemporal() {
        return "init_h"
                + siguienteTemporalHeap++;
    }

    private Optional<List<Integer>> calcularDimensiones(
            Lista lista
    ) {
        List<Integer> resultado =
                new ArrayList<>();

        if (!calcularDimensiones(
                lista,
                resultado
        )) {

            return Optional.empty();
        }

        return Optional.of(
                resultado
        );
    }

    private boolean calcularDimensiones(
            Lista lista,
            List<Integer> resultado
    ) {
        resultado.add(
                lista.elementos()
                        .size()
        );

        if (lista.elementos()
                .isEmpty()) {

            return true;
        }

        Elemento primero =
                lista.elementos()
                        .get(0);

        if (primero instanceof Valor) {

            for (Elemento elemento
                    : lista.elementos()) {

                if (!(elemento
                        instanceof Valor)) {

                    return false;
                }
            }

            return true;
        }

        if (!(primero
                instanceof Lista primeraLista)) {

            return false;
        }

        List<Integer> dimensionesHijo =
                new ArrayList<>();

        if (!calcularDimensiones(
                primeraLista,
                dimensionesHijo
        )) {

            return false;
        }

        for (int indice = 1;
             indice < lista.elementos()
                     .size();
             indice++) {

            Elemento elemento =
                    lista.elementos()
                            .get(indice);

            if (!(elemento
                    instanceof Lista listaHija)) {

                return false;
            }

            List<Integer> dimensionesActuales =
                    new ArrayList<>();

            if (!calcularDimensiones(
                    listaHija,
                    dimensionesActuales
            )) {

                return false;
            }

            if (!dimensionesHijo.equals(
                    dimensionesActuales
            )) {

                return false;
            }
        }

        resultado.addAll(
                dimensionesHijo
        );

        return true;
    }

    private void aplanar(
            Lista lista,
            List<String> valores
    ) {
        for (Elemento elemento
                : lista.elementos()) {

            if (elemento
                    instanceof Valor valor) {

                valores.add(
                        valor.texto()
                );

            } else if (elemento
                    instanceof Lista interna) {

                aplanar(
                        interna,
                        valores
                );
            }
        }
    }

    private String normalizarTipo(
            String tipo
    ) {
        if (tipo == null) {
            return null;
        }

        String resultado =
                tipo.trim();

        return resultado.isEmpty()
                || resultado.equals("-")
                ? null
                : resultado;
    }

    private boolean esTipoArreglo(
            String tipo
    ) {
        return tipo != null
                && tipo.endsWith("[]");
    }

    private int cantidadDimensionesTipo(
            String tipo
    ) {
        int dimensiones = 0;
        String actual = tipo;

        while (actual != null
                && actual.endsWith("[]")) {

            dimensiones++;
            actual =
                    actual.substring(
                            0,
                            actual.length() - 2
                    );
        }

        return dimensiones;
    }

    private String tipoBase(
            String tipo
    ) {
        String resultado =
                tipo == null
                        ? ""
                        : tipo.trim();

        while (resultado.endsWith("[]")) {
            resultado =
                    resultado.substring(
                            0,
                            resultado.length() - 2
                    );
        }

        return resultado;
    }

    private boolean esLiteral(
            String valor
    ) {
        if (valor == null
                || valor.isBlank()) {

            return false;
        }

        String texto =
                valor.trim();

        if (texto.equals("true")
                || texto.equals("false")
                || texto.equals("verdadero")
                || texto.equals("falso")
                || texto.equals("verum")
                || texto.equals("falsus")
                || texto.equals("null")) {

            return true;
        }

        if ((texto.startsWith("\"")
                && texto.endsWith("\""))
                || (texto.startsWith("'")
                && texto.endsWith("'"))) {

            return true;
        }

        try {
            Double.parseDouble(
                    texto
            );
            return true;

        } catch (NumberFormatException excepcion) {
            return false;
        }
    }

    private String extraerClaseZ(
            String funcion
    ) {
        if (funcion == null
                || funcion.equals("MAIOR")
                || !funcion.contains(".")) {

            return null;
        }

        int punto =
                funcion.indexOf('.');

        if (punto <= 0) {
            return null;
        }

        String clase =
                funcion.substring(
                        0,
                        punto
                );

        if (plan == null
                || plan.layout(
                "Z::"
                        + clase
        ).isEmpty()) {

            return null;
        }

        return clase;
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
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

    private sealed interface Elemento
            permits Valor, Lista {
    }

    private record Valor(
            String texto
    ) implements Elemento {

        private Valor {
            Objects.requireNonNull(
                    texto
            );
        }
    }

    private record Lista(
            List<Elemento> elementos
    ) implements Elemento {

        private Lista {
            elementos =
                    List.copyOf(
                            elementos
                    );
        }
    }

    private static final class ParserLista {

        private final String texto;
        private int posicion;

        private ParserLista(
                String texto
        ) {
            this.texto =
                    Objects.requireNonNull(
                            texto
                    );

            posicion = 0;
        }

        private Optional<Lista> parsear() {
            omitirEspacios();

            Optional<Lista> resultado =
                    parsearLista();

            if (resultado.isEmpty()) {
                return Optional.empty();
            }

            omitirEspacios();

            if (posicion
                    != texto.length()) {

                return Optional.empty();
            }

            return resultado;
        }

        private Optional<Lista> parsearLista() {
            omitirEspacios();

            if (!consumir(
                    '{'
            )) {

                return Optional.empty();
            }

            List<Elemento> elementos =
                    new ArrayList<>();

            omitirEspacios();

            if (consumir(
                    '}'
            )) {

                return Optional.of(
                        new Lista(
                                elementos
                        )
                );
            }

            while (true) {
                omitirEspacios();

                if (actual()
                        == '{') {

                    Optional<Lista> interna =
                            parsearLista();

                    if (interna.isEmpty()) {
                        return Optional.empty();
                    }

                    elementos.add(
                            interna.orElseThrow()
                    );

                } else {

                    Optional<Valor> valor =
                            parsearValor();

                    if (valor.isEmpty()) {
                        return Optional.empty();
                    }

                    elementos.add(
                            valor.orElseThrow()
                    );
                }

                omitirEspacios();

                if (consumir(
                        '}'
                )) {

                    break;
                }

                if (!consumir(
                        ','
                )) {

                    return Optional.empty();
                }
            }

            return Optional.of(
                    new Lista(
                            elementos
                    )
            );
        }

        private Optional<Valor> parsearValor() {
            omitirEspacios();

            int inicio =
                    posicion;

            boolean cadena =
                    false;

            boolean caracter =
                    false;

            boolean escape =
                    false;

            int parentesis =
                    0;

            while (posicion
                    < texto.length()) {

                char actual =
                        texto.charAt(
                                posicion
                        );

                if (escape) {
                    escape = false;
                    posicion++;
                    continue;
                }

                if ((cadena
                        || caracter)
                        && actual == '\\') {

                    escape = true;
                    posicion++;
                    continue;
                }

                if (!caracter
                        && actual == '"') {

                    cadena =
                            !cadena;

                    posicion++;
                    continue;
                }

                if (!cadena
                        && actual == '\'') {

                    caracter =
                            !caracter;

                    posicion++;
                    continue;
                }

                if (!cadena
                        && !caracter) {

                    if (actual == '(') {
                        parentesis++;
                        posicion++;
                        continue;
                    }

                    if (actual == ')'
                            && parentesis > 0) {

                        parentesis--;
                        posicion++;
                        continue;
                    }

                    if (parentesis == 0
                            && (
                            actual == ','
                                    || actual == '}'
                    )) {

                        break;
                    }
                }

                posicion++;
            }

            if (cadena
                    || caracter
                    || parentesis != 0) {

                return Optional.empty();
            }

            String valor =
                    texto.substring(
                            inicio,
                            posicion
                    ).trim();

            if (valor.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(
                    new Valor(
                            valor
                    )
            );
        }

        private void omitirEspacios() {
            while (posicion
                    < texto.length()
                    && Character.isWhitespace(
                    texto.charAt(
                            posicion
                    )
            )) {

                posicion++;
            }
        }

        private boolean consumir(
                char esperado
        ) {
            if (posicion
                    >= texto.length()
                    || texto.charAt(
                    posicion
            ) != esperado) {

                return false;
            }

            posicion++;
            return true;
        }

        private char actual() {
            if (posicion
                    >= texto.length()) {

                return '\0';
            }

            return texto.charAt(
                    posicion
            );
        }
    }
}
