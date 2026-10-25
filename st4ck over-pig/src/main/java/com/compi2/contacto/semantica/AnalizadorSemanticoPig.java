package com.compi2.contacto.semantica;

import com.compi2.contacto.analisis.ResultadoAnalisisArchivo;
import com.compi2.contacto.ast.AtributoEstructuraAst;
import com.compi2.contacto.ast.EstructuraAst;
import com.compi2.contacto.ast.ExpresionAst;
import com.compi2.contacto.ast.FuncionAst;
import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ParametroAst;
import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.TipoAst;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AnalizadorSemanticoPig {

    private final TablaSimbolos tabla;
    private final List<Diagnostico> diagnosticos;
    private final ResolutorImportsPig resolutorImports;
    private final EnlacesPig enlaces;

    private Map<String, EstructuraAst> estructuras;
    private Map<String, FuncionAst> funciones;
    private Map<String, ZAst.Clase> clases;

    private int profundidadCiclo;
    private int contadorAmbitos;

    public AnalizadorSemanticoPig() {
        tabla = new TablaSimbolos();
        diagnosticos = new ArrayList<>();
        resolutorImports = new ResolutorImportsPig();
        enlaces = new EnlacesPig();

        estructuras = Map.of();
        funciones = Map.of();
        clases = Map.of();
    }

    public ResultadoSemanticoPig analizar(
            ProyectoCompilacion proyecto,
            List<ResultadoAnalisisArchivo> resultados
    ) {
        Objects.requireNonNull(
                proyecto,
                "El proyecto es obligatorio"
        );

        Objects.requireNonNull(
                resultados,
                "Los resultados son obligatorios"
        );

        for (ResultadoAnalisisArchivo resultado
                : resultados) {

            if (resultado.archivo()
                    .lenguaje()
                    != LenguajeFuente.PIG_LATIN) {
                continue;
            }

            if (!resultado.esValido()
                    || resultado.ast()
                    .isEmpty()) {
                continue;
            }

            analizarArchivoPig(
                    proyecto,
                    resultado,
                    resultados
            );
        }

        return new ResultadoSemanticoPig(
                tabla,
                diagnosticos,
                enlaces
        );
    }

    private void analizarArchivoPig(
            ProyectoCompilacion proyecto,
            ResultadoAnalisisArchivo resultado,
            List<ResultadoAnalisisArchivo> resultados
    ) {
        ProgramaAst ast =
                resultado.ast()
                        .orElseThrow();

        if (ast.elementos().isEmpty()
                || !(ast.elementos().get(0)
                instanceof PAst.Programa programa)) {
            return;
        }

        ResultadoImportsPig imports =
                resolutorImports.resolver(
                        proyecto,
                        programa,
                        resultados
                );

        diagnosticos.addAll(
                imports.diagnosticos()
        );

        estructuras =
                new LinkedHashMap<>(
                        imports.estructurasY()
                );

        funciones =
                new LinkedHashMap<>(
                        imports.funcionesY()
                );

        clases =
                new LinkedHashMap<>(
                        imports.clasesZ()
                );

        tabla.entrar(
                "pig:"
                        + resultado.archivo()
                        .nombre()
        );

        declararImportados();
        declararGlobales(
                programa.globales()
        );

        tabla.entrar(
                "MAIOR:"
                        + contadorAmbitos++
        );

        analizarSentencias(
                programa.principal()
        );

        tabla.salir();
        tabla.salir();
    }

    private void declararImportados() {
        for (EstructuraAst estructura
                : estructuras.values()) {

            tabla.declarar(
                    new Simbolo(
                            estructura.nombre(),
                            CategoriaSimbolo.ESTRUCTURA,
                            TipoDato.ESTRUCTURA,
                            estructura.nombre(),
                            estructura.posicion()
                    )
            );
        }

        for (FuncionAst funcion
                : funciones.values()) {

            TipoPig retorno =
                    funcion.retorno()
                            .map(this::resolverTipoY)
                            .orElse(
                                    TipoPig.voidTipo()
                            );

            tabla.declarar(
                    new Simbolo(
                            funcion.nombre(),
                            CategoriaSimbolo.FUNCION,
                            convertirTipo(retorno),
                            retorno.declarado(),
                            funcion.posicion()
                    )
            );
        }

        for (ZAst.Clase clase
                : clases.values()) {

            tabla.declarar(
                    new Simbolo(
                            clase.nombre(),
                            CategoriaSimbolo.CLASE,
                            TipoDato.OBJETO,
                            clase.nombre(),
                            clase.posicion()
                    )
            );
        }
    }

    private void declararGlobales(
            List<PAst.Declaracion> globales
    ) {
        for (PAst.Declaracion declaracion
                : globales) {

            analizarDeclaracion(
                    declaracion
            );
        }
    }

    private void analizarSentencias(
            List<PAst.Sentencia> sentencias
    ) {
        for (PAst.Sentencia sentencia
                : sentencias) {

            analizarSentencia(
                    sentencia
            );
        }
    }

    private void analizarSentencia(
            PAst.Sentencia sentencia
    ) {
        if (sentencia
                instanceof PAst.Declaracion declaracion) {

            analizarDeclaracion(
                    declaracion
            );

            return;
        }

        if (sentencia
                instanceof PAst.ExpresionSentencia expresion) {

            resolverExpresion(
                    expresion.expresion()
            );

            return;
        }

        if (sentencia
                instanceof PAst.Entrada entrada) {

            analizarEntrada(
                    entrada
            );

            return;
        }

        if (sentencia
                instanceof PAst.Salida salida) {

            analizarSalida(
                    salida
            );

            return;
        }

        if (sentencia
                instanceof PAst.Si si) {

            analizarSi(
                    si
            );

            return;
        }

        if (sentencia
                instanceof PAst.Mientras mientras) {

            analizarMientras(
                    mientras
            );

            return;
        }

        if (sentencia
                instanceof PAst.HacerMientras hacer) {

            analizarHacerMientras(
                    hacer
            );

            return;
        }

        if (sentencia
                instanceof PAst.Para para) {

            analizarPara(
                    para
            );

            return;
        }

        if (sentencia
                instanceof PAst.Romper romper) {

            if (profundidadCiclo == 0) {
                error(
                        romper.posicion(),
                        "interrumpe solo puede utilizarse dentro de un ciclo"
                );
            }

            return;
        }

        if (sentencia
                instanceof PAst.Continuar continuar) {

            if (profundidadCiclo == 0) {
                error(
                        continuar.posicion(),
                        "perge solo puede utilizarse dentro de un ciclo"
                );
            }
        }
    }

    private void analizarDeclaracion(
            PAst.Declaracion declaracion
    ) {
        TipoPig tipo =
                resolverTipo(
                        declaracion.tipo()
                );

        if (tipo.esDesconocido()) {
            error(
                    declaracion.posicion(),
                    "El tipo '"
                            + declaracion.tipo()
                            .nombreCompleto()
                            + "' no existe o no fue importado"
            );
        }

        for (PAst.Expresion dimension
                : declaracion.dimensiones()) {

            TipoPig tipoDimension =
                    resolverExpresion(
                            dimension
                    );

            if (!tipoDimension.esEntero()
                    && !tipoDimension.esDesconocido()) {

                error(
                        dimension.posicion(),
                        "Las dimensiones de un arreglo deben ser de tipo numerus"
                );
            }
        }

        Simbolo simbolo =
                new Simbolo(
                        declaracion.nombre(),
                        CategoriaSimbolo.VARIABLE,
                        convertirTipo(tipo),
                        tipo.declarado(),
                        declaracion.posicion()
                );

        if (!tabla.declarar(
                simbolo
        )) {
            error(
                    declaracion.posicion(),
                    "El identificador '"
                            + declaracion.nombre()
                            + "' ya fue declarado en este ambito"
            );
        }

        declaracion.inicializador()
                .ifPresent(
                        inicializador ->
                                validarInicializador(
                                        tipo,
                                        inicializador,
                                        declaracion.posicion()
                                )
                );
    }

    private void analizarEntrada(
            PAst.Entrada entrada
    ) {
        if (entrada.destino()
                .isEmpty()) {

            return;
        }

        PAst.Expresion destino =
                entrada.destino()
                        .orElseThrow();

        exigirAsignable(
                destino,
                entrada.posicion()
        );

        TipoPig tipo =
                resolverExpresion(
                        destino
                );

        if (tipo.esArreglo()
                || tipo.clase()
                == ClaseTipo.ESTRUCTURA
                || tipo.clase()
                == ClaseTipo.OBJETO
                || tipo.esVoid()) {

            if (!tipo.esDesconocido()) {
                error(
                        entrada.posicion(),
                        "La entrada << requiere una variable de tipo primitivo"
                );
            }

            return;
        }

        if (!tipo.esDesconocido()) {
            enlaces.registrarTipoEntrada(
                    entrada,
                    tipo.nombreBase()
            );
        }
    }

    private void analizarSalida(
            PAst.Salida salida
    ) {
        for (PAst.Expresion expresion
                : salida.expresiones()) {

            TipoPig tipo =
                    resolverExpresion(
                            expresion
                    );

            if (tipo.esArreglo()
                    || tipo.clase()
                    == ClaseTipo.ESTRUCTURA
                    || tipo.clase()
                    == ClaseTipo.OBJETO
                    || tipo.esVoid()) {

                if (!tipo.esDesconocido()) {
                    error(
                            expresion.posicion(),
                            "La salida >> requiere valores de tipo primitivo"
                    );
                }
            }
        }
    }

    private void analizarSi(
            PAst.Si si
    ) {
        boolean encontroContrario = false;

        for (int indice = 0;
             indice < si.ramas().size();
             indice++) {

            PAst.RamaSi rama =
                    si.ramas().get(indice);

            if (rama.condicion()
                    .isEmpty()) {

                if (encontroContrario
                        || indice
                        != si.ramas().size() - 1) {

                    error(
                            rama.posicion(),
                            "La rama aliter sin condicion debe ser la ultima"
                    );
                }

                encontroContrario = true;

            } else {

                TipoPig condicion =
                        resolverExpresion(
                                rama.condicion()
                                        .orElseThrow()
                        );

                exigirBooleano(
                        condicion,
                        rama.posicion(),
                        "La condicion de si/aliter debe ser booleana"
                );
            }

            tabla.entrar(
                    "rama:"
                            + contadorAmbitos++
            );

            analizarSentencias(
                    rama.cuerpo()
            );

            tabla.salir();
        }
    }

    private void analizarMientras(
            PAst.Mientras mientras
    ) {
        TipoPig condicion =
                resolverExpresion(
                        mientras.condicion()
                );

        exigirBooleano(
                condicion,
                mientras.condicion()
                        .posicion(),
                "La condicion de dum debe ser booleana"
        );

        profundidadCiclo++;

        tabla.entrar(
                "dum:"
                        + contadorAmbitos++
        );

        analizarSentencias(
                mientras.cuerpo()
        );

        tabla.salir();

        profundidadCiclo--;
    }

    private void analizarHacerMientras(
            PAst.HacerMientras hacer
    ) {
        profundidadCiclo++;

        tabla.entrar(
                "facere:"
                        + contadorAmbitos++
        );

        analizarSentencias(
                hacer.cuerpo()
        );

        tabla.salir();

        profundidadCiclo--;

        TipoPig condicion =
                resolverExpresion(
                        hacer.condicion()
                );

        exigirBooleano(
                condicion,
                hacer.condicion()
                        .posicion(),
                "La condicion de facere-dum debe ser booleana"
        );
    }

    private void analizarPara(
            PAst.Para para
    ) {
        tabla.entrar(
                "per:"
                        + contadorAmbitos++
        );

        if (para.inicializacion()
                .isPresent()) {

            NodoAst inicializacion =
                    para.inicializacion()
                            .orElseThrow();

            if (inicializacion
                    instanceof PAst.Declaracion declaracion) {

                analizarDeclaracion(
                        declaracion
                );

            } else if (inicializacion
                    instanceof PAst.Expresion expresion) {

                resolverExpresion(
                        expresion
                );
            }
        }

        para.condicion()
                .ifPresent(
                        condicion -> {

                            TipoPig tipo =
                                    resolverExpresion(
                                            condicion
                                    );

                            exigirBooleano(
                                    tipo,
                                    condicion.posicion(),
                                    "La condicion de per debe ser booleana"
                            );
                        }
                );

        profundidadCiclo++;

        tabla.entrar(
                "cuerpo-per:"
                        + contadorAmbitos++
        );

        analizarSentencias(
                para.cuerpo()
        );

        tabla.salir();

        para.actualizacion()
                .ifPresent(
                        this::resolverExpresion
                );

        profundidadCiclo--;

        tabla.salir();
    }

    private void validarInicializador(
            TipoPig esperado,
            PAst.Inicializador inicializador,
            PosicionFuente posicion
    ) {
        if (inicializador
                instanceof PAst.InicializadorLista lista) {

            validarLista(
                    esperado,
                    lista
            );

            return;
        }

        TipoPig recibido =
                resolverExpresion(
                        (PAst.Expresion) inicializador
                );

        if (!esCompatible(
                esperado,
                recibido
        )) {
            error(
                    posicion,
                    "No se puede asignar un valor de tipo '"
                            + recibido.declarado()
                            + "' a un valor de tipo '"
                            + esperado.declarado()
                            + "'"
            );
        }
    }

    private void validarLista(
            TipoPig esperado,
            PAst.InicializadorLista lista
    ) {
        if (esperado.esArreglo()) {

            TipoPig elemento =
                    esperado.reducirArreglo();

            for (PAst.Inicializador valor
                    : lista.valores()) {

                validarInicializador(
                        elemento,
                        valor,
                        valor.posicion()
                );
            }

            return;
        }

        if (esperado.clase()
                == ClaseTipo.ESTRUCTURA) {

            EstructuraAst estructura =
                    estructuras.get(
                            esperado.nombreBase()
                    );

            if (estructura == null) {
                error(
                        lista.posicion(),
                        "La estructura '"
                                + esperado.nombreBase()
                                + "' no fue importada"
                );

                return;
            }

            if (estructura.atributos()
                    .size()
                    != lista.valores()
                    .size()) {

                error(
                        lista.posicion(),
                        "La estructura '"
                                + estructura.nombre()
                                + "' requiere "
                                + estructura.atributos()
                                .size()
                                + " valores pero se recibieron "
                                + lista.valores()
                                .size()
                );
            }

            int cantidad =
                    Math.min(
                            estructura.atributos()
                                    .size(),
                            lista.valores()
                                    .size()
                    );

            for (int indice = 0;
                 indice < cantidad;
                 indice++) {

                AtributoEstructuraAst atributo =
                        estructura.atributos()
                                .get(indice);

                TipoPig tipoAtributo =
                        resolverTipoY(
                                atributo.tipo(),
                                atributo.dimensiones()
                                        .size()
                        );

                PAst.Inicializador valor =
                        lista.valores()
                                .get(indice);

                validarInicializador(
                        tipoAtributo,
                        valor,
                        valor.posicion()
                );
            }

            return;
        }

        error(
                lista.posicion(),
                "Un inicializador con llaves solo puede utilizarse con arreglos o estructuras Y?"
        );
    }

    private TipoPig resolverExpresion(
            PAst.Expresion expresion
    ) {
        if (expresion
                instanceof PAst.Literal literal) {

            return resolverLiteral(
                    literal
            );
        }

        if (expresion
                instanceof PAst.Identificador identificador) {

            return resolverIdentificador(
                    identificador
            );
        }

        if (expresion
                instanceof PAst.Binaria binaria) {

            return resolverBinaria(
                    binaria
            );
        }

        if (expresion
                instanceof PAst.Unaria unaria) {

            return resolverUnaria(
                    unaria
            );
        }

        if (expresion
                instanceof PAst.Asignacion asignacion) {

            return resolverAsignacion(
                    asignacion
            );
        }

        if (expresion
                instanceof PAst.Llamada llamada) {

            return resolverLlamada(
                    llamada
            );
        }

        if (expresion
                instanceof PAst.AccesoArreglo acceso) {

            return resolverAccesoArreglo(
                    acceso
            );
        }

        if (expresion
                instanceof PAst.AccesoMiembro acceso) {

            return resolverAccesoMiembro(
                    acceso
            );
        }

        if (expresion
                instanceof PAst.CambioPostfijo cambio) {

            return resolverCambioPostfijo(
                    cambio
            );
        }

        if (expresion
                instanceof PAst.NuevoObjeto nuevo) {

            return resolverNuevoObjeto(
                    nuevo
            );
        }

        return TipoPig.desconocido();
    }

    private TipoPig resolverLiteral(
            PAst.Literal literal
    ) {
        return switch (literal.tipo()) {

            case ENTERO ->
                    TipoPig.primitivo(
                            ClaseTipo.ENTERO,
                            "numerus"
                    );

            case DECIMAL ->
                    TipoPig.primitivo(
                            ClaseTipo.DECIMAL,
                            "decimalis"
                    );

            case CADENA ->
                    TipoPig.primitivo(
                            ClaseTipo.CADENA,
                            "textum"
                    );

            case CARACTER ->
                    TipoPig.primitivo(
                            ClaseTipo.CARACTER,
                            "littera"
                    );

            case BOOLEANO ->
                    TipoPig.primitivo(
                            ClaseTipo.BOOLEANO,
                            "bool"
                    );

            case NULO ->
                    TipoPig.primitivo(
                            ClaseTipo.NULO,
                            "null"
                    );
        };
    }

    private TipoPig resolverIdentificador(
            PAst.Identificador identificador
    ) {
        Optional<Simbolo> simbolo =
                tabla.buscar(
                        identificador.nombre()
                );

        if (simbolo.isEmpty()) {
            error(
                    identificador.posicion(),
                    "El identificador '"
                            + identificador.nombre()
                            + "' no ha sido declarado"
            );

            return TipoPig.desconocido();
        }

        Simbolo encontrado =
                simbolo.orElseThrow();

        if (encontrado.categoria()
                == CategoriaSimbolo.FUNCION
                || encontrado.categoria()
                == CategoriaSimbolo.ESTRUCTURA
                || encontrado.categoria()
                == CategoriaSimbolo.CLASE) {

            error(
                    identificador.posicion(),
                    "El identificador '"
                            + identificador.nombre()
                            + "' no puede utilizarse directamente como valor"
            );

            return TipoPig.desconocido();
        }

        return tipoDesdeDeclarado(
                encontrado.tipoDeclarado()
        );
    }

    private TipoPig resolverBinaria(
            PAst.Binaria binaria
    ) {
        TipoPig izquierda =
                resolverExpresion(
                        binaria.izquierda()
                );

        TipoPig derecha =
                resolverExpresion(
                        binaria.derecha()
                );

        return switch (binaria.operador()) {

            case SUMA ->
                    resolverSuma(
                            izquierda,
                            derecha,
                            binaria.posicion()
                    );

            case RESTA,
                 MULTIPLICACION,
                 DIVISION ->
                    resolverAritmetica(
                            izquierda,
                            derecha,
                            binaria.posicion()
                    );

            case MENOR,
                 MAYOR,
                 MENOR_IGUAL,
                 MAYOR_IGUAL -> {

                if (!esNumerico(
                        izquierda
                )
                        || !esNumerico(
                        derecha
                )) {

                    if (!izquierda.esDesconocido()
                            && !derecha.esDesconocido()) {

                        error(
                                binaria.posicion(),
                                "Los operadores relacionales requieren valores numericos"
                        );
                    }
                }

                yield TipoPig.primitivo(
                        ClaseTipo.BOOLEANO,
                        "bool"
                );
            }

            case IGUALDAD,
                 DIFERENTE -> {

                if (!sonComparables(
                        izquierda,
                        derecha
                )) {
                    error(
                            binaria.posicion(),
                            "Los valores comparados no son compatibles"
                    );
                }

                yield TipoPig.primitivo(
                        ClaseTipo.BOOLEANO,
                        "bool"
                );
            }

            case AND,
                 OR -> {

                if (!izquierda.esBooleano()
                        || !derecha.esBooleano()) {

                    if (!izquierda.esDesconocido()
                            && !derecha.esDesconocido()) {

                        error(
                                binaria.posicion(),
                                "Los operadores logicos requieren valores booleanos"
                        );
                    }
                }

                yield TipoPig.primitivo(
                        ClaseTipo.BOOLEANO,
                        "bool"
                );
            }
        };
    }

    private TipoPig resolverSuma(
            TipoPig izquierda,
            TipoPig derecha,
            PosicionFuente posicion
    ) {
        if (izquierda.clase()
                == ClaseTipo.CADENA
                || derecha.clase()
                == ClaseTipo.CADENA) {

            if (izquierda.esComplejo()
                    || derecha.esComplejo()) {

                error(
                        posicion,
                        "No se puede concatenar directamente una estructura, objeto o arreglo"
                );

                return TipoPig.desconocido();
            }

            return TipoPig.primitivo(
                    ClaseTipo.CADENA,
                    "textum"
            );
        }

        return resolverAritmetica(
                izquierda,
                derecha,
                posicion
        );
    }

    private TipoPig resolverAritmetica(
            TipoPig izquierda,
            TipoPig derecha,
            PosicionFuente posicion
    ) {
        if (!esNumerico(
                izquierda
        )
                || !esNumerico(
                derecha
        )) {

            if (!izquierda.esDesconocido()
                    && !derecha.esDesconocido()) {

                error(
                        posicion,
                        "La operacion aritmetica requiere valores numericos"
                );
            }

            return TipoPig.desconocido();
        }

        if (izquierda.clase()
                == ClaseTipo.DECIMAL
                || derecha.clase()
                == ClaseTipo.DECIMAL) {

            return TipoPig.primitivo(
                    ClaseTipo.DECIMAL,
                    "decimalis"
            );
        }

        return TipoPig.primitivo(
                ClaseTipo.ENTERO,
                "numerus"
        );
    }

    private TipoPig resolverUnaria(
            PAst.Unaria unaria
    ) {
        TipoPig tipo =
                resolverExpresion(
                        unaria.expresion()
                );

        if (unaria.operador()
                == PAst.OperadorUnario.NEGACION) {

            exigirBooleano(
                    tipo,
                    unaria.posicion(),
                    "El operador ! requiere un valor booleano"
            );

            return TipoPig.primitivo(
                    ClaseTipo.BOOLEANO,
                    "bool"
            );
        }

        if (!esNumerico(
                tipo
        )
                && !tipo.esDesconocido()) {

            error(
                    unaria.posicion(),
                    "El operador requiere un valor numerico"
            );
        }

        if (unaria.operador()
                == PAst.OperadorUnario.INCREMENTO_PRE
                || unaria.operador()
                == PAst.OperadorUnario.DECREMENTO_PRE) {

            exigirAsignable(
                    unaria.expresion(),
                    unaria.posicion()
            );
        }

        return tipo;
    }

    private TipoPig resolverAsignacion(
            PAst.Asignacion asignacion
    ) {
        exigirAsignable(
                asignacion.destino(),
                asignacion.posicion()
        );

        TipoPig destino =
                resolverExpresion(
                        asignacion.destino()
                );

        TipoPig valor =
                resolverExpresion(
                        asignacion.valor()
                );

        if (!esCompatible(
                destino,
                valor
        )) {
            error(
                    asignacion.posicion(),
                    "No se puede asignar un valor de tipo '"
                            + valor.declarado()
                            + "' a un valor de tipo '"
                            + destino.declarado()
                            + "'"
            );
        }

        return destino;
    }

    private TipoPig resolverLlamada(
            PAst.Llamada llamada
    ) {
        if (llamada.objetivo()
                instanceof PAst.Identificador identificador) {

            TipoPig resultado =
                    resolverFuncionY(
                            identificador.nombre(),
                            llamada.argumentos(),
                            llamada.posicion()
                    );

            if (funciones.containsKey(
                    identificador.nombre()
            )) {
                enlaces.registrarLlamada(
                        llamada,
                        identificador.nombre(),
                        Optional.empty()
                );
            }

            return resultado;
        }

        if (llamada.objetivo()
                instanceof PAst.AccesoMiembro acceso) {

            TipoPig receptor =
                    resolverExpresion(
                            acceso.objetivo()
                    );

            if (receptor.clase()
                    != ClaseTipo.OBJETO
                    || receptor.esArreglo()) {

                resolverArgumentos(
                        llamada.argumentos()
                );

                if (!receptor.esDesconocido()) {
                    error(
                            llamada.posicion(),
                            "Solo los objetos Zetariano pueden invocar metodos"
                    );
                }

                return TipoPig.desconocido();
            }

            ZAst.Clase clase =
                    clases.get(
                            receptor.nombreBase()
                    );

            if (clase == null) {
                resolverArgumentos(
                        llamada.argumentos()
                );

                error(
                        llamada.posicion(),
                        "La clase '"
                                + receptor.nombreBase()
                                + "' no fue importada"
                );

                return TipoPig.desconocido();
            }

            return resolverMetodoZ(
                    clase,
                    acceso.miembro(),
                    llamada,
                    acceso.objetivo()
            );
        }

        resolverArgumentos(
                llamada.argumentos()
        );

        error(
                llamada.posicion(),
                "La expresion no puede utilizarse como llamada"
        );

        return TipoPig.desconocido();
    }

    private TipoPig resolverFuncionY(
            String nombre,
            List<PAst.Expresion> argumentos,
            PosicionFuente posicion
    ) {
        FuncionAst funcion =
                funciones.get(
                        nombre
                );

        List<TipoPig> tipos =
                resolverArgumentos(
                        argumentos
                );

        if (funcion == null) {
            error(
                    posicion,
                    "La funcion '"
                            + nombre
                            + "' no existe o no fue importada desde un archivo .y"
            );

            return TipoPig.desconocido();
        }

        if (funcion.parametros()
                .size()
                != tipos.size()) {

            error(
                    posicion,
                    "La funcion '"
                            + nombre
                            + "' requiere "
                            + funcion.parametros()
                            .size()
                            + " parametros pero se recibieron "
                            + tipos.size()
            );
        }

        int cantidad =
                Math.min(
                        funcion.parametros()
                                .size(),
                        tipos.size()
                );

        for (int indice = 0;
             indice < cantidad;
             indice++) {

            ParametroAst parametro =
                    funcion.parametros()
                            .get(indice);

            TipoPig recibido =
                    tipos.get(indice);

            validarParametroY(
                    parametro,
                    recibido,
                    argumentos.get(indice)
                            .posicion()
            );
        }

        return funcion.retorno()
                .map(this::resolverTipoY)
                .orElse(
                        TipoPig.voidTipo()
                );
    }

    private void validarParametroY(
            ParametroAst parametro,
            TipoPig recibido,
            PosicionFuente posicion
    ) {
        TipoPig base =
                resolverTipoY(
                        parametro.tipo()
                );

        if (parametro.modo()
                == ParametroAst.Modo.REFERENCIA_ARREGLO) {

            if (!recibido.esArreglo()
                    || !recibido.nombreBase()
                    .equals(
                            base.nombreBase()
                    )) {

                error(
                        posicion,
                        "El parametro '"
                                + parametro.nombre()
                                + "' requiere un arreglo de tipo '"
                                + base.declarado()
                                + "'"
                );
            }

            return;
        }

        if (parametro.modo()
                == ParametroAst.Modo.REFERENCIA_ESTRUCTURA) {

            if (recibido.clase()
                    != ClaseTipo.ESTRUCTURA
                    || !recibido.nombreBase()
                    .equals(
                            base.nombreBase()
                    )) {

                error(
                        posicion,
                        "El parametro '"
                                + parametro.nombre()
                                + "' requiere la estructura '"
                                + base.declarado()
                                + "'"
                );
            }

            return;
        }

        if (!esCompatible(
                base,
                recibido
        )) {
            error(
                    posicion,
                    "El parametro '"
                            + parametro.nombre()
                            + "' requiere '"
                            + base.declarado()
                            + "' pero se recibio '"
                            + recibido.declarado()
                            + "'"
            );
        }
    }

    private TipoPig resolverMetodoZ(
            ZAst.Clase clase,
            String nombre,
            PAst.Llamada llamada,
            PAst.Expresion receptor
    ) {
        List<TipoPig> tipos =
                resolverArgumentos(
                        llamada.argumentos()
                );

        ZAst.Clase declaradora = clase;
        List<ZAst.Metodo> candidatos = List.of();
        Set<String> visitadas = new HashSet<>();

        while (declaradora != null
                && visitadas.add(declaradora.nombre())) {
            candidatos = declaradora.miembros().stream()
                    .filter(ZAst.Metodo.class::isInstance)
                    .map(ZAst.Metodo.class::cast)
                    .filter(metodo -> metodo.nombre().equals(nombre))
                    .toList();
            if (!candidatos.isEmpty()) {
                break;
            }
            declaradora = declaradora.padre()
                    .map(clases::get)
                    .orElse(null);
        }

        if (candidatos.isEmpty()) {
            error(
                    llamada.posicion(),
                    "La clase '"
                            + clase.nombre()
                            + "' no contiene el metodo '"
                            + nombre
                            + "'"
            );

            return TipoPig.desconocido();
        }

        ZAst.Metodo seleccionado =
                seleccionarMetodo(
                        candidatos,
                        tipos
                );

        if (seleccionado == null) {
            error(
                    llamada.posicion(),
                    "No existe una sobrecarga compatible de '"
                            + nombre
                            + "'"
            );

            return TipoPig.desconocido();
        }

        enlaces.registrarLlamada(
                llamada,
                nombreMetodoZ(
                        declaradora,
                        seleccionado
                ),
                Optional.of(
                        receptor
                )
        );

        return seleccionado.retorno()
                .map(
                        this::resolverTipoZ
                )
                .orElse(
                        TipoPig.voidTipo()
                );
    }

    private TipoPig resolverAccesoArreglo(
            PAst.AccesoArreglo acceso
    ) {
        TipoPig objetivo =
                resolverExpresion(
                        acceso.objetivo()
                );

        TipoPig indice =
                resolverExpresion(
                        acceso.indice()
                );

        if (!indice.esEntero()
                && !indice.esDesconocido()) {

            error(
                    acceso.indice()
                            .posicion(),
                    "El indice de un arreglo debe ser numerus"
            );
        }

        if (!objetivo.esArreglo()) {

            if (!objetivo.esDesconocido()) {
                error(
                        acceso.posicion(),
                        "Se intento indexar un valor que no es un arreglo"
                );
            }

            return TipoPig.desconocido();
        }

        return objetivo.reducirArreglo();
    }

    private TipoPig resolverAccesoMiembro(
            PAst.AccesoMiembro acceso
    ) {
        TipoPig objetivo =
                resolverExpresion(
                        acceso.objetivo()
                );

        if (objetivo.clase()
                == ClaseTipo.ESTRUCTURA
                && !objetivo.esArreglo()) {

            return resolverAtributoEstructura(
                    objetivo,
                    acceso
            );
        }

        if (objetivo.clase()
                == ClaseTipo.OBJETO
                && !objetivo.esArreglo()) {

            return resolverMiembroObjeto(
                    objetivo,
                    acceso
            );
        }

        if (!objetivo.esDesconocido()) {
            error(
                    acceso.posicion(),
                    "Solo una estructura Y? o un objeto Zetariano puede acceder a miembros"
            );
        }

        return TipoPig.desconocido();
    }

    private TipoPig resolverAtributoEstructura(
            TipoPig objetivo,
            PAst.AccesoMiembro acceso
    ) {
        EstructuraAst estructura =
                estructuras.get(
                        objetivo.nombreBase()
                );

        if (estructura == null) {
            return TipoPig.desconocido();
        }

        for (AtributoEstructuraAst atributo
                : estructura.atributos()) {

            if (atributo.nombre()
                    .equals(
                            acceso.miembro()
                    )) {

                return resolverTipoY(
                        atributo.tipo(),
                        atributo.dimensiones()
                                .size()
                );
            }
        }

        error(
                acceso.posicion(),
                "La estructura '"
                        + estructura.nombre()
                        + "' no contiene el atributo '"
                        + acceso.miembro()
                        + "'"
        );

        return TipoPig.desconocido();
    }

    private TipoPig resolverMiembroObjeto(
            TipoPig objetivo,
            PAst.AccesoMiembro acceso
    ) {
        ZAst.Clase clase =
                clases.get(
                        objetivo.nombreBase()
                );

        if (clase == null) {
            return TipoPig.desconocido();
        }

        for (ZAst.Miembro miembro
                : clase.miembros()) {

            if (miembro
                    instanceof ZAst.Atributo atributo
                    && atributo.nombre()
                    .equals(
                            acceso.miembro()
                    )) {

                return resolverTipoZ(
                        atributo.tipo()
                );
            }
        }

        boolean metodo =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Metodo.class::isInstance
                        )
                        .map(
                                ZAst.Metodo.class::cast
                        )
                        .anyMatch(
                                actual ->
                                        actual.nombre()
                                                .equals(
                                                        acceso.miembro()
                                                )
                        );

        if (metodo) {
            error(
                    acceso.posicion(),
                    "El metodo '"
                            + acceso.miembro()
                            + "' debe invocarse con parentesis"
            );

            return TipoPig.desconocido();
        }

        error(
                acceso.posicion(),
                "La clase '"
                        + clase.nombre()
                        + "' no contiene el miembro '"
                        + acceso.miembro()
                        + "'"
        );

        return TipoPig.desconocido();
    }

    private TipoPig resolverCambioPostfijo(
            PAst.CambioPostfijo cambio
    ) {
        exigirAsignable(
                cambio.objetivo(),
                cambio.posicion()
        );

        TipoPig tipo =
                resolverExpresion(
                        cambio.objetivo()
                );

        if (!esNumerico(
                tipo
        )
                && !tipo.esDesconocido()) {

            error(
                    cambio.posicion(),
                    "Los operadores ++ y -- requieren un valor numerico"
            );
        }

        return tipo;
    }

    private TipoPig resolverNuevoObjeto(
            PAst.NuevoObjeto nuevo
    ) {
        ZAst.Clase clase =
                clases.get(
                        nuevo.tipo()
                );

        List<TipoPig> argumentos =
                resolverArgumentos(
                        nuevo.argumentos()
                );

        if (clase == null) {
            error(
                    nuevo.posicion(),
                    "La clase '"
                            + nuevo.tipo()
                            + "' no existe o no fue importada desde un archivo .z"
            );

            return TipoPig.desconocido();
        }

        List<ZAst.Constructor> constructores =
                clase.miembros()
                        .stream()
                        .filter(
                                ZAst.Constructor.class::isInstance
                        )
                        .map(
                                ZAst.Constructor.class::cast
                        )
                        .toList();

        if (constructores.isEmpty()
                && argumentos.isEmpty()) {

            enlaces.registrarConstructor(
                    nuevo,
                    clase.nombre()
                            + ".<init>()",
                    clase.nombre()
            );

            return TipoPig.objeto(
                    clase.nombre()
            );
        }

        ZAst.Constructor seleccionado =
                seleccionarConstructor(
                        constructores,
                        argumentos
                );

        if (seleccionado == null) {
            error(
                    nuevo.posicion(),
                    "No existe un constructor compatible para la clase '"
                            + clase.nombre()
                            + "'"
            );

            return TipoPig.objeto(
                    clase.nombre()
            );
        }

        enlaces.registrarConstructor(
                nuevo,
                nombreConstructorZ(
                        clase,
                        seleccionado
                ),
                clase.nombre()
        );

        return TipoPig.objeto(
                clase.nombre()
        );
    }

    private ZAst.Metodo seleccionarMetodo(
            List<ZAst.Metodo> candidatos,
            List<TipoPig> argumentos
    ) {
        ZAst.Metodo mejor = null;
        int mejorPuntaje =
                Integer.MAX_VALUE;

        boolean ambiguo = false;

        for (ZAst.Metodo metodo
                : candidatos) {

            int puntaje =
                    puntajeParametrosZ(
                            metodo.parametros(),
                            argumentos
                    );

            if (puntaje < 0) {
                continue;
            }

            if (puntaje < mejorPuntaje) {
                mejor = metodo;
                mejorPuntaje = puntaje;
                ambiguo = false;

            } else if (puntaje
                    == mejorPuntaje) {

                ambiguo = true;
            }
        }

        return ambiguo
                ? null
                : mejor;
    }

    private ZAst.Constructor seleccionarConstructor(
            List<ZAst.Constructor> candidatos,
            List<TipoPig> argumentos
    ) {
        ZAst.Constructor mejor = null;
        int mejorPuntaje =
                Integer.MAX_VALUE;

        boolean ambiguo = false;

        for (ZAst.Constructor constructor
                : candidatos) {

            int puntaje =
                    puntajeParametrosZ(
                            constructor.parametros(),
                            argumentos
                    );

            if (puntaje < 0) {
                continue;
            }

            if (puntaje < mejorPuntaje) {
                mejor = constructor;
                mejorPuntaje = puntaje;
                ambiguo = false;

            } else if (puntaje
                    == mejorPuntaje) {

                ambiguo = true;
            }
        }

        return ambiguo
                ? null
                : mejor;
    }

    private int puntajeParametrosZ(
            List<ZAst.Parametro> parametros,
            List<TipoPig> argumentos
    ) {
        if (parametros.size()
                != argumentos.size()) {

            return -1;
        }

        int total = 0;

        for (int indice = 0;
             indice < parametros.size();
             indice++) {

            TipoPig esperado =
                    resolverTipoZ(
                            parametros.get(indice)
                                    .tipo()
                    );

            int puntaje =
                    puntajeConversion(
                            esperado,
                            argumentos.get(indice)
                    );

            if (puntaje < 0) {
                return -1;
            }

            total += puntaje;
        }

        return total;
    }

    private List<TipoPig> resolverArgumentos(
            List<PAst.Expresion> argumentos
    ) {
        return argumentos.stream()
                .map(
                        this::resolverExpresion
                )
                .toList();
    }

    private TipoPig resolverTipo(
            PAst.Tipo tipo
    ) {
        TipoPig base =
                resolverTipoBase(
                        tipo.nombreBase()
                );

        if (base.esDesconocido()) {
            return base;
        }

        return new TipoPig(
                base.clase(),
                base.nombreBase(),
                tipo.dimensiones()
        );
    }

    private TipoPig resolverTipoBase(
            String nombre
    ) {
        return switch (nombre) {

            case "numerus" ->
                    TipoPig.primitivo(
                            ClaseTipo.ENTERO,
                            "numerus"
                    );

            case "decimalis" ->
                    TipoPig.primitivo(
                            ClaseTipo.DECIMAL,
                            "decimalis"
                    );

            case "textum" ->
                    TipoPig.primitivo(
                            ClaseTipo.CADENA,
                            "textum"
                    );

            case "littera" ->
                    TipoPig.primitivo(
                            ClaseTipo.CARACTER,
                            "littera"
                    );

            case "bool" ->
                    TipoPig.primitivo(
                            ClaseTipo.BOOLEANO,
                            "bool"
                    );

            default -> {

                if (estructuras.containsKey(
                        nombre
                )) {
                    yield TipoPig.estructura(
                            nombre
                    );
                }

                if (clases.containsKey(
                        nombre
                )) {
                    yield TipoPig.objeto(
                            nombre
                    );
                }

                yield TipoPig.desconocido(
                        nombre
                );
            }
        };
    }

    private TipoPig resolverTipoY(
            TipoAst tipo
    ) {
        return resolverTipoY(
                tipo,
                0
        );
    }

    private TipoPig resolverTipoY(
            TipoAst tipo,
            int dimensiones
    ) {
        TipoPig base =
                switch (tipo.nombre()) {

                    case "entero" ->
                            TipoPig.primitivo(
                                    ClaseTipo.ENTERO,
                                    "numerus"
                            );

                    case "flotante" ->
                            TipoPig.primitivo(
                                    ClaseTipo.DECIMAL,
                                    "decimalis"
                            );

                    case "cadena" ->
                            TipoPig.primitivo(
                                    ClaseTipo.CADENA,
                                    "textum"
                            );

                    case "caracter" ->
                            TipoPig.primitivo(
                                    ClaseTipo.CARACTER,
                                    "littera"
                            );

                    case "bool" ->
                            TipoPig.primitivo(
                                    ClaseTipo.BOOLEANO,
                                    "bool"
                            );

                    default -> {

                        if (estructuras.containsKey(
                                tipo.nombre()
                        )) {
                            yield TipoPig.estructura(
                                    tipo.nombre()
                            );
                        }

                        yield TipoPig.desconocido(
                                tipo.nombre()
                        );
                    }
                };

        if (base.esDesconocido()) {
            return base;
        }

        return new TipoPig(
                base.clase(),
                base.nombreBase(),
                dimensiones
        );
    }

    private TipoPig resolverTipoZ(
            ZAst.Tipo tipo
    ) {
        TipoPig base =
                switch (tipo.nombreBase()) {

                    case "int" ->
                            TipoPig.primitivo(
                                    ClaseTipo.ENTERO,
                                    "numerus"
                            );

                    case "double" ->
                            TipoPig.primitivo(
                                    ClaseTipo.DECIMAL,
                                    "decimalis"
                            );

                    case "String" ->
                            TipoPig.primitivo(
                                    ClaseTipo.CADENA,
                                    "textum"
                            );

                    case "char" ->
                            TipoPig.primitivo(
                                    ClaseTipo.CARACTER,
                                    "littera"
                            );

                    case "boolean" ->
                            TipoPig.primitivo(
                                    ClaseTipo.BOOLEANO,
                                    "bool"
                            );

                    default -> {

                        if (clases.containsKey(
                                tipo.nombreBase()
                        )) {
                            yield TipoPig.objeto(
                                    tipo.nombreBase()
                            );
                        }

                        yield TipoPig.desconocido(
                                tipo.nombreBase()
                        );
                    }
                };

        if (base.esDesconocido()) {
            return base;
        }

        return new TipoPig(
                base.clase(),
                base.nombreBase(),
                tipo.dimensiones()
        );
    }

    private TipoPig tipoDesdeDeclarado(
            String declarado
    ) {
        int dimensiones = 0;

        String base =
                declarado;

        while (base.endsWith(
                "[]"
        )) {
            dimensiones++;

            base = base.substring(
                    0,
                    base.length() - 2
            );
        }

        TipoPig tipo =
                resolverTipoBase(
                        base
                );

        if (tipo.esDesconocido()) {
            return tipo;
        }

        return new TipoPig(
                tipo.clase(),
                tipo.nombreBase(),
                dimensiones
        );
    }

    private boolean esCompatible(
            TipoPig esperado,
            TipoPig recibido
    ) {
        return puntajeConversion(
                esperado,
                recibido
        ) >= 0;
    }

    private int puntajeConversion(
            TipoPig esperado,
            TipoPig recibido
    ) {
        if (esperado.esDesconocido()
                || recibido.esDesconocido()) {

            return 100;
        }

        if (esperado.equals(
                recibido
        )) {
            return 0;
        }

        if (esperado.esArreglo()
                || recibido.esArreglo()) {

            return -1;
        }

        if (esperado.clase() == ClaseTipo.OBJETO
                && recibido.clase() == ClaseTipo.NULO) {
            return 1;
        }

        if (esperado.clase() == ClaseTipo.OBJETO
                && recibido.clase() == ClaseTipo.OBJETO) {
            Set<String> visitadas = new HashSet<>();
            String actual = recibido.nombreBase();
            int distancia = 0;
            while (actual != null && visitadas.add(actual)) {
                if (actual.equals(esperado.nombreBase())) {
                    return distancia;
                }
                ZAst.Clase clase = clases.get(actual);
                actual = clase == null ? null : clase.padre().orElse(null);
                distancia++;
            }
            return -1;
        }

        if (esperado.clase()
                == ClaseTipo.ENTERO
                && recibido.clase()
                == ClaseTipo.CARACTER) {

            return 1;
        }

        if (esperado.clase()
                == ClaseTipo.DECIMAL
                && recibido.clase()
                == ClaseTipo.ENTERO) {

            return 1;
        }

        if (esperado.clase()
                == ClaseTipo.DECIMAL
                && recibido.clase()
                == ClaseTipo.CARACTER) {

            return 2;
        }

        return -1;
    }

    private boolean sonComparables(
            TipoPig primero,
            TipoPig segundo
    ) {
        if (primero.esDesconocido()
                || segundo.esDesconocido()) {

            return true;
        }

        if (esNumerico(
                primero
        )
                && esNumerico(
                segundo
        )) {

            return true;
        }

        if (primero.clase() == ClaseTipo.OBJETO
                && segundo.clase() == ClaseTipo.NULO
                || segundo.clase() == ClaseTipo.OBJETO
                && primero.clase() == ClaseTipo.NULO) {
            return true;
        }

        if (primero.clase() == ClaseTipo.OBJETO
                && segundo.clase() == ClaseTipo.OBJETO) {
            return esCompatible(primero, segundo)
                    || esCompatible(segundo, primero);
        }

        return primero.equals(
                segundo
        );
    }

    private boolean esNumerico(
            TipoPig tipo
    ) {
        if (tipo.esArreglo()) {
            return false;
        }

        return tipo.clase()
                == ClaseTipo.ENTERO
                || tipo.clase()
                == ClaseTipo.DECIMAL
                || tipo.clase()
                == ClaseTipo.CARACTER;
    }

    private void exigirBooleano(
            TipoPig tipo,
            PosicionFuente posicion,
            String mensaje
    ) {
        if (!tipo.esBooleano()
                && !tipo.esDesconocido()) {

            error(
                    posicion,
                    mensaje
            );
        }
    }

    private void exigirAsignable(
            PAst.Expresion expresion,
            PosicionFuente posicion
    ) {
        if (expresion
                instanceof PAst.Identificador
                || expresion
                instanceof PAst.AccesoArreglo
                || expresion
                instanceof PAst.AccesoMiembro) {

            return;
        }

        error(
                posicion,
                "El destino debe ser una variable, atributo o posicion de arreglo"
        );
    }

    private TipoDato convertirTipo(
            TipoPig tipo
    ) {
        if (tipo.esArreglo()) {
            return TipoDato.ARREGLO;
        }

        return switch (tipo.clase()) {

            case ENTERO ->
                    TipoDato.ENTERO;

            case DECIMAL ->
                    TipoDato.DECIMAL;

            case CADENA ->
                    TipoDato.CADENA;

            case CARACTER ->
                    TipoDato.CARACTER;

            case BOOLEANO ->
                    TipoDato.BOOLEANO;

            case ESTRUCTURA ->
                    TipoDato.ESTRUCTURA;

            case OBJETO ->
                    TipoDato.OBJETO;

            case VOID ->
                    TipoDato.VOID;

            case DESCONOCIDO, NULO ->
                    TipoDato.DESCONOCIDO;
        };
    }

    private String nombreMetodoZ(
            ZAst.Clase clase,
            ZAst.Metodo metodo
    ) {
        String parametros =
                metodo.parametros()
                        .stream()
                        .map(
                                parametro ->
                                        parametro.tipo()
                                                .nombreCompleto()
                        )
                        .collect(
                                java.util.stream.Collectors.joining(",")
                        );

        return clase.nombre()
                + "."
                + metodo.nombre()
                + "("
                + parametros
                + ")";
    }

    private String nombreConstructorZ(
            ZAst.Clase clase,
            ZAst.Constructor constructor
    ) {
        String parametros =
                constructor.parametros()
                        .stream()
                        .map(
                                parametro ->
                                        parametro.tipo()
                                                .nombreCompleto()
                        )
                        .collect(
                                java.util.stream.Collectors.joining(",")
                        );

        return clase.nombre()
                + ".<init>("
                + parametros
                + ")";
    }

    private void error(
            PosicionFuente posicion,
            String mensaje
    ) {
        diagnosticos.add(
                new Diagnostico(
                        TipoDiagnostico.SEMANTICO,
                        Severidad.ERROR,
                        posicion.archivo(),
                        posicion.linea(),
                        posicion.columna(),
                        mensaje
                )
        );
    }

    private enum ClaseTipo {
        ENTERO,
        DECIMAL,
        CADENA,
        CARACTER,
        BOOLEANO,
        ESTRUCTURA,
        OBJETO,
        VOID,
        DESCONOCIDO,
        NULO
    }

    private record TipoPig(
            ClaseTipo clase,
            String nombreBase,
            int dimensiones
    ) {

        private TipoPig {
            Objects.requireNonNull(
                    clase
            );

            Objects.requireNonNull(
                    nombreBase
            );

            if (dimensiones < 0) {
                throw new IllegalArgumentException(
                        "Dimensiones invalidas"
                );
            }
        }

        private static TipoPig primitivo(
                ClaseTipo clase,
                String nombre
        ) {
            return new TipoPig(
                    clase,
                    nombre,
                    0
            );
        }

        private static TipoPig estructura(
                String nombre
        ) {
            return new TipoPig(
                    ClaseTipo.ESTRUCTURA,
                    nombre,
                    0
            );
        }

        private static TipoPig objeto(
                String nombre
        ) {
            return new TipoPig(
                    ClaseTipo.OBJETO,
                    nombre,
                    0
            );
        }

        private static TipoPig voidTipo() {
            return new TipoPig(
                    ClaseTipo.VOID,
                    "void",
                    0
            );
        }

        private static TipoPig desconocido() {
            return desconocido(
                    "desconocido"
            );
        }

        private static TipoPig desconocido(
                String nombre
        ) {
            return new TipoPig(
                    ClaseTipo.DESCONOCIDO,
                    nombre,
                    0
            );
        }

        private boolean esArreglo() {
            return dimensiones > 0;
        }

        private boolean esEntero() {
            return clase
                    == ClaseTipo.ENTERO
                    && dimensiones == 0;
        }

        private boolean esBooleano() {
            return clase
                    == ClaseTipo.BOOLEANO
                    && dimensiones == 0;
        }

        private boolean esVoid() {
            return clase
                    == ClaseTipo.VOID;
        }

        private boolean esDesconocido() {
            return clase
                    == ClaseTipo.DESCONOCIDO;
        }

        private boolean esComplejo() {
            return esArreglo()
                    || clase
                    == ClaseTipo.ESTRUCTURA
                    || clase
                    == ClaseTipo.OBJETO
                    || clase
                    == ClaseTipo.VOID;
        }

        private String declarado() {
            return nombreBase
                    + "[]".repeat(
                    dimensiones
            );
        }

        private TipoPig reducirArreglo() {
            if (dimensiones <= 0) {
                return desconocido();
            }

            return new TipoPig(
                    clase,
                    nombreBase,
                    dimensiones - 1
            );
        }
    }
}