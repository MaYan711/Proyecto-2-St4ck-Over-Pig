package com.compi2.contacto.ir;

import com.compi2.contacto.ast.AccesoArregloAst;
import com.compi2.contacto.ast.AccesoAtributoAst;
import com.compi2.contacto.ast.AsignacionAst;
import com.compi2.contacto.ast.BinariaAst;
import com.compi2.contacto.ast.CambioUnidadAst;
import com.compi2.contacto.ast.CambioUnidadExpresionAst;
import com.compi2.contacto.ast.ContinuarAst;
import com.compi2.contacto.ast.DeclaracionVariableAst;
import com.compi2.contacto.ast.ElegirAst;
import com.compi2.contacto.ast.EstructuraAst;
import com.compi2.contacto.ast.ExpresionAst;
import com.compi2.contacto.ast.ExpresionSentenciaAst;
import com.compi2.contacto.ast.FuncionAst;
import com.compi2.contacto.ast.HacerMientrasAst;
import com.compi2.contacto.ast.IdentificadorAst;
import com.compi2.contacto.ast.ImprimirAst;
import com.compi2.contacto.ast.InicializadorListaAst;
import com.compi2.contacto.ast.LeerAst;
import com.compi2.contacto.ast.LiteralAst;
import com.compi2.contacto.ast.LlamadaAst;
import com.compi2.contacto.ast.MientrasAst;
import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ParaAst;
import com.compi2.contacto.ast.ParametroAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.RetornarAst;
import com.compi2.contacto.ast.RomperAst;
import com.compi2.contacto.ast.SentenciaAst;
import com.compi2.contacto.ast.SiAst;
import com.compi2.contacto.ast.UnariaAst;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GeneradorCuartetasY {

    private ProgramaIntermedio programa;

    private final Map<String, FuncionAst> funciones;

    private final Deque<String> etiquetasRomper;
    private final Deque<String> etiquetasContinuar;

    public GeneradorCuartetasY() {
        funciones = new HashMap<>();
        etiquetasRomper = new ArrayDeque<>();
        etiquetasContinuar = new ArrayDeque<>();
    }

    public ProgramaIntermedio generar(
            List<ProgramaAst> programas
    ) {
        Objects.requireNonNull(
                programas,
                "Los programas son obligatorios"
        );

        programa = new ProgramaIntermedio();

        funciones.clear();
        etiquetasRomper.clear();
        etiquetasContinuar.clear();

        registrarFunciones(programas);

        for (ProgramaAst ast : programas) {
            generarPrograma(ast);
        }

        return programa;
    }

    private void registrarFunciones(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst ast : programas) {
            for (NodoAst elemento : ast.elementos()) {
                if (elemento instanceof FuncionAst funcion) {
                    funciones.put(
                            funcion.nombre(),
                            funcion
                    );
                }
            }
        }
    }

    private void generarPrograma(
            ProgramaAst ast
    ) {
        for (NodoAst elemento : ast.elementos()) {
            if (elemento instanceof FuncionAst funcion) {
                generarFuncion(funcion);
            }
        }
    }

    private void generarFuncion(
            FuncionAst funcion
    ) {
        String retorno = funcion.retorno()
                .map(tipo -> tipo.nombre())
                .orElse("void");

        agregar(
                OperadorCuarteta.INICIO_FUNCION,
                funcion.nombre(),
                retorno,
                String.valueOf(
                        funcion.parametros().size()
                )
        );

        for (ParametroAst parametro
                : funcion.parametros()) {

            agregar(
                    OperadorCuarteta.DECLARAR_PARAMETRO,
                    parametro.tipo().nombre(),
                    parametro.modo().name(),
                    parametro.nombre()
            );
        }

        generarBloque(
                funcion.cuerpo()
        );

        if (!funcion.tieneRetorno()) {
            agregar(
                    OperadorCuarteta.RETORNAR,
                    null,
                    null,
                    null
            );
        }

        agregar(
                OperadorCuarteta.FIN_FUNCION,
                funcion.nombre(),
                null,
                null
        );
    }

    private void generarBloque(
            List<SentenciaAst> sentencias
    ) {
        for (SentenciaAst sentencia : sentencias) {
            generarSentencia(sentencia);
        }
    }

    private void generarSentencia(
            SentenciaAst sentencia
    ) {
        if (sentencia instanceof EstructuraAst) {
            return;
        }

        if (sentencia
                instanceof DeclaracionVariableAst declaracion) {

            generarDeclaracion(declaracion);
            return;
        }

        if (sentencia
                instanceof AsignacionAst asignacion) {

            generarAsignacion(asignacion);
            return;
        }

        if (sentencia
                instanceof CambioUnidadAst cambio) {

            generarCambioUnidad(cambio);
            return;
        }

        if (sentencia instanceof SiAst condicion) {
            generarSi(condicion);
            return;
        }

        if (sentencia instanceof ElegirAst elegir) {
            generarElegir(elegir);
            return;
        }

        if (sentencia instanceof ParaAst para) {
            generarPara(para);
            return;
        }

        if (sentencia
                instanceof MientrasAst mientras) {

            generarMientras(mientras);
            return;
        }

        if (sentencia
                instanceof HacerMientrasAst hacer) {

            generarHacerMientras(hacer);
            return;
        }

        if (sentencia
                instanceof RetornarAst retornar) {

            String valor =
                    generarExpresion(
                            retornar.expresion()
                    );

            agregar(
                    OperadorCuarteta.RETORNAR,
                    valor,
                    null,
                    null
            );

            return;
        }

        if (sentencia instanceof RomperAst) {
            generarRomper();
            return;
        }

        if (sentencia instanceof ContinuarAst) {
            generarContinuar();
            return;
        }

        if (sentencia
                instanceof ImprimirAst imprimir) {

            String valor =
                    generarExpresion(
                            imprimir.expresion()
                    );

            agregar(
                    OperadorCuarteta.IMPRIMIR,
                    valor,
                    "println",
                    null
            );

            return;
        }

        if (sentencia
                instanceof ExpresionSentenciaAst expresion) {

            generarExpresion(
                    expresion.expresion()
            );
        }
    }

    private void generarDeclaracion(
            DeclaracionVariableAst declaracion
    ) {
        String dimensiones =
                generarDimensiones(
                        declaracion.dimensiones()
                );

        agregar(
                OperadorCuarteta.DECLARAR,
                declaracion.tipo().nombre(),
                dimensiones,
                declaracion.nombre()
        );

        if (declaracion.inicializador().isEmpty()) {
            if (!declaracion.dimensiones().isEmpty()) {
                agregar(
                        OperadorCuarteta.NUEVO_ARREGLO,
                        declaracion.tipo().nombre(),
                        dimensiones,
                        declaracion.nombre()
                );
            }

            return;
        }

        ExpresionAst inicializador =
                declaracion.inicializador()
                        .orElseThrow();

        if (inicializador
                instanceof InicializadorListaAst lista) {

            String tipoCompuesto =
                    declaracion.tipo()
                            .nombre()
                            + "[]".repeat(
                            declaracion.dimensiones()
                                    .size()
                    );

            agregar(
                    OperadorCuarteta.INICIALIZAR_COMPUESTO,
                    serializarLista(lista),
                    tipoCompuesto,
                    declaracion.nombre()
            );

            return;
        }

        String valor =
                generarExpresion(
                        inicializador
                );

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                null,
                declaracion.nombre()
        );
    }

    private String generarDimensiones(
            List<ExpresionAst> dimensiones
    ) {
        if (dimensiones.isEmpty()) {
            return null;
        }

        List<String> valores =
                new ArrayList<>();

        for (ExpresionAst dimension
                : dimensiones) {

            valores.add(
                    generarExpresion(dimension)
            );
        }

        return String.join(
                "x",
                valores
        );
    }

    private void generarAsignacion(
            AsignacionAst asignacion
    ) {
        String destino =
                generarLugar(
                        asignacion.destino()
                );

        if (asignacion.valor()
                instanceof InicializadorListaAst lista) {

            agregar(
                    OperadorCuarteta.INICIALIZAR_COMPUESTO,
                    serializarLista(lista),
                    null,
                    destino
            );

            return;
        }

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
    }

    private void generarCambioUnidad(
            CambioUnidadAst cambio
    ) {
        String lugar =
                generarLugar(
                        cambio.destino()
                );

        String actual =
                leerLugar(
                        cambio.destino()
                );

        String temporal =
                programa.nuevoTemporal();

        OperadorCuarteta operador =
                cambio.operacion()
                        == CambioUnidadAst.Operacion.INCREMENTO
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
    }

    private void generarSi(
            SiAst condicion
    ) {
        String etiquetaFin =
                programa.nuevaEtiqueta();

        for (SiAst.Rama rama
                : condicion.ramas()) {

            String etiquetaSiguiente =
                    programa.nuevaEtiqueta();

            String valorCondicion =
                    generarExpresion(
                            rama.condicion()
                    );

            agregar(
                    OperadorCuarteta.SALTAR_SI_FALSO,
                    valorCondicion,
                    null,
                    etiquetaSiguiente
            );

            generarBloque(
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

        if (!condicion.contrario().isEmpty()) {
            generarBloque(
                    condicion.contrario()
            );
        }

        etiqueta(
                etiquetaFin
        );
    }

    private void generarElegir(
            ElegirAst elegir
    ) {
        String selector =
                generarExpresion(
                        elegir.expresion()
                );

        String etiquetaFin =
                programa.nuevaEtiqueta();

        String etiquetaSiempre =
                elegir.siempre().isEmpty()
                        ? etiquetaFin
                        : programa.nuevaEtiqueta();

        List<String> etiquetasCasos =
                new ArrayList<>();

        for (int indice = 0;
             indice < elegir.casos().size();
             indice++) {

            etiquetasCasos.add(
                    programa.nuevaEtiqueta()
            );
        }

        for (int indice = 0;
             indice < elegir.casos().size();
             indice++) {

            ElegirAst.Caso caso =
                    elegir.casos().get(indice);

            String valorCaso =
                    generarExpresion(
                            caso.valor()
                    );

            String comparacion =
                    programa.nuevoTemporal();

            agregar(
                    OperadorCuarteta.IGUALDAD,
                    selector,
                    valorCaso,
                    comparacion
            );

            agregar(
                    OperadorCuarteta.SALTAR_SI_VERDADERO,
                    comparacion,
                    null,
                    etiquetasCasos.get(indice)
            );
        }

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetaSiempre
        );

        etiquetasRomper.push(
                etiquetaFin
        );

        for (int indice = 0;
             indice < elegir.casos().size();
             indice++) {

            etiqueta(
                    etiquetasCasos.get(indice)
            );

            generarBloque(
                    elegir.casos()
                            .get(indice)
                            .cuerpo()
            );
        }

        if (!elegir.siempre().isEmpty()) {
            etiqueta(
                    etiquetaSiempre
            );

            generarBloque(
                    elegir.siempre()
            );
        }

        etiquetasRomper.pop();

        etiqueta(
                etiquetaFin
        );
    }

    private void generarPara(
            ParaAst para
    ) {
        generarSentencia(
                para.inicializacion()
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

        String condicion =
                generarExpresion(
                        para.condicion()
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
                etiquetaActualizacion
        );

        generarBloque(
                para.cuerpo()
        );

        etiquetasContinuar.pop();
        etiquetasRomper.pop();

        etiqueta(
                etiquetaActualizacion
        );

        generarSentencia(
                para.actualizacion()
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

    private void generarMientras(
            MientrasAst mientras
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

        generarBloque(
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
            HacerMientrasAst hacer
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

        generarBloque(
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

    private void generarRomper() {
        if (etiquetasRomper.isEmpty()) {
            throw new IllegalStateException(
                    "No existe contexto para romper"
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
                    "No existe contexto para continuar"
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
            ExpresionAst expresion
    ) {
        if (expresion
                instanceof LiteralAst literal) {

            return literal.lexema();
        }

        if (expresion
                instanceof IdentificadorAst identificador) {

            return identificador.nombre();
        }

        if (expresion
                instanceof BinariaAst binaria) {

            return generarBinaria(binaria);
        }

        if (expresion
                instanceof UnariaAst unaria) {

            return generarUnaria(unaria);
        }

        if (expresion instanceof LeerAst) {
            String temporal =
                    programa.nuevoTemporal();

            agregar(
                    OperadorCuarteta.LEER,
                    "cadena",
                    null,
                    temporal
            );

            return temporal;
        }

        if (expresion
                instanceof LlamadaAst llamada) {

            return generarLlamada(llamada);
        }

        if (expresion
                instanceof AccesoArregloAst
                || expresion
                instanceof AccesoAtributoAst) {

            return leerLugar(expresion);
        }

        if (expresion
                instanceof CambioUnidadExpresionAst cambio) {

            return generarCambioUnidadExpresion(
                    cambio
            );
        }

        if (expresion
                instanceof InicializadorListaAst lista) {

            return serializarLista(lista);
        }

        throw new IllegalStateException(
                "Expresion Y? no soportada para cuartetas: "
                        + expresion.getClass().getSimpleName()
        );
    }

    private String generarBinaria(
            BinariaAst binaria
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

    private OperadorCuarteta operadorBinario(
            BinariaAst.Operador operador
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

    private String generarUnaria(
            UnariaAst unaria
    ) {
        String valor =
                generarExpresion(
                        unaria.expresion()
                );

        if (unaria.operador()
                == UnariaAst.Operador.POSITIVO) {

            return valor;
        }

        String temporal =
                programa.nuevoTemporal();

        OperadorCuarteta operador =
                unaria.operador()
                        == UnariaAst.Operador.NEGACION
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

    private String generarLlamada(
            LlamadaAst llamada
    ) {
        if (!(llamada.objetivo()
                instanceof IdentificadorAst identificador)) {

            throw new IllegalStateException(
                    "Y? solo permite llamadas a funciones globales"
            );
        }

        for (int indice = 0;
             indice < llamada.argumentos().size();
             indice++) {

            String argumento =
                    generarExpresion(
                            llamada.argumentos()
                                    .get(indice)
                    );

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    argumento,
                    String.valueOf(indice),
                    null
            );
        }

        FuncionAst funcion =
                funciones.get(
                        identificador.nombre()
                );

        boolean retorna =
                funcion != null
                        && funcion.tieneRetorno();

        String temporal =
                retorna
                        ? programa.nuevoTemporal()
                        : null;

        agregar(
                OperadorCuarteta.LLAMAR,
                identificador.nombre(),
                String.valueOf(
                        llamada.argumentos().size()
                ),
                temporal
        );

        return temporal == null
                ? "-"
                : temporal;
    }

    private String generarCambioUnidadExpresion(
            CambioUnidadExpresionAst cambio
    ) {
        String lugar =
                generarLugar(
                        cambio.objetivo()
                );

        String valorAnterior =
                leerLugar(
                        cambio.objetivo()
                );

        String temporalNuevo =
                programa.nuevoTemporal();

        OperadorCuarteta operador =
                cambio.operacion()
                        == CambioUnidadExpresionAst.Operacion.INCREMENTO
                        ? OperadorCuarteta.SUMAR
                        : OperadorCuarteta.RESTAR;

        agregar(
                operador,
                valorAnterior,
                "1",
                temporalNuevo
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporalNuevo,
                null,
                lugar
        );

        return valorAnterior;
    }

    private String generarLugar(
            ExpresionAst expresion
    ) {
        if (expresion
                instanceof IdentificadorAst identificador) {

            return identificador.nombre();
        }

        if (expresion
                instanceof AccesoAtributoAst acceso) {

            return generarLugar(
                    acceso.objetivo()
            )
                    + "."
                    + acceso.atributo();
        }

        if (expresion
                instanceof AccesoArregloAst acceso) {

            String indice =
                    generarExpresion(
                            acceso.indice()
                    );

            return generarLugar(
                    acceso.objetivo()
            )
                    + "["
                    + indice
                    + "]";
        }

        throw new IllegalStateException(
                "La expresion no es asignable: "
                        + expresion.getClass().getSimpleName()
        );
    }

    private String leerLugar(
            ExpresionAst expresion
    ) {
        String lugar =
                generarLugar(expresion);

        if (expresion instanceof IdentificadorAst) {
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

    private String serializarLista(
            InicializadorListaAst lista
    ) {
        List<String> valores =
                new ArrayList<>();

        for (ExpresionAst valor
                : lista.valores()) {

            if (valor
                    instanceof InicializadorListaAst interna) {

                valores.add(
                        serializarLista(interna)
                );

            } else {
                valores.add(
                        generarExpresion(valor)
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