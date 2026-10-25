package com.compi2.contacto.ir;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.semantica.EnlacesPig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class GeneradorCuartetasPig {

    private ProgramaIntermedio programa;
    private final Deque<String> etiquetasRomper;
    private final Deque<String> etiquetasContinuar;
    private EnlacesPig enlaces;

    public GeneradorCuartetasPig() {
        etiquetasRomper = new ArrayDeque<>();
        etiquetasContinuar = new ArrayDeque<>();
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programas
    ) {
        return generar(
                programas,
                new EnlacesPig()
        );
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programas,
            EnlacesPig enlaces
    ) {
        Objects.requireNonNull(
                programas,
                "Los programas son obligatorios"
        );

        this.enlaces =
                Objects.requireNonNull(
                        enlaces,
                        "Los enlaces son obligatorios"
                );

        programa =
                new ProgramaIntermedio();

        etiquetasRomper.clear();
        etiquetasContinuar.clear();

        for (ProgramaAst ast : programas) {
            generarPrograma(
                    ast
            );
        }

        return programa;
    }

    private void generarPrograma(
            ProgramaAst ast
    ) {
        for (NodoAst nodo : ast.elementos()) {

            if (!(nodo instanceof PAst.Programa pig)) {
                continue;
            }

            generarPig(
                    pig
            );
        }
    }

    private void generarPig(
            PAst.Programa pig
    ) {
        agregar(
                OperadorCuarteta.INICIO_FUNCION,
                "MAIOR",
                "void",
                "0"
        );

        for (PAst.Declaracion global
                : pig.globales()) {

            generarDeclaracion(
                    global
            );
        }

        generarSentencias(
                pig.principal()
        );

        agregar(
                OperadorCuarteta.RETORNAR,
                null,
                null,
                null
        );

        agregar(
                OperadorCuarteta.FIN_FUNCION,
                "MAIOR",
                null,
                null
        );
    }

    private void generarSentencias(
            List<PAst.Sentencia> sentencias
    ) {
        for (PAst.Sentencia sentencia
                : sentencias) {

            generarSentencia(
                    sentencia
            );
        }
    }

    private void generarSentencia(
            PAst.Sentencia sentencia
    ) {
        if (sentencia
                instanceof PAst.Declaracion declaracion) {

            generarDeclaracion(
                    declaracion
            );

            return;
        }

        if (sentencia
                instanceof PAst.ExpresionSentencia expresion) {

            generarExpresion(
                    expresion.expresion()
            );

            return;
        }

        if (sentencia
                instanceof PAst.Entrada entrada) {

            generarEntrada(
                    entrada
            );

            return;
        }

        if (sentencia
                instanceof PAst.Salida salida) {

            generarSalida(
                    salida
            );

            return;
        }

        if (sentencia
                instanceof PAst.Si si) {

            generarSi(
                    si
            );

            return;
        }

        if (sentencia
                instanceof PAst.Mientras mientras) {

            generarMientras(
                    mientras
            );

            return;
        }

        if (sentencia
                instanceof PAst.HacerMientras hacer) {

            generarHacerMientras(
                    hacer
            );

            return;
        }

        if (sentencia
                instanceof PAst.Para para) {

            generarPara(
                    para
            );

            return;
        }

        if (sentencia
                instanceof PAst.Romper) {

            generarRomper();

            return;
        }

        if (sentencia
                instanceof PAst.Continuar) {

            generarContinuar();
        }
    }

    private void generarDeclaracion(
            PAst.Declaracion declaracion
    ) {
        String dimensiones =
                declaracion.dimensiones()
                        .isEmpty()
                        ? null
                        : declaracion.dimensiones()
                        .stream()
                        .map(
                                this::generarExpresion
                        )
                        .reduce(
                                (primero, segundo) ->
                                        primero
                                                + ","
                                                + segundo
                        )
                        .orElse(
                                null
                        );

        agregar(
                OperadorCuarteta.DECLARAR,
                declaracion.tipo()
                        .nombreCompleto(),
                dimensiones,
                declaracion.nombre()
        );

        if (declaracion.inicializador()
                .isEmpty()) {

            if (!declaracion.dimensiones()
                    .isEmpty()) {

                agregar(
                        OperadorCuarteta.NUEVO_ARREGLO,
                        declaracion.tipo()
                                .nombreBase(),
                        dimensiones,
                        declaracion.nombre()
                );
            }

            return;
        }

        PAst.Inicializador inicializador =
                declaracion.inicializador()
                        .orElseThrow();

        if (inicializador
                instanceof PAst.InicializadorLista lista) {

            agregar(
                    OperadorCuarteta.INICIALIZAR_COMPUESTO,
                    serializarLista(
                            lista
                    ),
                    declaracion.tipo()
                            .nombreCompleto(),
                    declaracion.nombre()
            );

            return;
        }

        String valor =
                generarExpresion(
                        (PAst.Expresion) inicializador
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                null,
                declaracion.nombre()
        );
    }

    private void generarEntrada(
            PAst.Entrada entrada
    ) {
        if (entrada.destino()
                .isEmpty()) {

            agregar(
                    OperadorCuarteta.LEER,
                    "descartar",
                    null,
                    null
            );

            return;
        }

        String tipo =
                enlaces.buscarTipoEntrada(
                        entrada
                ).orElse(
                        "numerus"
                );

        String temporal =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER,
                tipo,
                null,
                temporal
        );

        String destino =
                generarLugar(
                        entrada.destino()
                                .orElseThrow()
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporal,
                null,
                destino
        );
    }

    private void generarSalida(
            PAst.Salida salida
    ) {
        for (PAst.Expresion expresion
                : salida.expresiones()) {

            String valor =
                    generarExpresion(
                            expresion
                    );

            agregar(
                    OperadorCuarteta.IMPRIMIR,
                    valor,
                    null,
                    null
            );
        }
    }

    private void generarSi(
            PAst.Si si
    ) {
        String etiquetaFin =
                programa.nuevaEtiqueta();

        for (PAst.RamaSi rama
                : si.ramas()) {

            if (rama.condicion()
                    .isEmpty()) {

                generarSentencias(
                        rama.cuerpo()
                );

                continue;
            }

            String etiquetaSiguiente =
                    programa.nuevaEtiqueta();

            String condicion =
                    generarExpresion(
                            rama.condicion()
                                    .orElseThrow()
                    );

            agregar(
                    OperadorCuarteta.SALTAR_SI_FALSO,
                    condicion,
                    null,
                    etiquetaSiguiente
            );

            generarSentencias(
                    rama.cuerpo()
            );

            agregar(
                    OperadorCuarteta.SALTAR,
                    null,
                    null,
                    etiquetaFin
            );

            etiqueta(
                    etiquetaSiguiente
            );
        }

        etiqueta(
                etiquetaFin
        );
    }

    private void generarMientras(
            PAst.Mientras mientras
    ) {
        String etiquetaCondicion =
                programa.nuevaEtiqueta();

        String etiquetaFin =
                programa.nuevaEtiqueta();

        etiqueta(
                etiquetaCondicion
        );

        String condicion =
                generarExpresion(
                        mientras.condicion()
                );

        agregar(
                OperadorCuarteta.SALTAR_SI_FALSO,
                condicion,
                null,
                etiquetaFin
        );

        etiquetasRomper.push(
                etiquetaFin
        );

        etiquetasContinuar.push(
                etiquetaCondicion
        );

        generarSentencias(
                mientras.cuerpo()
        );

        etiquetasContinuar.pop();
        etiquetasRomper.pop();

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetaCondicion
        );

        etiqueta(
                etiquetaFin
        );
    }

    private void generarHacerMientras(
            PAst.HacerMientras hacer
    ) {
        String etiquetaInicio =
                programa.nuevaEtiqueta();

        String etiquetaCondicion =
                programa.nuevaEtiqueta();

        String etiquetaFin =
                programa.nuevaEtiqueta();

        etiqueta(
                etiquetaInicio
        );

        etiquetasRomper.push(
                etiquetaFin
        );

        etiquetasContinuar.push(
                etiquetaCondicion
        );

        generarSentencias(
                hacer.cuerpo()
        );

        etiquetasContinuar.pop();
        etiquetasRomper.pop();

        etiqueta(
                etiquetaCondicion
        );

        String condicion =
                generarExpresion(
                        hacer.condicion()
                );

        agregar(
                OperadorCuarteta.SALTAR_SI_VERDADERO,
                condicion,
                null,
                etiquetaInicio
        );

        etiqueta(
                etiquetaFin
        );
    }

    private void generarPara(
            PAst.Para para
    ) {
        para.inicializacion()
                .ifPresent(
                        inicializacion -> {

                            if (inicializacion
                                    instanceof PAst.Declaracion declaracion) {

                                generarDeclaracion(
                                        declaracion
                                );

                            } else if (inicializacion
                                    instanceof PAst.Expresion expresion) {

                                generarExpresion(
                                        expresion
                                );
                            }
                        }
                );

        String etiquetaCondicion =
                programa.nuevaEtiqueta();

        String etiquetaActualizacion =
                programa.nuevaEtiqueta();

        String etiquetaFin =
                programa.nuevaEtiqueta();

        etiqueta(
                etiquetaCondicion
        );

        para.condicion()
                .ifPresent(
                        condicion -> {

                            String valor =
                                    generarExpresion(
                                            condicion
                                    );

                            agregar(
                                    OperadorCuarteta.SALTAR_SI_FALSO,
                                    valor,
                                    null,
                                    etiquetaFin
                            );
                        }
                );

        etiquetasRomper.push(
                etiquetaFin
        );

        etiquetasContinuar.push(
                etiquetaActualizacion
        );

        generarSentencias(
                para.cuerpo()
        );

        etiquetasContinuar.pop();
        etiquetasRomper.pop();

        etiqueta(
                etiquetaActualizacion
        );

        para.actualizacion()
                .ifPresent(
                        this::generarExpresion
                );

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetaCondicion
        );

        etiqueta(
                etiquetaFin
        );
    }

    private void generarRomper() {
        if (etiquetasRomper.isEmpty()) {
            throw new IllegalStateException(
                    "No existe contexto para interrumpe"
            );
        }

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetasRomper.peek()
        );
    }

    private void generarContinuar() {
        if (etiquetasContinuar.isEmpty()) {
            throw new IllegalStateException(
                    "No existe contexto para perge"
            );
        }

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetasContinuar.peek()
        );
    }

    private String generarExpresion(
            PAst.Expresion expresion
    ) {
        if (expresion
                instanceof PAst.Literal literal) {

            return literal.lexema();
        }

        if (expresion
                instanceof PAst.Identificador identificador) {

            return identificador.nombre();
        }

        if (expresion
                instanceof PAst.Binaria binaria) {

            return generarBinaria(
                    binaria
            );
        }

        if (expresion
                instanceof PAst.Unaria unaria) {

            return generarUnaria(
                    unaria
            );
        }

        if (expresion
                instanceof PAst.Asignacion asignacion) {

            return generarAsignacion(
                    asignacion
            );
        }

        if (expresion
                instanceof PAst.Llamada llamada) {

            return generarLlamada(
                    llamada
            );
        }

        if (expresion
                instanceof PAst.AccesoArreglo
                || expresion
                instanceof PAst.AccesoMiembro) {

            return leerLugar(
                    expresion
            );
        }

        if (expresion
                instanceof PAst.CambioPostfijo cambio) {

            return generarPostfijo(
                    cambio
            );
        }

        if (expresion
                instanceof PAst.NuevoObjeto nuevo) {

            return generarNuevoObjeto(
                    nuevo
            );
        }

        throw new IllegalStateException(
                "Expresion Pig Latin no soportada: "
                        + expresion.getClass()
                        .getSimpleName()
        );
    }

    private String generarBinaria(
            PAst.Binaria binaria
    ) {
        String izquierda =
                generarExpresion(
                        binaria.izquierda()
                );

        String derecha =
                generarExpresion(
                        binaria.derecha()
                );

        String temporal =
                programa.nuevoTemporal();

        agregar(
                operadorBinario(
                        binaria.operador()
                ),
                izquierda,
                derecha,
                temporal
        );

        return temporal;
    }

    private String generarUnaria(
            PAst.Unaria unaria
    ) {
        if (unaria.operador()
                == PAst.OperadorUnario.INCREMENTO_PRE
                || unaria.operador()
                == PAst.OperadorUnario.DECREMENTO_PRE) {

            String lugar =
                    generarLugar(
                            unaria.expresion()
                    );

            String actual =
                    leerLugar(
                            unaria.expresion()
                    );

            String temporal =
                    programa.nuevoTemporal();

            OperadorCuarteta operador =
                    unaria.operador()
                            == PAst.OperadorUnario.INCREMENTO_PRE
                            ? OperadorCuarteta.SUMAR
                            : OperadorCuarteta.RESTAR;

            agregar(
                    operador,
                    actual,
                    "1",
                    temporal
            );

            agregar(
                    OperadorCuarteta.ASIGNAR,
                    temporal,
                    null,
                    lugar
            );

            return temporal;
        }

        String valor =
                generarExpresion(
                        unaria.expresion()
                );

        if (unaria.operador()
                == PAst.OperadorUnario.POSITIVO) {

            return valor;
        }

        String temporal =
                programa.nuevoTemporal();

        agregar(
                unaria.operador()
                        == PAst.OperadorUnario.NEGACION
                        ? OperadorCuarteta.NEGAR
                        : OperadorCuarteta.NEGATIVO,
                valor,
                null,
                temporal
        );

        return temporal;
    }

    private String generarAsignacion(
            PAst.Asignacion asignacion
    ) {
        String destino =
                generarLugar(
                        asignacion.destino()
                );

        String valor =
                generarExpresion(
                        asignacion.valor()
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                null,
                destino
        );

        return destino;
    }

    private String generarLlamada(
            PAst.Llamada llamada
    ) {
        EnlacesPig.EnlaceLlamada enlace =
                enlaces.buscarLlamada(
                        llamada
                ).orElse(
                        null
                );

        int desplazamiento =
                0;

        if (enlace != null
                && enlace.tieneReceptor()) {

            String receptor =
                    generarExpresion(
                            enlace.receptor()
                                    .orElseThrow()
                    );

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    receptor,
                    "0",
                    null
            );

            desplazamiento =
                    1;
        }

        for (int indice = 0;
             indice < llamada.argumentos()
                     .size();
             indice++) {

            String valor =
                    generarExpresion(
                            llamada.argumentos()
                                    .get(indice)
                    );

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    valor,
                    String.valueOf(
                            indice
                                    + desplazamiento
                    ),
                    null
            );
        }

        String objetivo =
                enlace == null
                        ? generarObjetivoLlamada(
                        llamada.objetivo()
                )
                        : enlace.destino();

        String temporal =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.LLAMAR,
                objetivo,
                String.valueOf(
                        llamada.argumentos()
                                .size()
                                + desplazamiento
                ),
                temporal
        );

        return temporal;
    }

    private String generarNuevoObjeto(
            PAst.NuevoObjeto nuevo
    ) {
        for (int indice = 0;
             indice < nuevo.argumentos()
                     .size();
             indice++) {

            String valor =
                    generarExpresion(
                            nuevo.argumentos()
                                    .get(indice)
                    );

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    valor,
                    String.valueOf(
                            indice
                    ),
                    null
            );
        }

        EnlacesPig.EnlaceConstructor enlace =
                enlaces.buscarConstructor(
                        nuevo
                ).orElse(
                        null
                );

        String destino =
                enlace == null
                        ? nuevo.tipo()
                        : enlace.destino();

        String temporal =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.NUEVO_OBJETO,
                destino,
                String.valueOf(
                        nuevo.argumentos()
                                .size()
                ),
                temporal
        );

        return temporal;
    }

    private String generarPostfijo(
            PAst.CambioPostfijo cambio
    ) {
        String lugar =
                generarLugar(
                        cambio.objetivo()
                );

        String actual =
                leerLugar(
                        cambio.objetivo()
                );

        String anterior =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.ASIGNAR,
                actual,
                null,
                anterior
        );

        String nuevo =
                programa.nuevoTemporal();

        agregar(
                cambio.operacion()
                        == PAst.OperacionPostfija.INCREMENTO
                        ? OperadorCuarteta.SUMAR
                        : OperadorCuarteta.RESTAR,
                anterior,
                "1",
                nuevo
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevo,
                null,
                lugar
        );

        return anterior;
    }

    private String generarLugar(
            PAst.Expresion expresion
    ) {
        if (expresion
                instanceof PAst.Identificador identificador) {

            return identificador.nombre();
        }

        if (expresion
                instanceof PAst.AccesoMiembro acceso) {

            return generarBaseAcceso(
                    acceso.objetivo()
            )
                    + "."
                    + acceso.miembro();
        }

        if (expresion
                instanceof PAst.AccesoArreglo acceso) {

            String indice =
                    generarExpresion(
                            acceso.indice()
                    );

            return generarBaseAcceso(
                    acceso.objetivo()
            )
                    + "["
                    + indice
                    + "]";
        }

        throw new IllegalStateException(
                "La expresion no es asignable: "
                        + expresion.getClass()
                        .getSimpleName()
        );
    }

    private String generarBaseAcceso(
            PAst.Expresion expresion
    ) {
        if (expresion
                instanceof PAst.Identificador
                || expresion
                instanceof PAst.AccesoMiembro
                || expresion
                instanceof PAst.AccesoArreglo) {

            return generarLugar(
                    expresion
            );
        }

        return generarExpresion(
                expresion
        );
    }

    private String leerLugar(
            PAst.Expresion expresion
    ) {
        String lugar =
                generarLugar(
                        expresion
                );

        if (expresion
                instanceof PAst.Identificador) {

            return lugar;
        }

        String temporal =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.ASIGNAR,
                lugar,
                null,
                temporal
        );

        return temporal;
    }

    private String generarObjetivoLlamada(
            PAst.Expresion expresion
    ) {
        if (expresion
                instanceof PAst.Identificador identificador) {

            return identificador.nombre();
        }

        if (expresion
                instanceof PAst.AccesoMiembro acceso) {

            return generarBaseAcceso(
                    acceso.objetivo()
            )
                    + "."
                    + acceso.miembro();
        }

        return generarExpresion(
                expresion
        );
    }

    private String serializarLista(
            PAst.InicializadorLista lista
    ) {
        List<String> valores =
                new ArrayList<>();

        for (PAst.Inicializador valor
                : lista.valores()) {

            if (valor
                    instanceof PAst.InicializadorLista interna) {

                valores.add(
                        serializarLista(
                                interna
                        )
                );

            } else {

                valores.add(
                        generarExpresion(
                                (PAst.Expresion) valor
                        )
                );
            }
        }

        return "{"
                + String.join(
                ", ",
                valores
        )
                + "}";
    }

    private OperadorCuarteta operadorBinario(
            PAst.OperadorBinario operador
    ) {
        return switch (operador) {

            case SUMA ->
                    OperadorCuarteta.SUMAR;

            case RESTA ->
                    OperadorCuarteta.RESTAR;

            case MULTIPLICACION ->
                    OperadorCuarteta.MULTIPLICAR;

            case DIVISION ->
                    OperadorCuarteta.DIVIDIR;

            case IGUALDAD ->
                    OperadorCuarteta.IGUALDAD;

            case DIFERENTE ->
                    OperadorCuarteta.DIFERENTE;

            case MENOR ->
                    OperadorCuarteta.MENOR;

            case MAYOR ->
                    OperadorCuarteta.MAYOR;

            case MENOR_IGUAL ->
                    OperadorCuarteta.MENOR_IGUAL;

            case MAYOR_IGUAL ->
                    OperadorCuarteta.MAYOR_IGUAL;

            case AND ->
                    OperadorCuarteta.AND;

            case OR ->
                    OperadorCuarteta.OR;
        };
    }

    private void etiqueta(
            String nombre
    ) {
        agregar(
                OperadorCuarteta.ETIQUETA,
                null,
                null,
                nombre
        );
    }

    private void agregar(
            OperadorCuarteta operador,
            String argumento1,
            String argumento2,
            String resultado
    ) {
        programa.agregar(
                new Cuarteta(
                        operador,
                        argumento1,
                        argumento2,
                        resultado
                )
        );
    }
}