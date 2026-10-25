package com.compi2.contacto.ir;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.semantica.AnalizadorSemanticoZ;
import com.compi2.contacto.semantica.EnlacesZ;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GeneradorCuartetasZ {

    private ProgramaIntermedio programa;
    private final Deque<String> etiquetasRomper;
    private final Deque<String> etiquetasContinuar;
    private String claseActual;
    private EnlacesZ enlaces;

    public GeneradorCuartetasZ() {
        etiquetasRomper = new ArrayDeque<>();
        etiquetasContinuar = new ArrayDeque<>();
        enlaces = new EnlacesZ();
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programas
    ) {
        Objects.requireNonNull(
                programas,
                "Los programas son obligatorios"
        );

        AnalizadorSemanticoZ analizador =
                new AnalizadorSemanticoZ();

        analizador.analizar(
                programas
        );

        return generar(
                programas,
                analizador.enlaces()
        );
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programas,
            EnlacesZ enlaces
    ) {
        Objects.requireNonNull(
                programas,
                "Los programas son obligatorios"
        );

        this.enlaces =
                Objects.requireNonNull(
                        enlaces,
                        "Los enlaces Z son obligatorios"
                );

        programa = new ProgramaIntermedio();
        etiquetasRomper.clear();
        etiquetasContinuar.clear();
        claseActual = null;

        for (ProgramaAst ast : programas) {
            generarPrograma(ast);
        }

        return programa;
    }

    private void generarPrograma(
            ProgramaAst ast
    ) {
        for (NodoAst nodo : ast.elementos()) {
            if (nodo instanceof ZAst.Clase clase) {
                generarClase(clase);
            }
        }
    }

    private void generarClase(
            ZAst.Clase clase
    ) {
        String anterior =
                claseActual;

        claseActual =
                clase.nombre();

        for (ZAst.Miembro miembro : clase.miembros()) {
            if (miembro instanceof ZAst.Constructor constructor) {
                generarConstructor(
                        constructor
                );
            }
        }

        for (ZAst.Miembro miembro : clase.miembros()) {
            if (miembro instanceof ZAst.Metodo metodo) {
                generarMetodo(
                        metodo
                );
            }
        }

        claseActual =
                anterior;
    }

    private void generarConstructor(
            ZAst.Constructor constructor
    ) {
        String nombre =
                nombreConstructor(
                        constructor
                );

        agregar(
                OperadorCuarteta.INICIO_FUNCION,
                nombre,
                claseActual,
                String.valueOf(
                        constructor.parametros()
                                .size() + 1
                )
        );

        agregar(
                OperadorCuarteta.DECLARAR_PARAMETRO,
                claseActual,
                "THIS",
                "this"
        );

        for (ZAst.Parametro parametro
                : constructor.parametros()) {

            agregar(
                    OperadorCuarteta.DECLARAR_PARAMETRO,
                    parametro.tipo()
                            .nombreCompleto(),
                    "PARAMETRO",
                    parametro.nombre()
            );
        }

        generarBloque(
                constructor.cuerpo()
        );

        agregar(
                OperadorCuarteta.RETORNAR,
                "this",
                null,
                null
        );

        agregar(
                OperadorCuarteta.FIN_FUNCION,
                nombre,
                null,
                null
        );
    }

    private void generarMetodo(
            ZAst.Metodo metodo
    ) {
        String nombre =
                nombreMetodo(
                        metodo
                );

        String retorno =
                metodo.retorno()
                        .map(
                                ZAst.Tipo::nombreCompleto
                        )
                        .orElse(
                                "void"
                        );

        agregar(
                OperadorCuarteta.INICIO_FUNCION,
                nombre,
                retorno,
                String.valueOf(
                        metodo.parametros()
                                .size() + 1
                )
        );

        agregar(
                OperadorCuarteta.DECLARAR_PARAMETRO,
                claseActual,
                "THIS",
                "this"
        );

        for (ZAst.Parametro parametro
                : metodo.parametros()) {

            agregar(
                    OperadorCuarteta.DECLARAR_PARAMETRO,
                    parametro.tipo()
                            .nombreCompleto(),
                    "PARAMETRO",
                    parametro.nombre()
            );
        }

        generarBloque(
                metodo.cuerpo()
        );

        if (metodo.esVoid()) {
            agregar(
                    OperadorCuarteta.RETORNAR,
                    null,
                    null,
                    null
            );
        }

        agregar(
                OperadorCuarteta.FIN_FUNCION,
                nombre,
                null,
                null
        );
    }

    private void generarBloque(
            ZAst.Bloque bloque
    ) {
        for (ZAst.Sentencia sentencia
                : bloque.sentencias()) {

            generarSentencia(
                    sentencia
            );
        }
    }

    private void generarSentencia(
            ZAst.Sentencia sentencia
    ) {
        if (sentencia instanceof ZAst.Bloque bloque) {
            generarBloque(
                    bloque
            );

            return;
        }

        if (sentencia instanceof ZAst.Declaracion declaracion) {
            generarDeclaracion(
                    declaracion
            );

            return;
        }

        if (sentencia instanceof ZAst.ExpresionSentencia expresion) {
            generarExpresion(
                    expresion.expresion()
            );

            return;
        }

        if (sentencia instanceof ZAst.Si si) {
            generarSi(
                    si
            );

            return;
        }

        if (sentencia instanceof ZAst.Seleccion seleccion) {
            generarSwitch(
                    seleccion
            );

            return;
        }

        if (sentencia instanceof ZAst.Para para) {
            generarPara(
                    para
            );

            return;
        }

        if (sentencia instanceof ZAst.Mientras mientras) {
            generarMientras(
                    mientras
            );

            return;
        }

        if (sentencia instanceof ZAst.HacerMientras hacer) {
            generarHacerMientras(
                    hacer
            );

            return;
        }

        if (sentencia instanceof ZAst.Retorno retorno) {
            generarRetorno(
                    retorno
            );

            return;
        }

        if (sentencia instanceof ZAst.Romper) {
            generarRomper();

            return;
        }

        if (sentencia instanceof ZAst.Continuar) {
            generarContinuar();
        }
    }

    private void generarDeclaracion(
            ZAst.Declaracion declaracion
    ) {
        agregar(
                OperadorCuarteta.DECLARAR,
                declaracion.tipo()
                        .nombreCompleto(),
                null,
                declaracion.nombre()
        );

        if (declaracion.inicializador()
                .isEmpty()) {

            return;
        }

        ZAst.Inicializador inicializador =
                declaracion.inicializador()
                        .orElseThrow();

        if (inicializador instanceof ZAst.InicializadorLista lista) {
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
                        (ZAst.Expresion) inicializador
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                null,
                declaracion.nombre()
        );
    }

    private void generarSi(
            ZAst.Si si
    ) {
        String etiquetaSino =
                programa.nuevaEtiqueta();

        String etiquetaFin =
                programa.nuevaEtiqueta();

        String condicion =
                generarExpresion(
                        si.condicion()
                );

        agregar(
                OperadorCuarteta.SALTAR_SI_FALSO,
                condicion,
                null,
                etiquetaSino
        );

        generarSentencia(
                si.entonces()
        );

        if (si.sino().isPresent()) {
            agregar(
                    OperadorCuarteta.SALTAR,
                    null,
                    null,
                    etiquetaFin
            );

            etiqueta(
                    etiquetaSino
            );

            generarSentencia(
                    si.sino()
                            .orElseThrow()
            );

            etiqueta(
                    etiquetaFin
            );

        } else {
            etiqueta(
                    etiquetaSino
            );
        }
    }

    private void generarSwitch(
            ZAst.Seleccion seleccion
    ) {
        String selector =
                generarExpresion(
                        seleccion.expresion()
                );

        String etiquetaFin =
                programa.nuevaEtiqueta();

        List<String> etiquetas =
                new ArrayList<>();

        String etiquetaDefault =
                null;

        for (ZAst.CasoSeleccion caso
                : seleccion.casos()) {

            String etiquetaCaso =
                    programa.nuevaEtiqueta();

            etiquetas.add(
                    etiquetaCaso
            );

            if (caso.esDefault()) {
                etiquetaDefault =
                        etiquetaCaso;
            }
        }

        for (int indice = 0;
             indice < seleccion.casos().size();
             indice++) {

            ZAst.CasoSeleccion caso =
                    seleccion.casos()
                            .get(indice);

            if (caso.esDefault()) {
                continue;
            }

            String valorCaso =
                    generarExpresion(
                            caso.valor()
                                    .orElseThrow()
                    );

            String temporal =
                    programa.nuevoTemporal();

            agregar(
                    OperadorCuarteta.IGUALDAD,
                    selector,
                    valorCaso,
                    temporal
            );

            agregar(
                    OperadorCuarteta.SALTAR_SI_VERDADERO,
                    temporal,
                    null,
                    etiquetas.get(indice)
            );
        }

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetaDefault == null
                        ? etiquetaFin
                        : etiquetaDefault
        );

        etiquetasRomper.push(
                etiquetaFin
        );

        for (int indice = 0;
             indice < seleccion.casos().size();
             indice++) {

            etiqueta(
                    etiquetas.get(indice)
            );

            ZAst.CasoSeleccion caso =
                    seleccion.casos()
                            .get(indice);

            for (ZAst.Sentencia sentencia
                    : caso.sentencias()) {

                generarSentencia(
                        sentencia
                );
            }
        }

        etiquetasRomper.pop();

        etiqueta(
                etiquetaFin
        );
    }

    private void generarPara(
            ZAst.Para para
    ) {
        if (para.inicializacion()
                .isPresent()) {

            NodoAst inicio =
                    para.inicializacion()
                            .orElseThrow();

            if (inicio instanceof ZAst.Declaracion declaracion) {
                generarDeclaracion(
                        declaracion
                );

            } else if (inicio instanceof ZAst.ListaExpresiones lista) {

                for (ZAst.Expresion expresion
                        : lista.expresiones()) {

                    generarExpresion(
                            expresion
                    );
                }
            }
        }

        String etiquetaCondicion =
                programa.nuevaEtiqueta();

        String etiquetaActualizacion =
                programa.nuevaEtiqueta();

        String etiquetaFin =
                programa.nuevaEtiqueta();

        etiqueta(
                etiquetaCondicion
        );

        if (para.condicion()
                .isPresent()) {

            String condicion =
                    generarExpresion(
                            para.condicion()
                                    .orElseThrow()
                    );

            agregar(
                    OperadorCuarteta.SALTAR_SI_FALSO,
                    condicion,
                    null,
                    etiquetaFin
            );
        }

        etiquetasRomper.push(
                etiquetaFin
        );

        etiquetasContinuar.push(
                etiquetaActualizacion
        );

        generarSentencia(
                para.cuerpo()
        );

        etiquetasContinuar.pop();
        etiquetasRomper.pop();

        etiqueta(
                etiquetaActualizacion
        );

        for (ZAst.Expresion actualizacion
                : para.actualizaciones()) {

            generarExpresion(
                    actualizacion
            );
        }

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

    private void generarMientras(
            ZAst.Mientras mientras
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

        generarSentencia(
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
            ZAst.HacerMientras hacer
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

        generarSentencia(
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

    private void generarRetorno(
            ZAst.Retorno retorno
    ) {
        String valor =
                retorno.expresion()
                        .map(
                                this::generarExpresion
                        )
                        .orElse(
                                null
                        );

        agregar(
                OperadorCuarteta.RETORNAR,
                valor,
                null,
                null
        );
    }

    private void generarRomper() {
        if (etiquetasRomper.isEmpty()) {
            throw new IllegalStateException(
                    "No existe contexto para break"
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
                    "No existe contexto para continue"
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
            ZAst.Expresion expresion
    ) {
        if (expresion instanceof ZAst.Literal literal) {
            return literal.lexema();
        }

        if (expresion instanceof ZAst.Identificador identificador) {
            return identificador.nombre();
        }

        if (expresion instanceof ZAst.Binaria binaria) {
            return generarBinaria(
                    binaria
            );
        }

        if (expresion instanceof ZAst.Unaria unaria) {
            return generarUnaria(
                    unaria
            );
        }

        if (expresion instanceof ZAst.AsignacionExpresion asignacion) {
            return generarAsignacion(
                    asignacion
            );
        }

        if (expresion instanceof ZAst.Ternaria ternaria) {
            return generarTernaria(
                    ternaria
            );
        }

        if (expresion instanceof ZAst.Llamada llamada) {
            return generarLlamada(
                    llamada
            );
        }

        if (expresion instanceof ZAst.AccesoArreglo
                || expresion instanceof ZAst.AccesoMiembro) {

            return leerLugar(
                    expresion
            );
        }

        if (expresion instanceof ZAst.CambioPostfijo cambio) {
            return generarPostfijo(
                    cambio
            );
        }

        if (expresion instanceof ZAst.NuevoObjeto nuevo) {
            return generarNuevoObjeto(
                    nuevo
            );
        }

        if (expresion instanceof ZAst.NuevoArreglo nuevo) {
            return generarNuevoArreglo(
                    nuevo
            );
        }

        throw new IllegalStateException(
                "Expresion Zetariano no soportada: "
                        + expresion.getClass()
                        .getSimpleName()
        );
    }

    private String generarBinaria(
            ZAst.Binaria binaria
    ) {
        String izquierda =
                generarExpresion(
                        binaria.izquierda()
                );

        String derecha =
                generarExpresion(
                        binaria.derecha()
                );

        if (binaria.operador()
                == ZAst.OperadorBinario.DIVISION
                && esTipoInt(
                enlaces.buscarTipo(
                        binaria
                ).orElse(
                        ""
                )
        )) {

            return generarDivisionEntera(
                    izquierda,
                    derecha
            );
        }

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
            ZAst.Unaria unaria
    ) {
        if (unaria.operador()
                == ZAst.OperadorUnario.INCREMENTO_PRE
                || unaria.operador()
                == ZAst.OperadorUnario.DECREMENTO_PRE) {

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
                            == ZAst.OperadorUnario.INCREMENTO_PRE
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
                == ZAst.OperadorUnario.POSITIVO) {

            return valor;
        }

        String temporal =
                programa.nuevoTemporal();

        OperadorCuarteta operador =
                unaria.operador()
                        == ZAst.OperadorUnario.NEGACION
                        ? OperadorCuarteta.NEGAR
                        : OperadorCuarteta.NEGATIVO;

        agregar(
                operador,
                valor,
                null,
                temporal
        );

        return temporal;
    }

    private String generarAsignacion(
            ZAst.AsignacionExpresion asignacion
    ) {
        String destino =
                generarLugar(
                        asignacion.destino()
                );

        String valor =
                generarExpresion(
                        asignacion.valor()
                );

        if (asignacion.operador()
                == ZAst.OperadorAsignacion.ASIGNAR) {

            agregar(
                    OperadorCuarteta.ASIGNAR,
                    valor,
                    null,
                    destino
            );

            return destino;
        }

        String actual =
                leerLugar(
                        asignacion.destino()
                );

        String temporal;

        if (asignacion.operador()
                == ZAst.OperadorAsignacion.DIVIDIR_ASIGNAR
                && esTipoInt(
                enlaces.buscarTipo(
                        asignacion.destino()
                ).orElse(
                        ""
                )
        )) {

            temporal =
                    generarDivisionEntera(
                            actual,
                            valor
                    );

        } else {
            temporal =
                    programa.nuevoTemporal();

            agregar(
                    operadorAsignacionCompuesta(
                            asignacion.operador()
                    ),
                    actual,
                    valor,
                    temporal
            );
        }

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporal,
                null,
                destino
        );

        return temporal;
    }

    private String generarTernaria(
            ZAst.Ternaria ternaria
    ) {
        String temporal =
                programa.nuevoTemporal();

        String etiquetaFalso =
                programa.nuevaEtiqueta();

        String etiquetaFin =
                programa.nuevaEtiqueta();

        String condicion =
                generarExpresion(
                        ternaria.condicion()
                );

        agregar(
                OperadorCuarteta.SALTAR_SI_FALSO,
                condicion,
                null,
                etiquetaFalso
        );

        String verdadero =
                generarExpresion(
                        ternaria.verdadero()
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                verdadero,
                null,
                temporal
        );

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetaFin
        );

        etiqueta(
                etiquetaFalso
        );

        String falso =
                generarExpresion(
                        ternaria.falso()
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                falso,
                null,
                temporal
        );

        etiqueta(
                etiquetaFin
        );

        return temporal;
    }

    private String generarLlamada(
            ZAst.Llamada llamada
    ) {
        String objetivo =
                generarObjetivoLlamada(
                        llamada.objetivo()
                );

        if (objetivo.equals("print")
                || objetivo.equals("println")
                || objetivo.equals("System.out.print")
                || objetivo.equals("System.out.println")) {

            for (ZAst.Expresion argumento
                    : llamada.argumentos()) {

                String modo =
                        objetivo.endsWith(
                                "println"
                        )
                                ? "println"
                                : "print";

                String tipo =
                        enlaces.buscarTipo(
                                argumento
                        ).orElse(
                                ""
                        );

                if (esTipoChar(
                        tipo
                )) {
                    modo += ":char";

                } else if (esTipoBoolean(
                        tipo
                )) {
                    modo += ":boolean";
                }

                agregar(
                        OperadorCuarteta.IMPRIMIR,
                        generarExpresion(
                                argumento
                        ),
                        modo,
                        null
                );
            }

            return "-";
        }

        if (objetivo.equals("readln")) {
            String temporal =
                    programa.nuevoTemporal();

            agregar(
                    OperadorCuarteta.LEER,
                    "String",
                    null,
                    temporal
            );

            return temporal;
        }

        String destino =
                objetivoResueltoLlamada(
                        llamada,
                        objetivo
                );

        for (int indice = 0;
             indice < llamada.argumentos()
                     .size();
             indice++) {

            String argumento =
                    generarExpresion(
                            llamada.argumentos()
                                    .get(indice)
                    );

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    argumento,
                    String.valueOf(
                            indice
                    ),
                    null
            );
        }

        String temporal =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.LLAMAR,
                destino,
                String.valueOf(
                        llamada.argumentos()
                                .size()
                ),
                temporal
        );

        return temporal;
    }

    private String generarValorIndice(
            ZAst.Expresion expresion
    ) {
        String valor = generarExpresion(expresion);
        String temporal = programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                null,
                temporal
        );

        return temporal;
    }

    private String generarNuevoObjeto(
            ZAst.NuevoObjeto nuevo
    ) {
        for (int indice = 0;
             indice < nuevo.argumentos()
                     .size();
             indice++) {

            String argumento =
                    generarExpresion(
                            nuevo.argumentos()
                                    .get(indice)
                    );

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    argumento,
                    String.valueOf(
                            indice
                    ),
                    null
            );
        }

        String temporal =
                programa.nuevoTemporal();

        String constructor =
                enlaces.buscarConstructor(
                        nuevo
                ).orElse(
                        nuevo.tipo()
                );

        agregar(
                OperadorCuarteta.NUEVO_OBJETO,
                constructor,
                String.valueOf(
                        nuevo.argumentos()
                                .size()
                ),
                temporal
        );

        return temporal;
    }

    private String generarNuevoArreglo(
            ZAst.NuevoArreglo nuevo
    ) {
        List<String> dimensiones = nuevo.dimensiones()
                .stream()
                .map(this::generarValorIndice)
                .toList();

        String temporal = programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.NUEVO_ARREGLO,
                nuevo.tipoBase(),
                String.join(",", dimensiones),
                temporal
        );

        return temporal;
    }

    private String generarPostfijo(
            ZAst.CambioPostfijo cambio
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

        OperadorCuarteta operador =
                cambio.operacion()
                        == ZAst.OperacionPostfija.INCREMENTO
                        ? OperadorCuarteta.SUMAR
                        : OperadorCuarteta.RESTAR;

        agregar(
                operador,
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
            ZAst.Expresion expresion
    ) {
        if (expresion instanceof ZAst.Identificador identificador) {
            return identificador.nombre();
        }

        if (expresion instanceof ZAst.AccesoMiembro acceso) {
            return generarBaseAcceso(acceso.objetivo())
                    + "."
                    + acceso.miembro();
        }

        if (expresion instanceof ZAst.AccesoArreglo acceso) {
            String indice = generarValorIndice(acceso.indice());

            return generarBaseAcceso(acceso.objetivo())
                    + "["
                    + indice
                    + "]";
        }

        throw new IllegalStateException(
                "La expresion no es asignable: "
                        + expresion.getClass().getSimpleName()
        );
    }

    private String generarBaseAcceso(
            ZAst.Expresion expresion
    ) {
        if (expresion instanceof ZAst.Identificador
                || expresion instanceof ZAst.AccesoMiembro
                || expresion instanceof ZAst.AccesoArreglo) {

            return generarLugar(
                    expresion
            );
        }

        return generarExpresion(
                expresion
        );
    }

    private String leerLugar(
            ZAst.Expresion expresion
    ) {
        String lugar =
                generarLugar(
                        expresion
                );

        if (expresion instanceof ZAst.Identificador) {
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

    private String objetivoResueltoLlamada(
            ZAst.Llamada llamada,
            String objetivo
    ) {
        return enlaces.buscarLlamada(
                llamada
        ).map(
                destino -> {
                    int punto =
                            destino.indexOf('.');

                    String firma =
                            punto >= 0
                                    ? destino.substring(
                                    punto + 1
                            )
                                    : destino;

                    if (llamada.objetivo()
                            instanceof ZAst.Identificador identificador) {

                        if (identificador.nombre().equals("super")) {
                            return destino;
                        }

                        return firma;
                    }

                    if (llamada.objetivo()
                            instanceof ZAst.AccesoMiembro acceso) {

                        int separador = objetivo.lastIndexOf('.');
                        String receptor = objetivo.substring(0, separador);

                        if (acceso.objetivo() instanceof ZAst.AccesoArreglo) {
                            String referencia = programa.nuevoTemporal();
                            agregar(
                                    OperadorCuarteta.ASIGNAR,
                                    receptor,
                                    null,
                                    referencia
                            );
                            return "@receptor@" + referencia + "@" + destino;
                        }

                        return receptor + "." + firma;
                    }

                    return objetivo;
                }
        ).orElse(
                objetivo
        );
    }

    private String generarObjetivoLlamada(
            ZAst.Expresion objetivo
    ) {
        if (objetivo instanceof ZAst.Identificador identificador) {
            return identificador.nombre();
        }

        if (objetivo instanceof ZAst.AccesoMiembro acceso) {
            return generarBaseAcceso(
                    acceso.objetivo()
            )
                    + "."
                    + acceso.miembro();
        }

        return generarExpresion(
                objetivo
        );
    }

    private String serializarLista(
            ZAst.InicializadorLista lista
    ) {
        List<String> valores =
                new ArrayList<>();

        for (ZAst.Inicializador valor
                : lista.valores()) {

            if (valor instanceof ZAst.InicializadorLista interna) {
                valores.add(
                        serializarLista(
                                interna
                        )
                );

            } else {
                valores.add(
                        generarExpresion(
                                (ZAst.Expresion) valor
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

    private String generarDivisionEntera(
            String izquierda,
            String derecha
    ) {
        String resto =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.MODULO,
                izquierda,
                derecha,
                resto
        );

        String ajustado =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.RESTAR,
                izquierda,
                resto,
                ajustado
        );

        String cociente =
                programa.nuevoTemporal();

        agregar(
                OperadorCuarteta.DIVIDIR,
                ajustado,
                derecha,
                cociente
        );

        return cociente;
    }

    private boolean esTipoInt(
            String tipo
    ) {
        return "int".equals(
                tipo
        );
    }

    private boolean esTipoChar(
            String tipo
    ) {
        return "char".equals(
                tipo
        );
    }

    private boolean esTipoBoolean(
            String tipo
    ) {
        return "boolean".equals(
                tipo
        );
    }

    private OperadorCuarteta operadorBinario(
            ZAst.OperadorBinario operador
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

            case MODULO ->
                    OperadorCuarteta.MODULO;

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

    private OperadorCuarteta operadorAsignacionCompuesta(
            ZAst.OperadorAsignacion operador
    ) {
        return switch (operador) {
            case SUMAR_ASIGNAR ->
                    OperadorCuarteta.SUMAR;

            case RESTAR_ASIGNAR ->
                    OperadorCuarteta.RESTAR;

            case MULTIPLICAR_ASIGNAR ->
                    OperadorCuarteta.MULTIPLICAR;

            case DIVIDIR_ASIGNAR ->
                    OperadorCuarteta.DIVIDIR;

            case ASIGNAR ->
                    throw new IllegalArgumentException(
                            "La asignacion simple no es compuesta"
                    );
        };
    }

    private String nombreConstructor(
            ZAst.Constructor constructor
    ) {
        return claseActual
                + ".<init>("
                + tiposParametros(
                constructor.parametros()
        )
                + ")";
    }

    private String nombreMetodo(
            ZAst.Metodo metodo
    ) {
        return claseActual
                + "."
                + metodo.nombre()
                + "("
                + tiposParametros(
                metodo.parametros()
        )
                + ")";
    }

    private String tiposParametros(
            List<ZAst.Parametro> parametros
    ) {
        return parametros.stream()
                .map(
                        parametro ->
                                parametro.tipo()
                                        .nombreCompleto()
                )
                .collect(
                        Collectors.joining(",")
                );
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