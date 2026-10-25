package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AnalizadorSemanticoZ {

    private final TablaSimbolos tabla;
    private final List<Diagnostico> diagnosticos;

    private final Map<String, ClaseInfo> clases;
    private final EnlacesZ enlaces;

    private JerarquiaClasesZ jerarquia;
    private ZAst.Llamada superPermitido;
    private ClaseInfo claseActual;
    private TipoZ retornoActual;

    private int profundidadCiclo;
    private int profundidadSwitch;
    private int contadorAmbitos;

    private boolean retornoEncontrado;

    public AnalizadorSemanticoZ() {
        tabla = new TablaSimbolos();
        diagnosticos = new ArrayList<>();
        clases = new LinkedHashMap<>();
        enlaces = new EnlacesZ();

        retornoActual = TipoZ.voidTipo();
    }

    public ResultadoSemanticoZ analizar(
            List<ProgramaAst> programas
    ) {
        Objects.requireNonNull(
                programas,
                "Los programas son obligatorios"
        );

        jerarquia = new JerarquiaClasesZ(programas);
        registrarClases(programas);

        diagnosticos.addAll(
                new ValidadorHerenciaZ().validar(programas)
        );

        if (diagnosticos.stream().anyMatch(Diagnostico::esError)) {
            return new ResultadoSemanticoZ(tabla, diagnosticos);
        }
        registrarMiembros();
        for (ClaseInfo clase : clases.values()) {
            analizarClase(clase);
        }

        return new ResultadoSemanticoZ(
                tabla,
                diagnosticos
        );
    }

    public EnlacesZ enlaces() {
        return enlaces;
    }


    private void registrarClases(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {

            for (NodoAst nodo : programa.elementos()) {

                if (!(nodo instanceof ZAst.Clase clase)) {
                    continue;
                }

                if (clases.containsKey(
                        clase.nombre()
                )) {
                    error(
                            clase.posicion(),
                            "La clase '"
                                    + clase.nombre()
                                    + "' ya fue declarada"
                    );

                    continue;
                }

                clases.put(
                        clase.nombre(),
                        new ClaseInfo(clase)
                );

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
    }


    private void registrarMiembros() {

        for (ClaseInfo info : clases.values()) {

            ZAst.Clase clase =
                    info.ast;

            for (ZAst.Miembro miembro
                    : clase.miembros()) {

                if (miembro instanceof ZAst.Atributo atributo) {
                    registrarAtributo(
                            info,
                            atributo
                    );

                    continue;
                }

                if (miembro instanceof ZAst.Constructor constructor) {
                    registrarConstructor(
                            info,
                            constructor
                    );

                    continue;
                }

                if (miembro instanceof ZAst.Metodo metodo) {
                    registrarMetodo(
                            info,
                            metodo
                    );
                }
            }
        }
    }

    private void registrarAtributo(
            ClaseInfo info,
            ZAst.Atributo atributo
    ) {
        if (info.atributos.containsKey(
                atributo.nombre()
        )) {
            error(
                    atributo.posicion(),
                    "El atributo '"
                            + atributo.nombre()
                            + "' ya fue declarado en la clase '"
                            + info.ast.nombre()
                            + "'"
            );

            return;
        }

        info.atributos.put(
                atributo.nombre(),
                atributo
        );

        validarTipo(
                atributo.tipo()
        );
    }

    private void registrarConstructor(
            ClaseInfo info,
            ZAst.Constructor constructor
    ) {
        if (!constructor.nombre()
                .equals(info.ast.nombre())) {

            error(
                    constructor.posicion(),
                    "El constructor '"
                            + constructor.nombre()
                            + "' debe llamarse '"
                            + info.ast.nombre()
                            + "'"
            );
        }

        validarParametros(
                constructor.parametros()
        );

        String firma =
                firma(
                        constructor.nombre(),
                        constructor.parametros()
                );

        boolean repetido =
                info.constructores.stream()
                        .anyMatch(
                                existente ->
                                        firma(
                                                existente.nombre(),
                                                existente.parametros()
                                        ).equals(firma)
                        );

        if (repetido) {
            error(
                    constructor.posicion(),
                    "Ya existe un constructor con la firma "
                            + firma
            );

            return;
        }

        info.constructores.add(
                constructor
        );
    }

    private void registrarMetodo(
            ClaseInfo info,
            ZAst.Metodo metodo
    ) {
        metodo.retorno()
                .ifPresent(
                        this::validarTipo
                );

        validarParametros(
                metodo.parametros()
        );

        List<ZAst.Metodo> sobrecargas =
                info.metodos.computeIfAbsent(
                        metodo.nombre(),
                        ignorado ->
                                new ArrayList<>()
                );

        String firmaNueva =
                firma(
                        metodo.nombre(),
                        metodo.parametros()
                );

        boolean repetido =
                sobrecargas.stream()
                        .anyMatch(
                                existente ->
                                        firma(
                                                existente.nombre(),
                                                existente.parametros()
                                        ).equals(
                                                firmaNueva
                                        )
                        );

        if (repetido) {
            error(
                    metodo.posicion(),
                    "Ya existe un metodo con la firma "
                            + firmaNueva
            );

            return;
        }

        sobrecargas.add(
                metodo
        );
    }

    private void validarParametros(
            List<ZAst.Parametro> parametros
    ) {
        for (ZAst.Parametro parametro
                : parametros) {

            validarTipo(
                    parametro.tipo()
            );
        }
    }


    private void analizarClase(
            ClaseInfo info
    ) {
        claseActual = info;

        tabla.entrar(
                "clase:"
                        + info.ast.nombre()
        );

        declararMiembrosEnTabla(
                info
        );

        analizarInicializadoresAtributos(
                info
        );
        if (info.constructores.isEmpty() && info.ast.padre().isPresent()) {
            validarConstructorPadre(List.of(), info.ast.posicion(), null);
        }

        for (ZAst.Constructor constructor
                : info.constructores) {

            analizarConstructor(
                    constructor
            );
        }

        for (List<ZAst.Metodo> sobrecargas
                : info.metodos.values()) {

            for (ZAst.Metodo metodo
                    : sobrecargas) {

                analizarMetodo(
                        metodo
                );
            }
        }

        tabla.salir();

        claseActual = null;
    }

    private void declararMiembrosEnTabla(
            ClaseInfo info
    ) {
        for (ZAst.Atributo atributo
                : jerarquia.atributos(info.ast.nombre()).values()) {
            if (!jerarquia.accesible(atributo.visibilidad(),
                    jerarquia.propietario(atributo), info.ast.nombre())) {
                continue;
            }

            TipoZ tipo =
                    resolverTipo(
                            atributo.tipo()
                    );

            tabla.declarar(
                    new Simbolo(
                            atributo.nombre(),
                            CategoriaSimbolo.ATRIBUTO,
                            tipoDato(tipo),
                            tipo.declarado(),
                            atributo.posicion()
                    )
            );
        }

        for (ZAst.Constructor constructor
                : info.constructores) {

            String firma =
                    firma(
                            constructor.nombre(),
                            constructor.parametros()
                    );

            tabla.declarar(
                    new Simbolo(
                            "constructor:"
                                    + firma,
                            CategoriaSimbolo.CONSTRUCTOR,
                            TipoDato.OBJETO,
                            info.ast.nombre(),
                            constructor.posicion()
                    )
            );
        }

        for (List<ZAst.Metodo> metodos
                : info.metodos.values()) {

            for (ZAst.Metodo metodo
                    : metodos) {

                TipoZ retorno =
                        metodo.retorno()
                                .map(this::resolverTipo)
                                .orElse(
                                        TipoZ.voidTipo()
                                );

                tabla.declarar(
                        new Simbolo(
                                "metodo:"
                                        + firma(
                                        metodo.nombre(),
                                        metodo.parametros()
                                ),
                                CategoriaSimbolo.METODO,
                                tipoDato(retorno),
                                retorno.declarado(),
                                metodo.posicion()
                        )
                );
            }
        }
    }

    private void analizarInicializadoresAtributos(
            ClaseInfo info
    ) {
        for (ZAst.Atributo atributo
                : info.atributos.values()) {

            if (atributo.inicializador()
                    .isEmpty()) {
                continue;
            }

            TipoZ esperado =
                    resolverTipo(
                            atributo.tipo()
                    );

            validarInicializador(
                    esperado,
                    atributo.inicializador()
                            .orElseThrow(),
                    atributo.posicion()
            );
        }
    }


    private void analizarConstructor(
            ZAst.Constructor constructor
    ) {
        tabla.entrar(
                "constructor:"
                        + firma(
                        constructor.nombre(),
                        constructor.parametros()
                )
        );

        TipoZ retornoAnterior =
                retornoActual;

        boolean retornoAnteriorEncontrado =
                retornoEncontrado;

        retornoActual =
                TipoZ.voidTipo();

        retornoEncontrado = false;

        declararParametros(
                constructor.parametros()
        );

        ZAst.Llamada anteriorSuper = superPermitido;
        superPermitido = primeraLlamadaSuper(constructor);
        if (superPermitido == null && claseActual.ast.padre().isPresent()) {
            validarConstructorPadre(List.of(), constructor.posicion(), null);
        }

        analizarBloque(
                constructor.cuerpo(),
                false
        );
        superPermitido = anteriorSuper;

        retornoActual =
                retornoAnterior;

        retornoEncontrado =
                retornoAnteriorEncontrado;

        tabla.salir();
    }


    private void analizarMetodo(
            ZAst.Metodo metodo
    ) {
        tabla.entrar(
                "metodo:"
                        + firma(
                        metodo.nombre(),
                        metodo.parametros()
                )
        );

        TipoZ retornoAnterior =
                retornoActual;

        boolean retornoAnteriorEncontrado =
                retornoEncontrado;

        retornoActual =
                metodo.retorno()
                        .map(this::resolverTipo)
                        .orElse(
                                TipoZ.voidTipo()
                        );

        retornoEncontrado = false;

        declararParametros(
                metodo.parametros()
        );

        analizarBloque(
                metodo.cuerpo(),
                false
        );

        if (!retornoActual.esVoid()
                && !retornoEncontrado) {

            error(
                    metodo.posicion(),
                    "El metodo '"
                            + metodo.nombre()
                            + "' debe retornar un valor de tipo '"
                            + retornoActual.declarado()
                            + "'"
            );
        }

        retornoActual =
                retornoAnterior;

        retornoEncontrado =
                retornoAnteriorEncontrado;

        tabla.salir();
    }

    private void declararParametros(
            List<ZAst.Parametro> parametros
    ) {
        for (ZAst.Parametro parametro
                : parametros) {

            TipoZ tipo =
                    resolverTipo(
                            parametro.tipo()
                    );

            Simbolo simbolo =
                    new Simbolo(
                            parametro.nombre(),
                            CategoriaSimbolo.PARAMETRO,
                            tipoDato(tipo),
                            tipo.declarado(),
                            parametro.posicion()
                    );

            if (!tabla.declarar(
                    simbolo
            )) {
                error(
                        parametro.posicion(),
                        "El parametro '"
                                + parametro.nombre()
                                + "' ya fue declarado"
                );
            }
        }
    }


    private void analizarBloque(
            ZAst.Bloque bloque,
            boolean crearAmbito
    ) {
        if (crearAmbito) {
            tabla.entrar(
                    "bloque:"
                            + contadorAmbitos++
            );
        }

        for (ZAst.Sentencia sentencia
                : bloque.sentencias()) {

            analizarSentencia(
                    sentencia
            );
        }

        if (crearAmbito) {
            tabla.salir();
        }
    }

    private void analizarSentencia(
            ZAst.Sentencia sentencia
    ) {
        if (sentencia instanceof ZAst.Bloque bloque) {
            analizarBloque(
                    bloque,
                    true
            );

            return;
        }

        if (sentencia instanceof ZAst.Declaracion declaracion) {
            analizarDeclaracion(
                    declaracion
            );

            return;
        }

        if (sentencia instanceof ZAst.ExpresionSentencia expresion) {
            resolverExpresion(
                    expresion.expresion()
            );

            return;
        }

        if (sentencia instanceof ZAst.Si si) {
            analizarSi(si);
            return;
        }

        if (sentencia instanceof ZAst.Seleccion seleccion) {
            analizarSwitch(seleccion);
            return;
        }

        if (sentencia instanceof ZAst.Para para) {
            analizarPara(para);
            return;
        }

        if (sentencia instanceof ZAst.Mientras mientras) {
            analizarMientras(mientras);
            return;
        }

        if (sentencia instanceof ZAst.HacerMientras hacerMientras) {
            analizarHacerMientras(
                    hacerMientras
            );

            return;
        }

        if (sentencia instanceof ZAst.Retorno retorno) {
            analizarRetorno(
                    retorno
            );

            return;
        }

        if (sentencia instanceof ZAst.Romper romper) {

            if (profundidadCiclo == 0
                    && profundidadSwitch == 0) {

                error(
                        romper.posicion(),
                        "break solo puede utilizarse dentro de un ciclo o switch"
                );
            }

            return;
        }

        if (sentencia instanceof ZAst.Continuar continuar) {

            if (profundidadCiclo == 0) {
                error(
                        continuar.posicion(),
                        "continue solo puede utilizarse dentro de un ciclo"
                );
            }
        }
    }


    private void analizarDeclaracion(
            ZAst.Declaracion declaracion
    ) {
        TipoZ tipo =
                resolverTipo(
                        declaracion.tipo()
                );

        if (tipo.esDesconocido()) {
            error(
                    declaracion.tipo()
                            .posicion(),
                    "El tipo '"
                            + declaracion.tipo()
                            .nombreCompleto()
                            + "' no existe"
            );
        }

        Simbolo simbolo =
                new Simbolo(
                        declaracion.nombre(),
                        CategoriaSimbolo.VARIABLE,
                        tipoDato(tipo),
                        tipo.declarado(),
                        declaracion.posicion()
                );

        if (!tabla.declarar(
                simbolo
        )) {
            error(
                    declaracion.posicion(),
                    "La variable '"
                            + declaracion.nombre()
                            + "' ya fue declarada en este ambito"
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


    private void analizarSi(
            ZAst.Si si
    ) {
        TipoZ condicion =
                resolverExpresion(
                        si.condicion()
                );

        exigirBooleano(
                condicion,
                si.condicion()
                        .posicion(),
                "La condicion de if debe ser boolean"
        );

        tabla.entrar(
                "if:"
                        + contadorAmbitos++
        );

        analizarSentencia(
                si.entonces()
        );

        tabla.salir();

        if (si.sino().isPresent()) {

            tabla.entrar(
                    "else:"
                            + contadorAmbitos++
            );

            analizarSentencia(
                    si.sino()
                            .orElseThrow()
            );

            tabla.salir();
        }
    }


    private void analizarSwitch(
            ZAst.Seleccion seleccion
    ) {
        TipoZ selector =
                resolverExpresion(
                        seleccion.expresion()
                );

        int defaults = 0;

        profundidadSwitch++;

        for (ZAst.CasoSeleccion caso
                : seleccion.casos()) {

            if (caso.esDefault()) {
                defaults++;

                if (defaults > 1) {
                    error(
                            caso.posicion(),
                            "Un switch solo puede contener un default"
                    );
                }

            } else {

                TipoZ tipoCaso =
                        resolverExpresion(
                                caso.valor()
                                        .orElseThrow()
                        );

                if (!sonComparables(
                        selector,
                        tipoCaso
                )) {
                    error(
                            caso.posicion(),
                            "El tipo del case no es compatible con la expresion del switch"
                    );
                }
            }

            tabla.entrar(
                    "case:"
                            + contadorAmbitos++
            );

            for (ZAst.Sentencia sentencia
                    : caso.sentencias()) {

                analizarSentencia(
                        sentencia
                );
            }

            tabla.salir();
        }

        profundidadSwitch--;
    }


    private void analizarPara(
            ZAst.Para para
    ) {
        tabla.entrar(
                "for:"
                        + contadorAmbitos++
        );

        if (para.inicializacion()
                .isPresent()) {

            NodoAst inicio =
                    para.inicializacion()
                            .orElseThrow();

            if (inicio instanceof ZAst.Declaracion declaracion) {
                analizarDeclaracion(
                        declaracion
                );

            } else if (inicio instanceof ZAst.ListaExpresiones lista) {

                for (ZAst.Expresion expresion
                        : lista.expresiones()) {

                    resolverExpresion(
                            expresion
                    );
                }
            }
        }

        para.condicion()
                .ifPresent(
                        condicion -> {

                            TipoZ tipo =
                                    resolverExpresion(
                                            condicion
                                    );

                            exigirBooleano(
                                    tipo,
                                    condicion.posicion(),
                                    "La condicion de for debe ser boolean"
                            );
                        }
                );

        profundidadCiclo++;

        tabla.entrar(
                "cuerpo-for:"
                        + contadorAmbitos++
        );

        analizarSentencia(
                para.cuerpo()
        );

        tabla.salir();

        for (ZAst.Expresion actualizacion
                : para.actualizaciones()) {

            resolverExpresion(
                    actualizacion
            );
        }

        profundidadCiclo--;

        tabla.salir();
    }


    private void analizarMientras(
            ZAst.Mientras mientras
    ) {
        TipoZ condicion =
                resolverExpresion(
                        mientras.condicion()
                );

        exigirBooleano(
                condicion,
                mientras.condicion()
                        .posicion(),
                "La condicion de while debe ser boolean"
        );

        profundidadCiclo++;

        tabla.entrar(
                "while:"
                        + contadorAmbitos++
        );

        analizarSentencia(
                mientras.cuerpo()
        );

        tabla.salir();

        profundidadCiclo--;
    }


    private void analizarHacerMientras(
            ZAst.HacerMientras hacer
    ) {
        profundidadCiclo++;

        tabla.entrar(
                "do:"
                        + contadorAmbitos++
        );

        analizarSentencia(
                hacer.cuerpo()
        );

        tabla.salir();

        profundidadCiclo--;

        TipoZ condicion =
                resolverExpresion(
                        hacer.condicion()
                );

        exigirBooleano(
                condicion,
                hacer.condicion()
                        .posicion(),
                "La condicion de do-while debe ser boolean"
        );
    }


    private void analizarRetorno(
            ZAst.Retorno retorno
    ) {
        retornoEncontrado = true;

        if (retornoActual.esVoid()) {

            if (retorno.expresion()
                    .isPresent()) {

                resolverExpresion(
                        retorno.expresion()
                                .orElseThrow()
                );

                error(
                        retorno.posicion(),
                        "Un metodo void o constructor no puede retornar un valor"
                );
            }

            return;
        }

        if (retorno.expresion()
                .isEmpty()) {

            error(
                    retorno.posicion(),
                    "Se esperaba retornar un valor de tipo '"
                            + retornoActual.declarado()
                            + "'"
            );

            return;
        }

        TipoZ recibido =
                resolverExpresion(
                        retorno.expresion()
                                .orElseThrow()
                );

        if (!esCompatible(
                retornoActual,
                recibido
        )) {
            error(
                    retorno.posicion(),
                    "El retorno es de tipo '"
                            + recibido.declarado()
                            + "' pero se requiere '"
                            + retornoActual.declarado()
                            + "'"
            );
        }
    }


    private void validarInicializador(
            TipoZ esperado,
            ZAst.Inicializador inicializador,
            PosicionFuente posicion
    ) {
        if (inicializador
                instanceof ZAst.InicializadorLista lista) {

            validarLista(
                    esperado,
                    lista
            );

            return;
        }

        TipoZ recibido =
                resolverExpresion(
                        (ZAst.Expresion) inicializador
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
            TipoZ esperado,
            ZAst.InicializadorLista lista
    ) {
        if (!esperado.esArreglo()) {

            error(
                    lista.posicion(),
                    "Un inicializador con llaves requiere un tipo arreglo"
            );

            return;
        }

        TipoZ elemento =
                esperado.reducirArreglo();

        for (ZAst.Inicializador valor
                : lista.valores()) {

            if (valor
                    instanceof ZAst.InicializadorLista interna) {

                if (!elemento.esArreglo()) {
                    error(
                            interna.posicion(),
                            "La profundidad del inicializador no coincide con las dimensiones del arreglo"
                    );

                    continue;
                }

                validarLista(
                        elemento,
                        interna
                );

            } else {

                TipoZ recibido =
                        resolverExpresion(
                                (ZAst.Expresion) valor
                        );

                if (!esCompatible(
                        elemento,
                        recibido
                )) {
                    error(
                            valor.posicion(),
                            "El arreglo requiere elementos de tipo '"
                                    + elemento.declarado()
                                    + "' pero se encontro '"
                                    + recibido.declarado()
                                    + "'"
                    );
                }
            }
        }
    }


    private TipoZ resolverExpresion(
            ZAst.Expresion expresion
    ) {
        TipoZ tipo =
                resolverExpresionInterna(
                        expresion
                );

        enlaces.registrarTipo(
                expresion,
                tipo.declarado()
        );

        return tipo;
    }

    private TipoZ resolverExpresionInterna(
            ZAst.Expresion expresion
    ) {
        if (expresion instanceof ZAst.Literal literal) {
            return resolverLiteral(
                    literal
            );
        }

        if (expresion instanceof ZAst.Identificador identificador) {
            return resolverIdentificador(
                    identificador
            );
        }

        if (expresion instanceof ZAst.Binaria binaria) {
            return resolverBinaria(
                    binaria
            );
        }

        if (expresion instanceof ZAst.Unaria unaria) {
            return resolverUnaria(
                    unaria
            );
        }

        if (expresion instanceof ZAst.AsignacionExpresion asignacion) {
            return resolverAsignacion(
                    asignacion
            );
        }

        if (expresion instanceof ZAst.Ternaria ternaria) {
            return resolverTernaria(
                    ternaria
            );
        }

        if (expresion instanceof ZAst.Llamada llamada) {
            return resolverLlamada(
                    llamada
            );
        }

        if (expresion instanceof ZAst.AccesoArreglo acceso) {
            return resolverAccesoArreglo(
                    acceso
            );
        }

        if (expresion instanceof ZAst.AccesoMiembro acceso) {
            return resolverAccesoMiembro(
                    acceso
            );
        }

        if (expresion instanceof ZAst.CambioPostfijo cambio) {
            return resolverCambioPostfijo(
                    cambio
            );
        }

        if (expresion instanceof ZAst.NuevoObjeto nuevo) {
            return resolverNuevoObjeto(
                    nuevo
            );
        }

        if (expresion instanceof ZAst.NuevoArreglo nuevo) {
            return resolverNuevoArreglo(
                    nuevo
            );
        }

        return TipoZ.desconocido();
    }


    private TipoZ resolverLiteral(
            ZAst.Literal literal
    ) {
        return switch (literal.tipo()) {

            case ENTERO ->
                    TipoZ.primitivo(
                            ClaseTipo.INT,
                            "int"
                    );

            case DECIMAL ->
                    TipoZ.primitivo(
                            ClaseTipo.DOUBLE,
                            "double"
                    );

            case CADENA ->
                    TipoZ.primitivo(
                            ClaseTipo.STRING,
                            "String"
                    );

            case CARACTER ->
                    TipoZ.primitivo(
                            ClaseTipo.CHAR,
                            "char"
                    );

            case BOOLEANO ->
                    TipoZ.primitivo(
                            ClaseTipo.BOOLEAN,
                            "boolean"
                    );

            case NULO ->
                    TipoZ.nulo();
        };
    }


    private TipoZ resolverIdentificador(
            ZAst.Identificador identificador
    ) {
        if (identificador.nombre().equals("this")) {
            return TipoZ.objeto(claseActual.ast.nombre());
        }
        if (identificador.nombre().equals("super")) {
            if (claseActual.ast.padre().isEmpty()) {
                error(identificador.posicion(), "super requiere una clase padre");
                return TipoZ.desconocido();
            }
            return TipoZ.objeto(claseActual.ast.padre().orElseThrow());
        }
        Optional<Simbolo> encontrado =
                tabla.buscar(
                        identificador.nombre()
                );

        if (encontrado.isEmpty()) {
            error(
                    identificador.posicion(),
                    "El identificador '"
                            + identificador.nombre()
                            + "' no ha sido declarado"
            );

            return TipoZ.desconocido();
        }

        Simbolo simbolo =
                encontrado.orElseThrow();

        if (simbolo.categoria()
                == CategoriaSimbolo.CLASE) {

            error(
                    identificador.posicion(),
                    "La clase '"
                            + identificador.nombre()
                            + "' no puede utilizarse directamente como valor"
            );

            return TipoZ.desconocido();
        }

        return tipoDesdeDeclarado(
                simbolo.tipoDeclarado()
        );
    }


    private TipoZ resolverBinaria(
            ZAst.Binaria binaria
    ) {
        TipoZ izquierda =
                resolverExpresion(
                        binaria.izquierda()
                );

        TipoZ derecha =
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
                 DIVISION,
                 MODULO ->
                    resolverAritmetica(
                            izquierda,
                            derecha,
                            binaria.posicion()
                    );

            case MENOR,
                 MAYOR,
                 MENOR_IGUAL,
                 MAYOR_IGUAL -> {

                if (!esNumerico(izquierda)
                        || !esNumerico(derecha)) {

                    if (!izquierda.esDesconocido()
                            && !derecha.esDesconocido()) {

                        error(
                                binaria.posicion(),
                                "Los operadores relacionales requieren valores numericos"
                        );
                    }
                }

                yield TipoZ.primitivo(
                        ClaseTipo.BOOLEAN,
                        "boolean"
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

                yield TipoZ.primitivo(
                        ClaseTipo.BOOLEAN,
                        "boolean"
                );
            }

            case AND,
                 OR -> {

                if (!izquierda.esBoolean()
                        || !derecha.esBoolean()) {

                    if (!izquierda.esDesconocido()
                            && !derecha.esDesconocido()) {

                        error(
                                binaria.posicion(),
                                "Los operadores logicos requieren valores boolean"
                        );
                    }
                }

                yield TipoZ.primitivo(
                        ClaseTipo.BOOLEAN,
                        "boolean"
                );
            }
        };
    }

    private TipoZ resolverSuma(
            TipoZ izquierda,
            TipoZ derecha,
            PosicionFuente posicion
    ) {
        if (izquierda.clase()
                == ClaseTipo.STRING
                || derecha.clase()
                == ClaseTipo.STRING) {

            if (izquierda.esArreglo()
                    || derecha.esArreglo()
                    || izquierda.clase()
                    == ClaseTipo.OBJETO
                    || derecha.clase()
                    == ClaseTipo.OBJETO) {

                error(
                        posicion,
                        "No se puede concatenar directamente un arreglo u objeto"
                );

                return TipoZ.desconocido();
            }

            return TipoZ.primitivo(
                    ClaseTipo.STRING,
                    "String"
            );
        }

        return resolverAritmetica(
                izquierda,
                derecha,
                posicion
        );
    }

    private TipoZ resolverAritmetica(
            TipoZ izquierda,
            TipoZ derecha,
            PosicionFuente posicion
    ) {
        if (!esNumerico(izquierda)
                || !esNumerico(derecha)) {

            if (!izquierda.esDesconocido()
                    && !derecha.esDesconocido()) {

                error(
                        posicion,
                        "La operacion aritmetica requiere valores numericos"
                );
            }

            return TipoZ.desconocido();
        }

        if (izquierda.clase()
                == ClaseTipo.DOUBLE
                || derecha.clase()
                == ClaseTipo.DOUBLE) {

            return TipoZ.primitivo(
                    ClaseTipo.DOUBLE,
                    "double"
            );
        }

        return TipoZ.primitivo(
                ClaseTipo.INT,
                "int"
        );
    }


    private TipoZ resolverUnaria(
            ZAst.Unaria unaria
    ) {
        TipoZ tipo =
                resolverExpresion(
                        unaria.expresion()
                );

        if (unaria.operador()
                == ZAst.OperadorUnario.NEGACION) {

            exigirBooleano(
                    tipo,
                    unaria.posicion(),
                    "El operador ! requiere un valor boolean"
            );

            return TipoZ.primitivo(
                    ClaseTipo.BOOLEAN,
                    "boolean"
            );
        }

        if (!esNumerico(tipo)
                && !tipo.esDesconocido()) {

            error(
                    unaria.posicion(),
                    "El operador requiere un valor numerico"
            );
        }

        if (unaria.operador()
                == ZAst.OperadorUnario.INCREMENTO_PRE
                || unaria.operador()
                == ZAst.OperadorUnario.DECREMENTO_PRE) {

            exigirAsignable(
                    unaria.expresion(),
                    unaria.posicion()
            );
        }

        return tipo;
    }


    private TipoZ resolverAsignacion(
            ZAst.AsignacionExpresion asignacion
    ) {
        exigirAsignable(
                asignacion.destino(),
                asignacion.posicion()
        );

        TipoZ destino =
                resolverExpresion(
                        asignacion.destino()
                );

        TipoZ valor =
                resolverExpresion(
                        asignacion.valor()
                );

        if (asignacion.operador()
                == ZAst.OperadorAsignacion.ASIGNAR) {

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

        if (asignacion.operador()
                == ZAst.OperadorAsignacion.SUMAR_ASIGNAR
                && destino.clase()
                == ClaseTipo.STRING) {

            return destino;
        }

        if (!esNumerico(destino)
                || !esNumerico(valor)) {

            if (!destino.esDesconocido()
                    && !valor.esDesconocido()) {

                error(
                        asignacion.posicion(),
                        "La asignacion compuesta requiere valores numericos"
                );
            }
        }

        return destino;
    }


    private TipoZ resolverTernaria(
            ZAst.Ternaria ternaria
    ) {
        TipoZ condicion =
                resolverExpresion(
                        ternaria.condicion()
                );

        exigirBooleano(
                condicion,
                ternaria.condicion()
                        .posicion(),
                "La condicion del operador ternario debe ser boolean"
        );

        TipoZ verdadero =
                resolverExpresion(
                        ternaria.verdadero()
                );

        TipoZ falso =
                resolverExpresion(
                        ternaria.falso()
                );

        TipoZ comun =
                tipoComun(
                        verdadero,
                        falso
                );

        if (comun.esDesconocido()
                && !verdadero.esDesconocido()
                && !falso.esDesconocido()) {

            error(
                    ternaria.posicion(),
                    "Los resultados del operador ternario no son compatibles"
            );
        }

        return comun;
    }


    private TipoZ resolverLlamada(
            ZAst.Llamada llamada
    ) {
        if (llamada.objetivo()
                instanceof ZAst.Identificador identificador) {

            if (identificador.nombre().equals("super")) {
                if (llamada != superPermitido) {
                    error(llamada.posicion(),
                            "super(...) solo puede ser la primera instruccion del constructor");
                    resolverArgumentos(llamada.argumentos());
                    return TipoZ.desconocido();
                }
                return validarConstructorPadre(llamada.argumentos(), llamada.posicion(), llamada);
            }
            if (identificador.nombre().equals("this")) {
                error(llamada.posicion(), "La llamada this(...) no esta soportada");
                resolverArgumentos(llamada.argumentos());
                return TipoZ.desconocido();
            }

            if (identificador.nombre()
                    .equals("print")
                    || identificador.nombre()
                    .equals("println")) {

                return resolverPrint(
                        llamada
                );
            }

            if (identificador.nombre()
                    .equals("readln")) {

                return resolverReadln(
                        llamada
                );
            }

            return resolverMetodo(
                    claseActual,
                    identificador.nombre(),
                    llamada.argumentos(),
                    llamada.posicion(),
                    llamada
            );
        }

        if (llamada.objetivo()
                instanceof ZAst.AccesoMiembro acceso) {

            if (esSystemOut(
                    acceso
            )) {
                return resolverPrint(
                        llamada
                );
            }

            TipoZ receptor =
                    resolverExpresion(
                            acceso.objetivo()
                    );

            if (receptor.clase()
                    != ClaseTipo.OBJETO
                    || receptor.esArreglo()) {

                if (!receptor.esDesconocido()) {
                    error(
                            llamada.posicion(),
                            "Solo un objeto puede invocar metodos"
                    );
                }

                resolverArgumentos(
                        llamada.argumentos()
                );

                return TipoZ.desconocido();
            }

            ClaseInfo clase =
                    clases.get(
                            receptor.nombreBase()
                    );

            if (clase == null) {
                resolverArgumentos(
                        llamada.argumentos()
                );

                return TipoZ.desconocido();
            }

            return resolverMetodo(
                    clase,
                    acceso.miembro(),
                    llamada.argumentos(),
                    llamada.posicion(),
                    llamada
            );
        }

        error(
                llamada.posicion(),
                "La expresion no puede ser invocada como metodo"
        );

        resolverArgumentos(
                llamada.argumentos()
        );

        return TipoZ.desconocido();
    }

    private TipoZ resolverPrint(
            ZAst.Llamada llamada
    ) {
        if (llamada.argumentos().size()
                != 1) {

            error(
                    llamada.posicion(),
                    "print/println requiere exactamente un argumento"
            );
        }

        resolverArgumentos(
                llamada.argumentos()
        );

        return TipoZ.voidTipo();
    }

    private TipoZ resolverReadln(
            ZAst.Llamada llamada
    ) {
        if (!llamada.argumentos()
                .isEmpty()) {

            error(
                    llamada.posicion(),
                    "readln no recibe argumentos"
            );

            resolverArgumentos(
                    llamada.argumentos()
            );
        }

        return TipoZ.primitivo(
                ClaseTipo.STRING,
                "String"
        );
    }

    private TipoZ resolverMetodo(
            ClaseInfo clase,
            String nombre,
            List<ZAst.Expresion> argumentos,
            PosicionFuente posicion,
            ZAst.Llamada llamada
    ) {
        List<TipoZ> tiposArgumentos =
                resolverArgumentos(
                        argumentos
                );

        List<ZAst.Metodo> todos = jerarquia.metodos(clase.ast.nombre(), nombre);
        List<ZAst.Metodo> candidatos = todos.stream()
                .filter(m -> jerarquia.accesible(m.visibilidad(),
                        jerarquia.propietario(m), claseActual.ast.nombre()))
                .toList();
        if (!todos.isEmpty() && candidatos.isEmpty()) {
            error(posicion, "El metodo '" + nombre + "' no es accesible desde '"
                    + claseActual.ast.nombre() + "'");
            return TipoZ.desconocido();
        }

        if (candidatos.isEmpty()) {
            error(
                    posicion,
                    "La clase '"
                            + clase.ast.nombre()
                            + "' no contiene un metodo llamado '"
                            + nombre
                            + "'"
            );

            return TipoZ.desconocido();
        }

        ZAst.Metodo seleccionado =
                seleccionarMetodo(
                        candidatos,
                        tiposArgumentos
                );

        if (seleccionado == null) {
            error(
                    posicion,
                    "No existe una sobrecarga compatible para "
                            + nombre
                            + firmaArgumentos(
                            tiposArgumentos
                    )
            );

            return TipoZ.desconocido();
        }

        enlaces.registrarLlamada(
                llamada,
                jerarquia.propietario(seleccionado)
                        + "."
                        + firma(
                        seleccionado.nombre(),
                        seleccionado.parametros()
                )
        );

        return seleccionado.retorno()
                .map(this::resolverTipo)
                .orElse(
                        TipoZ.voidTipo()
                );
    }


    private TipoZ resolverAccesoArreglo(
            ZAst.AccesoArreglo acceso
    ) {
        TipoZ objetivo =
                resolverExpresion(
                        acceso.objetivo()
                );

        TipoZ indice =
                resolverExpresion(
                        acceso.indice()
                );

        if (indice.clase()
                != ClaseTipo.INT
                && !indice.esDesconocido()) {

            error(
                    acceso.indice()
                            .posicion(),
                    "El indice de un arreglo debe ser int"
            );
        }

        if (!objetivo.esArreglo()) {

            if (!objetivo.esDesconocido()) {
                error(
                        acceso.posicion(),
                        "Se intento indexar un valor que no es un arreglo"
                );
            }

            return TipoZ.desconocido();
        }

        return objetivo.reducirArreglo();
    }


    private TipoZ resolverAccesoMiembro(
            ZAst.AccesoMiembro acceso
    ) {
        TipoZ objetivo =
                resolverExpresion(
                        acceso.objetivo()
                );

        if (objetivo.clase()
                != ClaseTipo.OBJETO
                || objetivo.esArreglo()) {

            if (!objetivo.esDesconocido()) {
                error(
                        acceso.posicion(),
                        "Solo un objeto puede acceder a atributos"
                );
            }

            return TipoZ.desconocido();
        }

        ClaseInfo info =
                clases.get(
                        objetivo.nombreBase()
                );

        if (info == null) {
            return TipoZ.desconocido();
        }

        ZAst.Atributo atributo = jerarquia.atributos(info.ast.nombre())
                .get(acceso.miembro());

        if (atributo != null) {
            if (!jerarquia.accesible(atributo.visibilidad(),
                    jerarquia.propietario(atributo), claseActual.ast.nombre())) {
                error(acceso.posicion(), "El atributo '" + acceso.miembro()
                        + "' no es accesible desde '" + claseActual.ast.nombre() + "'");
                return TipoZ.desconocido();
            }
            return resolverTipo(
                    atributo.tipo()
            );
        }

        if (info.metodos.containsKey(
                acceso.miembro()
        )) {

            error(
                    acceso.posicion(),
                    "El metodo '"
                            + acceso.miembro()
                            + "' debe invocarse usando parentesis"
            );

            return TipoZ.desconocido();
        }

        error(
                acceso.posicion(),
                "La clase '"
                        + info.ast.nombre()
                        + "' no contiene el miembro '"
                        + acceso.miembro()
                        + "'"
        );

        return TipoZ.desconocido();
    }


    private TipoZ resolverCambioPostfijo(
            ZAst.CambioPostfijo cambio
    ) {
        exigirAsignable(
                cambio.objetivo(),
                cambio.posicion()
        );

        TipoZ tipo =
                resolverExpresion(
                        cambio.objetivo()
                );

        if (!esNumerico(tipo)
                && !tipo.esDesconocido()) {

            error(
                    cambio.posicion(),
                    "Los operadores ++ y -- requieren un valor numerico"
            );
        }

        return tipo;
    }


    private TipoZ resolverNuevoObjeto(
            ZAst.NuevoObjeto nuevo
    ) {
        ClaseInfo clase =
                clases.get(
                        nuevo.tipo()
                );

        List<TipoZ> argumentos =
                resolverArgumentos(
                        nuevo.argumentos()
                );

        if (clase == null) {
            error(
                    nuevo.posicion(),
                    "La clase '"
                            + nuevo.tipo()
                            + "' no existe"
            );

            return TipoZ.desconocido();
        }

        if (clase.constructores.isEmpty()
                && argumentos.isEmpty()) {

            return TipoZ.objeto(
                    nuevo.tipo()
            );
        }

        ZAst.Constructor constructor =
                seleccionarConstructor(
                        clase.constructores,
                        argumentos
                );

        if (constructor == null) {
            error(
                    nuevo.posicion(),
                    "No existe un constructor compatible para "
                            + nuevo.tipo()
                            + firmaArgumentos(
                            argumentos
                    )
            );
        } else {
            if (!jerarquia.accesible(constructor.visibilidad(),
                    clase.ast.nombre(), claseActual.ast.nombre())) {
                error(nuevo.posicion(), "El constructor de '" + nuevo.tipo()
                        + "' no es accesible");
            }
            enlaces.registrarConstructor(
                    nuevo,
                    nuevo.tipo()
                            + "."
                            + firma(
                            "<init>",
                            constructor.parametros()
                    )
            );
        }

        return TipoZ.objeto(
                nuevo.tipo()
        );
    }


    private TipoZ resolverNuevoArreglo(
            ZAst.NuevoArreglo nuevo
    ) {
        TipoZ base =
                resolverTipoBase(
                        nuevo.tipoBase()
                );

        if (base.esDesconocido()) {
            error(
                    nuevo.posicion(),
                    "El tipo '"
                            + nuevo.tipoBase()
                            + "' no existe"
            );
        }

        for (ZAst.Expresion dimension
                : nuevo.dimensiones()) {

            TipoZ tipoDimension =
                    resolverExpresion(
                            dimension
                    );

            if (tipoDimension.clase()
                    != ClaseTipo.INT
                    && !tipoDimension.esDesconocido()) {

                error(
                        dimension.posicion(),
                        "Las dimensiones de un arreglo deben ser int"
                );
            }
        }

        return new TipoZ(
                base.clase(),
                base.nombreBase(),
                nuevo.dimensiones()
                        .size()
        );
    }


    private ZAst.Metodo seleccionarMetodo(
            List<ZAst.Metodo> candidatos,
            List<TipoZ> argumentos
    ) {
        ZAst.Metodo mejor = null;
        int mejorPuntaje =
                Integer.MAX_VALUE;

        boolean ambiguo = false;

        for (ZAst.Metodo candidato
                : candidatos) {

            int puntaje =
                    puntajeParametros(
                            candidato.parametros(),
                            argumentos
                    );

            if (puntaje < 0) {
                continue;
            }

            if (puntaje < mejorPuntaje) {
                mejor = candidato;
                mejorPuntaje = puntaje;
                ambiguo = false;

            } else if (puntaje
                    == mejorPuntaje) {

                ambiguo = true;
            }
        }

        if (ambiguo) {
            return null;
        }

        return mejor;
    }

    private ZAst.Constructor seleccionarConstructor(
            List<ZAst.Constructor> candidatos,
            List<TipoZ> argumentos
    ) {
        ZAst.Constructor mejor = null;
        int mejorPuntaje =
                Integer.MAX_VALUE;

        boolean ambiguo = false;

        for (ZAst.Constructor candidato
                : candidatos) {

            int puntaje =
                    puntajeParametros(
                            candidato.parametros(),
                            argumentos
                    );

            if (puntaje < 0) {
                continue;
            }

            if (puntaje < mejorPuntaje) {
                mejor = candidato;
                mejorPuntaje = puntaje;
                ambiguo = false;

            } else if (puntaje
                    == mejorPuntaje) {

                ambiguo = true;
            }
        }

        if (ambiguo) {
            return null;
        }

        return mejor;
    }

    private int puntajeParametros(
            List<ZAst.Parametro> parametros,
            List<TipoZ> argumentos
    ) {
        if (parametros.size()
                != argumentos.size()) {

            return -1;
        }

        int total = 0;

        for (int indice = 0;
             indice < parametros.size();
             indice++) {

            TipoZ esperado =
                    resolverTipo(
                            parametros.get(indice)
                                    .tipo()
                    );

            TipoZ recibido =
                    argumentos.get(indice);

            int puntaje =
                    puntajeConversion(
                            esperado,
                            recibido
                    );

            if (puntaje < 0) {
                return -1;
            }

            total += puntaje;
        }

        return total;
    }

    private int puntajeConversion(
            TipoZ esperado,
            TipoZ recibido
    ) {
        if (esperado.esDesconocido()
                || recibido.esDesconocido()) {

            return 100;
        }

        if (esperado.equals(recibido)) {
            return 0;
        }

        if (recibido.esNulo()
                && esperado.esReferencia()) {

            return 1;
        }

        if (esperado.esArreglo()
                || recibido.esArreglo()) {

            return -1;
        }

        if (esperado.clase() == ClaseTipo.OBJETO
                && recibido.clase() == ClaseTipo.OBJETO) {
            return jerarquia.distancia(recibido.nombreBase(), esperado.nombreBase());
        }

        if (esperado.clase()
                == ClaseTipo.INT
                && recibido.clase()
                == ClaseTipo.CHAR) {

            return 1;
        }

        if (esperado.clase()
                == ClaseTipo.DOUBLE
                && recibido.clase()
                == ClaseTipo.INT) {

            return 1;
        }

        if (esperado.clase()
                == ClaseTipo.DOUBLE
                && recibido.clase()
                == ClaseTipo.CHAR) {

            return 2;
        }

        return -1;
    }


    private void validarTipo(
            ZAst.Tipo tipo
    ) {
        TipoZ resuelto =
                resolverTipo(
                        tipo
                );

        if (resuelto.esDesconocido()) {
            error(
                    tipo.posicion(),
                    "El tipo '"
                            + tipo.nombreCompleto()
                            + "' no existe"
            );
        }
    }

    private TipoZ resolverTipo(
            ZAst.Tipo tipo
    ) {
        TipoZ base =
                resolverTipoBase(
                        tipo.nombreBase()
                );

        if (base.esDesconocido()) {
            return base;
        }

        return new TipoZ(
                base.clase(),
                base.nombreBase(),
                tipo.dimensiones()
        );
    }

    private TipoZ resolverTipoBase(
            String nombre
    ) {
        return switch (nombre) {

            case "int" ->
                    TipoZ.primitivo(
                            ClaseTipo.INT,
                            "int"
                    );

            case "double" ->
                    TipoZ.primitivo(
                            ClaseTipo.DOUBLE,
                            "double"
                    );

            case "char" ->
                    TipoZ.primitivo(
                            ClaseTipo.CHAR,
                            "char"
                    );

            case "boolean" ->
                    TipoZ.primitivo(
                            ClaseTipo.BOOLEAN,
                            "boolean"
                    );

            case "String" ->
                    TipoZ.primitivo(
                            ClaseTipo.STRING,
                            "String"
                    );

            default -> {

                if (clases.containsKey(
                        nombre
                )) {
                    yield TipoZ.objeto(
                            nombre
                    );
                }

                yield TipoZ.desconocido(
                        nombre
                );
            }
        };
    }

    private TipoZ tipoDesdeDeclarado(
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

        TipoZ tipoBase =
                resolverTipoBase(
                        base
                );

        if (tipoBase.esDesconocido()) {
            return tipoBase;
        }

        return new TipoZ(
                tipoBase.clase(),
                tipoBase.nombreBase(),
                dimensiones
        );
    }

    private TipoDato tipoDato(
            TipoZ tipo
    ) {
        if (tipo.esArreglo()) {
            return TipoDato.ARREGLO;
        }

        return switch (tipo.clase()) {

            case INT ->
                    TipoDato.ENTERO;

            case DOUBLE ->
                    TipoDato.DECIMAL;

            case CHAR ->
                    TipoDato.CARACTER;

            case BOOLEAN ->
                    TipoDato.BOOLEANO;

            case STRING ->
                    TipoDato.CADENA;

            case OBJETO ->
                    TipoDato.OBJETO;

            case VOID ->
                    TipoDato.VOID;

            case NULL,
                 DESCONOCIDO ->
                    TipoDato.DESCONOCIDO;
        };
    }


    private boolean esCompatible(
            TipoZ esperado,
            TipoZ recibido
    ) {
        return puntajeConversion(
                esperado,
                recibido
        ) >= 0;
    }

    private boolean sonComparables(
            TipoZ primero,
            TipoZ segundo
    ) {
        if (primero.esDesconocido()
                || segundo.esDesconocido()) {

            return true;
        }

        if (primero.esNulo()
                && segundo.esReferencia()) {

            return true;
        }

        if (segundo.esNulo()
                && primero.esReferencia()) {

            return true;
        }

        if (primero.esArreglo()
                || segundo.esArreglo()) {

            if (primero.clase() == ClaseTipo.OBJETO && segundo.clase() == ClaseTipo.OBJETO) {
            return jerarquia.distancia(primero.nombreBase(), segundo.nombreBase()) >= 0
                    || jerarquia.distancia(segundo.nombreBase(), primero.nombreBase()) >= 0;
        }

        return primero.equals(
                    segundo
            );
        }

        if (esNumerico(primero)
                && esNumerico(segundo)) {

            return true;
        }

        return primero.equals(
                segundo
        );
    }

    private TipoZ tipoComun(
            TipoZ primero,
            TipoZ segundo
    ) {
        if (primero.equals(
                segundo
        )) {
            return primero;
        }

        if (primero.esNulo()
                && segundo.esReferencia()) {

            return segundo;
        }

        if (segundo.esNulo()
                && primero.esReferencia()) {

            return primero;
        }

        if (!primero.esArreglo() && !segundo.esArreglo()
                && primero.clase() == ClaseTipo.OBJETO
                && segundo.clase() == ClaseTipo.OBJETO) {
            for (ZAst.Clase ancestro : jerarquia.recorrido(primero.nombreBase())) {
                if (jerarquia.distancia(segundo.nombreBase(), ancestro.nombre()) >= 0) {
                    return TipoZ.objeto(ancestro.nombre());
                }
            }
        }

        if (esNumerico(primero)
                && esNumerico(segundo)) {

            if (primero.clase()
                    == ClaseTipo.DOUBLE
                    || segundo.clase()
                    == ClaseTipo.DOUBLE) {

                return TipoZ.primitivo(
                        ClaseTipo.DOUBLE,
                        "double"
                );
            }

            return TipoZ.primitivo(
                    ClaseTipo.INT,
                    "int"
            );
        }

        return TipoZ.desconocido();
    }

    private boolean esNumerico(
            TipoZ tipo
    ) {
        if (tipo.esArreglo()) {
            return false;
        }

        return tipo.clase()
                == ClaseTipo.INT
                || tipo.clase()
                == ClaseTipo.DOUBLE
                || tipo.clase()
                == ClaseTipo.CHAR;
    }


    private void exigirAsignable(
            ZAst.Expresion expresion,
            PosicionFuente posicion
    ) {
        if (expresion instanceof ZAst.Identificador identificador
                && (identificador.nombre().equals("this")
                || identificador.nombre().equals("super"))) {
            error(posicion, "No se puede asignar a this ni a super");
            return;
        }
        if (expresion instanceof ZAst.Identificador
                || expresion instanceof ZAst.AccesoArreglo
                || expresion instanceof ZAst.AccesoMiembro) {

            return;
        }

        error(
                posicion,
                "El lado izquierdo de la asignacion debe ser una variable, atributo o posicion de arreglo"
        );
    }


    private void exigirBooleano(
            TipoZ tipo,
            PosicionFuente posicion,
            String mensaje
    ) {
        if (!tipo.esBoolean()
                && !tipo.esDesconocido()) {

            error(
                    posicion,
                    mensaje
            );
        }
    }


    private List<TipoZ> resolverArgumentos(
            List<ZAst.Expresion> argumentos
    ) {
        return argumentos.stream()
                .map(
                        this::resolverExpresion
                )
                .toList();
    }


    private boolean esSystemOut(
            ZAst.AccesoMiembro acceso
    ) {
        if (!acceso.miembro()
                .equals("print")
                && !acceso.miembro()
                .equals("println")) {

            return false;
        }

        if (!(acceso.objetivo()
                instanceof ZAst.AccesoMiembro out)) {

            return false;
        }

        if (!out.miembro()
                .equals("out")) {

            return false;
        }

        return out.objetivo()
                instanceof ZAst.Identificador sistema
                && sistema.nombre()
                .equals("System");
    }


    private String firma(
            String nombre,
            List<ZAst.Parametro> parametros
    ) {
        String tipos =
                parametros.stream()
                        .map(
                                parametro ->
                                        parametro.tipo()
                                                .nombreCompleto()
                        )
                        .reduce(
                                "",
                                (actual, siguiente) -> {

                                    if (actual.isEmpty()) {
                                        return siguiente;
                                    }

                                    return actual
                                            + ","
                                            + siguiente;
                                }
                        );

        return nombre
                + "("
                + tipos
                + ")";
    }

    private String firmaArgumentos(
            List<TipoZ> argumentos
    ) {
        return "("
                + argumentos.stream()
                .map(
                        TipoZ::declarado
                )
                .reduce(
                        "",
                        (actual, siguiente) -> {

                            if (actual.isEmpty()) {
                                return siguiente;
                            }

                            return actual
                                    + ","
                                    + siguiente;
                        }
                )
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


    private ZAst.Llamada primeraLlamadaSuper(ZAst.Constructor constructor) {
        if (constructor.cuerpo().sentencias().isEmpty()) {
            return null;
        }
        var primera = constructor.cuerpo().sentencias().get(0);
        if (primera instanceof ZAst.ExpresionSentencia sentencia
                && sentencia.expresion() instanceof ZAst.Llamada llamada
                && llamada.objetivo() instanceof ZAst.Identificador identificador
                && identificador.nombre().equals("super")) {
            return llamada;
        }
        return null;
    }

    private TipoZ validarConstructorPadre(
            List<ZAst.Expresion> argumentos,
            PosicionFuente posicion,
            ZAst.Llamada llamada
    ) {
        List<TipoZ> tipos = resolverArgumentos(argumentos);
        ClaseInfo padre = claseActual.ast.padre().map(clases::get).orElse(null);
        if (padre == null) {
            error(posicion, "super requiere una clase padre");
            return TipoZ.desconocido();
        }
        if (padre.constructores.isEmpty() && tipos.isEmpty()) {
            if (llamada != null) {
                enlaces.registrarLlamada(llamada, padre.ast.nombre() + ".<init>()");
            }
            return TipoZ.voidTipo();
        }
        ZAst.Constructor seleccionado = seleccionarConstructor(padre.constructores, tipos);
        if (seleccionado == null) {
            error(posicion, "No existe un constructor padre compatible; especifique super(...)"
                    + firmaArgumentos(tipos));
        } else if (!jerarquia.accesible(seleccionado.visibilidad(),
                padre.ast.nombre(), claseActual.ast.nombre())) {
            error(posicion, "El constructor padre no es accesible");
        } else if (llamada != null) {
            enlaces.registrarLlamada(llamada, padre.ast.nombre() + "."
                    + firma("<init>", seleccionado.parametros()));
        }
        return TipoZ.voidTipo();
    }

    private static final class ClaseInfo {

        private final ZAst.Clase ast;

        private final Map<String, ZAst.Atributo>
                atributos;

        private final Map<String, List<ZAst.Metodo>>
                metodos;

        private final List<ZAst.Constructor>
                constructores;

        private ClaseInfo(
                ZAst.Clase ast
        ) {
            this.ast = ast;

            this.atributos =
                    new LinkedHashMap<>();

            this.metodos =
                    new LinkedHashMap<>();

            this.constructores =
                    new ArrayList<>();
        }
    }


    private enum ClaseTipo {
        INT,
        DOUBLE,
        CHAR,
        BOOLEAN,
        STRING,
        OBJETO,
        VOID,
        NULL,
        DESCONOCIDO
    }

    private record TipoZ(
            ClaseTipo clase,
            String nombreBase,
            int dimensiones
    ) {

        private TipoZ {
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

        private static TipoZ primitivo(
                ClaseTipo clase,
                String nombre
        ) {
            return new TipoZ(
                    clase,
                    nombre,
                    0
            );
        }

        private static TipoZ objeto(
                String nombre
        ) {
            return new TipoZ(
                    ClaseTipo.OBJETO,
                    nombre,
                    0
            );
        }

        private static TipoZ voidTipo() {
            return new TipoZ(
                    ClaseTipo.VOID,
                    "void",
                    0
            );
        }

        private static TipoZ nulo() {
            return new TipoZ(
                    ClaseTipo.NULL,
                    "null",
                    0
            );
        }

        private static TipoZ desconocido() {
            return desconocido(
                    "desconocido"
            );
        }

        private static TipoZ desconocido(
                String nombre
        ) {
            return new TipoZ(
                    ClaseTipo.DESCONOCIDO,
                    nombre,
                    0
            );
        }

        private boolean esArreglo() {
            return dimensiones > 0;
        }

        private boolean esVoid() {
            return clase
                    == ClaseTipo.VOID;
        }

        private boolean esNulo() {
            return clase
                    == ClaseTipo.NULL;
        }

        private boolean esDesconocido() {
            return clase
                    == ClaseTipo.DESCONOCIDO;
        }

        private boolean esBoolean() {
            return clase
                    == ClaseTipo.BOOLEAN
                    && dimensiones == 0;
        }

        private boolean esReferencia() {
            return esArreglo()
                    || clase == ClaseTipo.OBJETO
                    || clase == ClaseTipo.STRING;
        }

        private String declarado() {
            return nombreBase
                    + "[]".repeat(
                    dimensiones
            );
        }

        private TipoZ reducirArreglo() {
            if (dimensiones <= 0) {
                return desconocido();
            }

            return new TipoZ(
                    clase,
                    nombreBase,
                    dimensiones - 1
            );
        }
    }
}
