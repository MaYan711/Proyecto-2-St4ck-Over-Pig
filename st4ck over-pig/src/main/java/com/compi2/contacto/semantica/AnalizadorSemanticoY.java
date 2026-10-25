package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.AccesoArregloAst;
import com.compi2.contacto.ast.AccesoAtributoAst;
import com.compi2.contacto.ast.AsignacionAst;
import com.compi2.contacto.ast.AtributoEstructuraAst;
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
import com.compi2.contacto.ast.ParametroAst;
import com.compi2.contacto.ast.ParaAst;
import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.RetornarAst;
import com.compi2.contacto.ast.RomperAst;
import com.compi2.contacto.ast.SentenciaAst;
import com.compi2.contacto.ast.SiAst;
import com.compi2.contacto.ast.TipoAst;
import com.compi2.contacto.ast.UnariaAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Optional;

public final class AnalizadorSemanticoY {

    private final TablaSimbolos tabla;
    private final List<Diagnostico> diagnosticos;

    private final Map<String, FuncionAst> funciones;
    private final Map<String, EstructuraAst> estructurasGlobales;

    private final Deque<Map<String, EstructuraAst>>
            estructurasLocales;

    private TipoResuelto tipoRetornoActual;

    private int profundidadCiclo;
    private int profundidadElegir;
    private int contadorAmbitos;

    private boolean huboRetorno;

    public AnalizadorSemanticoY() {
        tabla = new TablaSimbolos();
        diagnosticos = new ArrayList<>();

        funciones = new HashMap<>();
        estructurasGlobales = new HashMap<>();
        estructurasLocales = new ArrayDeque<>();

        tipoRetornoActual = TipoResuelto.voidTipo();

        profundidadCiclo = 0;
        profundidadElegir = 0;
        contadorAmbitos = 0;
    }

    public ResultadoSemanticoY analizar(
            List<ProgramaAst> programas
    ) {
        registrarEstructurasGlobales(programas);
        registrarFuncionesGlobales(programas);

        for (ProgramaAst programa : programas) {
            analizarPrograma(programa);
        }

        return new ResultadoSemanticoY(
                tabla,
                diagnosticos
        );
    }

    private void registrarEstructurasGlobales(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {
            for (NodoAst elemento : programa.elementos()) {
                if (!(elemento instanceof EstructuraAst estructura)) {
                    continue;
                }

                if (estructurasGlobales.containsKey(
                        estructura.nombre()
                )) {
                    error(
                            estructura.posicion(),
                            "La estructura '"
                                    + estructura.nombre()
                                    + "' ya fue declarada"
                    );

                    continue;
                }

                Simbolo simbolo = new Simbolo(
                        estructura.nombre(),
                        CategoriaSimbolo.ESTRUCTURA,
                        TipoDato.ESTRUCTURA,
                        estructura.nombre(),
                        estructura.posicion()
                );

                if (!tabla.declarar(simbolo)) {
                    error(
                            estructura.posicion(),
                            "El identificador '"
                                    + estructura.nombre()
                                    + "' ya existe en el ambito global"
                    );

                    continue;
                }

                estructurasGlobales.put(
                        estructura.nombre(),
                        estructura
                );
            }
        }
    }

    private void registrarFuncionesGlobales(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {
            for (NodoAst elemento : programa.elementos()) {
                if (!(elemento instanceof FuncionAst funcion)) {
                    continue;
                }

                if (funciones.containsKey(funcion.nombre())) {
                    error(
                            funcion.posicion(),
                            "La funcion '"
                                    + funcion.nombre()
                                    + "' ya fue declarada"
                    );

                    continue;
                }

                TipoResuelto retorno = funcion.retorno()
                        .map(this::resolverTipo)
                        .orElse(TipoResuelto.voidTipo());

                Simbolo simbolo = new Simbolo(
                        funcion.nombre(),
                        CategoriaSimbolo.FUNCION,
                        retorno.tipo(),
                        retorno.declarado(),
                        funcion.posicion()
                );

                if (!tabla.declarar(simbolo)) {
                    error(
                            funcion.posicion(),
                            "El identificador '"
                                    + funcion.nombre()
                                    + "' ya existe en el ambito global"
                    );

                    continue;
                }

                funciones.put(
                        funcion.nombre(),
                        funcion
                );
            }
        }
    }

    private void analizarPrograma(
            ProgramaAst programa
    ) {
        for (NodoAst elemento : programa.elementos()) {
            if (elemento instanceof EstructuraAst estructura) {
                analizarEstructura(estructura);
            }

            if (elemento instanceof FuncionAst funcion) {
                analizarFuncion(funcion);
            }
        }
    }

    private void analizarEstructura(
            EstructuraAst estructura
    ) {
        Set<String> atributos = new HashSet<>();

        for (AtributoEstructuraAst atributo
                : estructura.atributos()) {

            if (!atributos.add(atributo.nombre())) {
                error(
                        atributo.posicion(),
                        "El atributo '"
                                + atributo.nombre()
                                + "' esta repetido en la estructura '"
                                + estructura.nombre()
                                + "'"
                );
            }

            TipoResuelto tipo =
                    resolverTipo(atributo.tipo());

            if (tipo.tipo() == TipoDato.DESCONOCIDO) {
                error(
                        atributo.tipo().posicion(),
                        "El tipo '"
                                + atributo.tipo().nombre()
                                + "' no existe"
                );
            }

            for (ExpresionAst dimension
                    : atributo.dimensiones()) {

                OptionalLong valor =
                        evaluarConstanteEntera(dimension);

                if (valor.isEmpty()) {
                    error(
                            dimension.posicion(),
                            "La dimension de un arreglo dentro de una estructura debe ser constante"
                    );

                    continue;
                }

                if (valor.getAsLong() <= 0) {
                    error(
                            dimension.posicion(),
                            "La dimension de un arreglo debe ser mayor que cero"
                    );
                }
            }
        }
    }

    private void analizarFuncion(
            FuncionAst funcion
    ) {
        tabla.entrar(
                "funcion:"
                        + funcion.nombre()
        );

        estructurasLocales.push(
                new HashMap<>()
        );

        TipoResuelto retornoAnterior =
                tipoRetornoActual;

        boolean retornoAnteriorEncontrado =
                huboRetorno;

        tipoRetornoActual = funcion.retorno()
                .map(this::resolverTipo)
                .orElse(TipoResuelto.voidTipo());

        huboRetorno = false;

        declararParametros(funcion);

        analizarBloque(
                funcion.cuerpo(),
                false,
                "cuerpo-funcion"
        );

        if (tipoRetornoActual.tipo() != TipoDato.VOID
                && !huboRetorno) {

            error(
                    funcion.posicion(),
                    "La funcion '"
                            + funcion.nombre()
                            + "' declara retorno de tipo '"
                            + tipoRetornoActual.declarado()
                            + "' pero no contiene ninguna instruccion retornar"
            );
        }

        tipoRetornoActual = retornoAnterior;
        huboRetorno = retornoAnteriorEncontrado;

        estructurasLocales.pop();
        tabla.salir();
    }

    private void declararParametros(
            FuncionAst funcion
    ) {
        for (ParametroAst parametro
                : funcion.parametros()) {

            TipoResuelto base =
                    resolverTipo(parametro.tipo());

            if (base.tipo() == TipoDato.DESCONOCIDO) {
                error(
                        parametro.tipo().posicion(),
                        "El tipo '"
                                + parametro.tipo().nombre()
                                + "' no existe"
                );
            }

            TipoResuelto tipoParametro;

            if (parametro.modo()
                    == ParametroAst.Modo.REFERENCIA_ARREGLO) {

                tipoParametro = new TipoResuelto(
                        TipoDato.ARREGLO,
                        base.declarado() + "[]"
                );

            } else if (parametro.modo()
                    == ParametroAst.Modo.REFERENCIA_ESTRUCTURA) {

                tipoParametro = base;

                if (base.tipo() != TipoDato.ESTRUCTURA
                        && base.tipo() != TipoDato.DESCONOCIDO) {

                    error(
                            parametro.posicion(),
                            "El parametro '"
                                    + parametro.nombre()
                                    + "' usa {} pero su tipo no es una estructura"
                    );
                }

            } else {
                tipoParametro = base;

                if (base.tipo() == TipoDato.ESTRUCTURA) {
                    error(
                            parametro.posicion(),
                            "Las estructuras deben pasarse por referencia usando {}"
                    );
                }
            }

            Simbolo simbolo = new Simbolo(
                    parametro.nombre(),
                    CategoriaSimbolo.PARAMETRO,
                    tipoParametro.tipo(),
                    tipoParametro.declarado(),
                    parametro.posicion()
            );

            if (!tabla.declarar(simbolo)) {
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
            List<SentenciaAst> sentencias,
            boolean crearAmbito,
            String nombre
    ) {
        if (crearAmbito) {
            tabla.entrar(
                    nombre
                            + ":"
                            + contadorAmbitos++
            );

            estructurasLocales.push(
                    new HashMap<>()
            );
        }

        registrarEstructurasLocales(
                sentencias
        );

        for (SentenciaAst sentencia : sentencias) {
            analizarSentencia(sentencia);
        }

        if (crearAmbito) {
            estructurasLocales.pop();
            tabla.salir();
        }
    }

    private void registrarEstructurasLocales(
            List<SentenciaAst> sentencias
    ) {
        if (estructurasLocales.isEmpty()) {
            return;
        }

        Map<String, EstructuraAst> locales =
                estructurasLocales.peek();

        for (SentenciaAst sentencia : sentencias) {
            if (!(sentencia instanceof EstructuraAst estructura)) {
                continue;
            }

            if (locales.containsKey(
                    estructura.nombre()
            )) {
                error(
                        estructura.posicion(),
                        "La estructura local '"
                                + estructura.nombre()
                                + "' ya fue declarada"
                );

                continue;
            }

            Simbolo simbolo = new Simbolo(
                    estructura.nombre(),
                    CategoriaSimbolo.ESTRUCTURA,
                    TipoDato.ESTRUCTURA,
                    estructura.nombre(),
                    estructura.posicion()
            );

            if (!tabla.declarar(simbolo)) {
                error(
                        estructura.posicion(),
                        "El identificador '"
                                + estructura.nombre()
                                + "' ya existe en este ambito"
                );

                continue;
            }

            locales.put(
                    estructura.nombre(),
                    estructura
            );
        }
    }

    private void analizarSentencia(
            SentenciaAst sentencia
    ) {
        if (sentencia instanceof EstructuraAst estructura) {
            analizarEstructura(estructura);
            return;
        }

        if (sentencia instanceof DeclaracionVariableAst declaracion) {
            analizarDeclaracion(declaracion);
            return;
        }

        if (sentencia instanceof AsignacionAst asignacion) {
            analizarAsignacion(asignacion);
            return;
        }

        if (sentencia instanceof CambioUnidadAst cambio) {
            analizarCambioUnidad(cambio);
            return;
        }

        if (sentencia instanceof SiAst condicion) {
            analizarSi(condicion);
            return;
        }

        if (sentencia instanceof ElegirAst elegir) {
            analizarElegir(elegir);
            return;
        }

        if (sentencia instanceof ParaAst para) {
            analizarPara(para);
            return;
        }

        if (sentencia instanceof MientrasAst mientras) {
            analizarMientras(mientras);
            return;
        }

        if (sentencia instanceof HacerMientrasAst hacerMientras) {
            analizarHacerMientras(hacerMientras);
            return;
        }

        if (sentencia instanceof RetornarAst retornar) {
            analizarRetornar(retornar);
            return;
        }

        if (sentencia instanceof RomperAst romper) {
            analizarRomper(romper);
            return;
        }

        if (sentencia instanceof ContinuarAst continuar) {
            analizarContinuar(continuar);
            return;
        }

        if (sentencia instanceof ImprimirAst imprimir) {
            analizarImprimir(imprimir);
            return;
        }

        if (sentencia instanceof ExpresionSentenciaAst expresion) {
            resolverExpresion(
                    expresion.expresion()
            );
        }
    }

    private void analizarDeclaracion(
            DeclaracionVariableAst declaracion
    ) {
        TipoResuelto base =
                resolverTipo(
                        declaracion.tipo()
                );

        if (base.tipo() == TipoDato.DESCONOCIDO) {
            error(
                    declaracion.tipo().posicion(),
                    "El tipo '"
                            + declaracion.tipo().nombre()
                            + "' no existe"
            );
        }

        for (ExpresionAst dimension
                : declaracion.dimensiones()) {

            TipoResuelto tipoDimension =
                    resolverExpresion(dimension);

            if (tipoDimension.tipo() != TipoDato.ENTERO
                    && tipoDimension.tipo()
                    != TipoDato.DESCONOCIDO) {

                error(
                        dimension.posicion(),
                        "La dimension de un arreglo debe ser de tipo entero"
                );
            }

            OptionalLong constante =
                    evaluarConstanteEntera(dimension);

            if (constante.isPresent()
                    && constante.getAsLong() <= 0) {

                error(
                        dimension.posicion(),
                        "La dimension de un arreglo debe ser mayor que cero"
                );
            }
        }

        TipoResuelto tipoDeclarado =
                construirTipoDeclaracion(
                        base,
                        declaracion.dimensiones().size()
                );

        declaracion.inicializador()
                .ifPresent(valor ->
                        validarValorContraTipo(
                                tipoDeclarado,
                                valor,
                                declaracion.posicion()
                        )
                );

        Simbolo simbolo = new Simbolo(
                declaracion.nombre(),
                CategoriaSimbolo.VARIABLE,
                tipoDeclarado.tipo(),
                tipoDeclarado.declarado(),
                declaracion.posicion()
        );

        if (!tabla.declarar(simbolo)) {
            error(
                    declaracion.posicion(),
                    "La variable '"
                            + declaracion.nombre()
                            + "' ya fue declarada en este ambito"
            );
        }
    }

    private void analizarAsignacion(
            AsignacionAst asignacion
    ) {
        TipoResuelto destino =
                resolverExpresion(
                        asignacion.destino()
                );

        validarValorContraTipo(
                destino,
                asignacion.valor(),
                asignacion.posicion()
        );
    }

    private void analizarCambioUnidad(
            CambioUnidadAst cambio
    ) {
        TipoResuelto tipo =
                resolverExpresion(
                        cambio.destino()
                );

        if (!esNumerico(tipo.tipo())
                && tipo.tipo() != TipoDato.DESCONOCIDO) {

            error(
                    cambio.posicion(),
                    "Los operadores ++ y -- solo pueden aplicarse a valores numericos"
            );
        }
    }

    private void analizarSi(
            SiAst condicion
    ) {
        for (SiAst.Rama rama
                : condicion.ramas()) {

            exigirBooleano(
                    rama.condicion(),
                    "La condicion de si debe ser booleana"
            );

            analizarBloque(
                    rama.cuerpo(),
                    true,
                    "si"
            );
        }

        if (!condicion.contrario().isEmpty()) {
            analizarBloque(
                    condicion.contrario(),
                    true,
                    "contrario"
            );
        }
    }

    private void analizarElegir(
            ElegirAst elegir
    ) {
        TipoResuelto tipoPrincipal =
                resolverExpresion(
                        elegir.expresion()
                );

        if (tipoPrincipal.tipo() == TipoDato.ARREGLO
                || tipoPrincipal.tipo() == TipoDato.ESTRUCTURA
                || tipoPrincipal.tipo() == TipoDato.VOID) {

            error(
                    elegir.expresion().posicion(),
                    "La expresion de elegir debe ser un valor primitivo"
            );
        }

        profundidadElegir++;

        for (ElegirAst.Caso caso
                : elegir.casos()) {

            TipoResuelto tipoCaso =
                    resolverExpresion(
                            caso.valor()
                    );

            if (!sonComparables(
                    tipoPrincipal,
                    tipoCaso
            )) {
                error(
                        caso.posicion(),
                        "El tipo del caso no es compatible con la expresion de elegir"
                );
            }

            analizarBloque(
                    caso.cuerpo(),
                    true,
                    "caso"
            );
        }

        if (!elegir.siempre().isEmpty()) {
            analizarBloque(
                    elegir.siempre(),
                    true,
                    "siempre"
            );
        }

        profundidadElegir--;
    }

    private void analizarPara(
            ParaAst para
    ) {
        tabla.entrar(
                "para:"
                        + contadorAmbitos++
        );

        estructurasLocales.push(
                new HashMap<>()
        );

        analizarSentencia(
                para.inicializacion()
        );

        exigirBooleano(
                para.condicion(),
                "La condicion de para debe ser booleana"
        );

        profundidadCiclo++;

        analizarBloque(
                para.cuerpo(),
                true,
                "cuerpo-para"
        );

        analizarSentencia(
                para.actualizacion()
        );

        profundidadCiclo--;

        estructurasLocales.pop();
        tabla.salir();
    }

    private void analizarMientras(
            MientrasAst mientras
    ) {
        exigirBooleano(
                mientras.condicion(),
                "La condicion de mientras debe ser booleana"
        );

        profundidadCiclo++;

        analizarBloque(
                mientras.cuerpo(),
                true,
                "mientras"
        );

        profundidadCiclo--;
    }

    private void analizarHacerMientras(
            HacerMientrasAst hacerMientras
    ) {
        profundidadCiclo++;

        analizarBloque(
                hacerMientras.cuerpo(),
                true,
                "hacer"
        );

        profundidadCiclo--;

        exigirBooleano(
                hacerMientras.condicion(),
                "La condicion de hacer-mientras debe ser booleana"
        );
    }

    private void analizarRetornar(
            RetornarAst retornar
    ) {
        huboRetorno = true;

        if (tipoRetornoActual.tipo() == TipoDato.VOID) {
            error(
                    retornar.posicion(),
                    "Una funcion sin retorno no puede retornar un valor"
            );

            resolverExpresion(
                    retornar.expresion()
            );

            return;
        }

        TipoResuelto actual =
                resolverExpresion(
                        retornar.expresion()
                );

        if (!esCompatible(
                tipoRetornoActual,
                actual
        )) {
            error(
                    retornar.posicion(),
                    "El retorno es de tipo '"
                            + actual.declarado()
                            + "' pero la funcion requiere '"
                            + tipoRetornoActual.declarado()
                            + "'"
            );
        }
    }

    private void analizarRomper(
            RomperAst romper
    ) {
        if (profundidadCiclo == 0
                && profundidadElegir == 0) {

            error(
                    romper.posicion(),
                    "romper solo puede utilizarse dentro de un ciclo o elegir"
            );
        }
    }

    private void analizarContinuar(
            ContinuarAst continuar
    ) {
        if (profundidadCiclo == 0) {
            error(
                    continuar.posicion(),
                    "continuar solo puede utilizarse dentro de un ciclo"
            );
        }
    }

    private void analizarImprimir(
            ImprimirAst imprimir
    ) {
        TipoResuelto tipo =
                resolverExpresion(
                        imprimir.expresion()
                );

        if (tipo.tipo() == TipoDato.ARREGLO
                || tipo.tipo() == TipoDato.ESTRUCTURA
                || tipo.tipo() == TipoDato.VOID) {

            error(
                    imprimir.posicion(),
                    "imprimir requiere un valor primitivo"
            );
        }
    }

    private void exigirBooleano(
            ExpresionAst expresion,
            String mensaje
    ) {
        TipoResuelto tipo =
                resolverExpresion(expresion);

        if (tipo.tipo() != TipoDato.BOOLEANO
                && tipo.tipo() != TipoDato.DESCONOCIDO) {

            error(
                    expresion.posicion(),
                    mensaje
            );
        }
    }

    private TipoResuelto resolverExpresion(
            ExpresionAst expresion
    ) {
        if (expresion instanceof LiteralAst literal) {
            return resolverLiteral(literal);
        }

        if (expresion instanceof IdentificadorAst identificador) {
            return resolverIdentificador(
                    identificador
            );
        }

        if (expresion instanceof BinariaAst binaria) {
            return resolverBinaria(binaria);
        }

        if (expresion instanceof UnariaAst unaria) {
            return resolverUnaria(unaria);
        }

        if (expresion instanceof LeerAst) {
            return new TipoResuelto(
                    TipoDato.CADENA,
                    "cadena"
            );
        }

        if (expresion instanceof LlamadaAst llamada) {
            return resolverLlamada(llamada);
        }

        if (expresion instanceof AccesoArregloAst acceso) {
            return resolverAccesoArreglo(acceso);
        }

        if (expresion instanceof AccesoAtributoAst acceso) {
            return resolverAccesoAtributo(acceso);
        }

        if (expresion instanceof CambioUnidadExpresionAst cambio) {
            TipoResuelto tipo =
                    resolverExpresion(
                            cambio.objetivo()
                    );

            if (!esNumerico(tipo.tipo())
                    && tipo.tipo() != TipoDato.DESCONOCIDO) {

                error(
                        cambio.posicion(),
                        "Los operadores ++ y -- requieren un valor numerico"
                );
            }

            return tipo;
        }

        if (expresion instanceof InicializadorListaAst) {
            return TipoResuelto.desconocido();
        }

        return TipoResuelto.desconocido();
    }

    private TipoResuelto resolverLiteral(
            LiteralAst literal
    ) {
        return switch (literal.tipo()) {
            case ENTERO ->
                    new TipoResuelto(
                            TipoDato.ENTERO,
                            "entero"
                    );

            case DECIMAL ->
                    new TipoResuelto(
                            TipoDato.DECIMAL,
                            "flotante"
                    );

            case CADENA ->
                    new TipoResuelto(
                            TipoDato.CADENA,
                            "cadena"
                    );

            case CARACTER ->
                    new TipoResuelto(
                            TipoDato.CARACTER,
                            "caracter"
                    );

            case BOOLEANO ->
                    new TipoResuelto(
                            TipoDato.BOOLEANO,
                            "bool"
                    );
        };
    }

    private TipoResuelto resolverIdentificador(
            IdentificadorAst identificador
    ) {
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

            return TipoResuelto.desconocido();
        }

        Simbolo simbolo =
                encontrado.get();

        if (simbolo.categoria()
                == CategoriaSimbolo.FUNCION
                || simbolo.categoria()
                == CategoriaSimbolo.ESTRUCTURA) {

            error(
                    identificador.posicion(),
                    "El identificador '"
                            + identificador.nombre()
                            + "' no puede utilizarse como valor"
            );

            return TipoResuelto.desconocido();
        }

        return new TipoResuelto(
                simbolo.tipo(),
                simbolo.tipoDeclarado()
        );
    }

    private TipoResuelto resolverBinaria(
            BinariaAst binaria
    ) {
        TipoResuelto izquierda =
                resolverExpresion(
                        binaria.izquierda()
                );

        TipoResuelto derecha =
                resolverExpresion(
                        binaria.derecha()
                );

        switch (binaria.operador()) {

            case SUMA -> {
                if (izquierda.tipo() == TipoDato.CADENA
                        || derecha.tipo() == TipoDato.CADENA) {

                    if (esComplejo(izquierda.tipo())
                            || esComplejo(derecha.tipo())) {

                        error(
                                binaria.posicion(),
                                "No se puede concatenar un arreglo o estructura"
                        );

                        return TipoResuelto.desconocido();
                    }

                    return new TipoResuelto(
                            TipoDato.CADENA,
                            "cadena"
                    );
                }

                return resolverAritmetica(
                        binaria,
                        izquierda,
                        derecha
                );
            }

            case RESTA,
                 MULTIPLICACION,
                 DIVISION -> {

                return resolverAritmetica(
                        binaria,
                        izquierda,
                        derecha
                );
            }

            case MENOR,
                 MAYOR,
                 MENOR_IGUAL,
                 MAYOR_IGUAL -> {

                if (!esNumerico(izquierda.tipo())
                        || !esNumerico(derecha.tipo())) {

                    if (izquierda.tipo()
                            != TipoDato.DESCONOCIDO
                            && derecha.tipo()
                            != TipoDato.DESCONOCIDO) {

                        error(
                                binaria.posicion(),
                                "Los operadores relacionales requieren valores numericos"
                        );
                    }
                }

                return new TipoResuelto(
                        TipoDato.BOOLEANO,
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

                return new TipoResuelto(
                        TipoDato.BOOLEANO,
                        "bool"
                );
            }

            case AND,
                 OR -> {

                if (izquierda.tipo()
                        != TipoDato.BOOLEANO
                        || derecha.tipo()
                        != TipoDato.BOOLEANO) {

                    if (izquierda.tipo()
                            != TipoDato.DESCONOCIDO
                            && derecha.tipo()
                            != TipoDato.DESCONOCIDO) {

                        error(
                                binaria.posicion(),
                                "Los operadores logicos requieren valores booleanos"
                        );
                    }
                }

                return new TipoResuelto(
                        TipoDato.BOOLEANO,
                        "bool"
                );
            }
        }

        return TipoResuelto.desconocido();
    }

    private TipoResuelto resolverAritmetica(
            BinariaAst expresion,
            TipoResuelto izquierda,
            TipoResuelto derecha
    ) {
        if (!esNumerico(izquierda.tipo())
                || !esNumerico(derecha.tipo())) {

            if (izquierda.tipo()
                    != TipoDato.DESCONOCIDO
                    && derecha.tipo()
                    != TipoDato.DESCONOCIDO) {

                error(
                        expresion.posicion(),
                        "La operacion aritmetica requiere valores numericos"
                );
            }

            return TipoResuelto.desconocido();
        }

        if (izquierda.tipo() == TipoDato.DECIMAL
                || derecha.tipo() == TipoDato.DECIMAL) {

            return new TipoResuelto(
                    TipoDato.DECIMAL,
                    "flotante"
            );
        }

        if (izquierda.tipo() == TipoDato.ENTERO
                || derecha.tipo() == TipoDato.ENTERO) {

            return new TipoResuelto(
                    TipoDato.ENTERO,
                    "entero"
            );
        }

        return new TipoResuelto(
                TipoDato.CARACTER,
                "caracter"
        );
    }

    private TipoResuelto resolverUnaria(
            UnariaAst unaria
    ) {
        TipoResuelto tipo =
                resolverExpresion(
                        unaria.expresion()
                );

        if (unaria.operador()
                == UnariaAst.Operador.NEGACION) {

            if (tipo.tipo() != TipoDato.BOOLEANO
                    && tipo.tipo()
                    != TipoDato.DESCONOCIDO) {

                error(
                        unaria.posicion(),
                        "El operador ! requiere un valor booleano"
                );
            }

            return new TipoResuelto(
                    TipoDato.BOOLEANO,
                    "bool"
            );
        }

        if (!esNumerico(tipo.tipo())
                && tipo.tipo()
                != TipoDato.DESCONOCIDO) {

            error(
                    unaria.posicion(),
                    "Los operadores + y - unarios requieren un valor numerico"
            );
        }

        return tipo;
    }

    private TipoResuelto resolverLlamada(
            LlamadaAst llamada
    ) {
        if (!(llamada.objetivo()
                instanceof IdentificadorAst identificador)) {

            error(
                    llamada.posicion(),
                    "Y? solo permite llamadas a funciones globales"
            );

            for (ExpresionAst argumento
                    : llamada.argumentos()) {
                resolverExpresion(argumento);
            }

            return TipoResuelto.desconocido();
        }

        FuncionAst funcion =
                funciones.get(
                        identificador.nombre()
                );

        if (funcion == null) {
            error(
                    llamada.posicion(),
                    "La funcion '"
                            + identificador.nombre()
                            + "' no existe"
            );

            for (ExpresionAst argumento
                    : llamada.argumentos()) {
                resolverExpresion(argumento);
            }

            return TipoResuelto.desconocido();
        }

        if (funcion.parametros().size()
                != llamada.argumentos().size()) {

            error(
                    llamada.posicion(),
                    "La funcion '"
                            + funcion.nombre()
                            + "' requiere "
                            + funcion.parametros().size()
                            + " parametros pero se recibieron "
                            + llamada.argumentos().size()
            );
        }

        int cantidad = Math.min(
                funcion.parametros().size(),
                llamada.argumentos().size()
        );

        for (int indice = 0;
             indice < cantidad;
             indice++) {

            ParametroAst parametro =
                    funcion.parametros().get(indice);

            ExpresionAst argumento =
                    llamada.argumentos().get(indice);

            TipoResuelto esperado =
                    tipoParametro(parametro);

            TipoResuelto actual =
                    resolverExpresion(argumento);

            if (!compatibleParametro(
                    parametro,
                    esperado,
                    actual
            )) {
                error(
                        argumento.posicion(),
                        "El parametro '"
                                + parametro.nombre()
                                + "' requiere '"
                                + esperado.declarado()
                                + "' pero se recibio '"
                                + actual.declarado()
                                + "'"
                );
            }
        }

        for (int indice = cantidad;
             indice < llamada.argumentos().size();
             indice++) {

            resolverExpresion(
                    llamada.argumentos().get(indice)
            );
        }

        return funcion.retorno()
                .map(this::resolverTipo)
                .orElse(TipoResuelto.voidTipo());
    }

    private TipoResuelto resolverAccesoArreglo(
            AccesoArregloAst acceso
    ) {
        TipoResuelto objetivo =
                resolverExpresion(
                        acceso.objetivo()
                );

        TipoResuelto indice =
                resolverExpresion(
                        acceso.indice()
                );

        if (indice.tipo() != TipoDato.ENTERO
                && indice.tipo()
                != TipoDato.DESCONOCIDO) {

            error(
                    acceso.indice().posicion(),
                    "El indice de un arreglo debe ser entero"
            );
        }

        if (objetivo.tipo() != TipoDato.ARREGLO) {
            if (objetivo.tipo()
                    != TipoDato.DESCONOCIDO) {

                error(
                        acceso.posicion(),
                        "Se intento indexar un valor que no es un arreglo"
                );
            }

            return TipoResuelto.desconocido();
        }

        return reducirArreglo(objetivo);
    }

    private TipoResuelto resolverAccesoAtributo(
            AccesoAtributoAst acceso
    ) {
        TipoResuelto objetivo =
                resolverExpresion(
                        acceso.objetivo()
                );

        if (objetivo.tipo()
                != TipoDato.ESTRUCTURA) {

            if (objetivo.tipo()
                    != TipoDato.DESCONOCIDO) {

                error(
                        acceso.posicion(),
                        "Solo una estructura puede acceder a atributos"
                );
            }

            return TipoResuelto.desconocido();
        }

        EstructuraAst estructura =
                buscarEstructura(
                        objetivo.declarado()
                );

        if (estructura == null) {
            error(
                    acceso.posicion(),
                    "No se encontro la estructura '"
                            + objetivo.declarado()
                            + "'"
            );

            return TipoResuelto.desconocido();
        }

        for (AtributoEstructuraAst atributo
                : estructura.atributos()) {

            if (!atributo.nombre()
                    .equals(acceso.atributo())) {
                continue;
            }

            TipoResuelto base =
                    resolverTipo(
                            atributo.tipo()
                    );

            return construirTipoDeclaracion(
                    base,
                    atributo.dimensiones().size()
            );
        }

        error(
                acceso.posicion(),
                "La estructura '"
                        + estructura.nombre()
                        + "' no contiene el atributo '"
                        + acceso.atributo()
                        + "'"
        );

        return TipoResuelto.desconocido();
    }

    private void validarValorContraTipo(
            TipoResuelto esperado,
            ExpresionAst valor,
            PosicionFuente posicion
    ) {
        if (esperado.tipo()
                == TipoDato.DESCONOCIDO) {

            resolverExpresion(valor);
            return;
        }

        if (valor instanceof InicializadorListaAst lista) {
            validarInicializadorLista(
                    esperado,
                    lista
            );

            return;
        }

        TipoResuelto actual =
                resolverExpresion(valor);

        if (!esCompatible(
                esperado,
                actual
        )) {
            error(
                    posicion,
                    "No se puede asignar un valor de tipo '"
                            + actual.declarado()
                            + "' a un valor de tipo '"
                            + esperado.declarado()
                            + "'"
            );
        }
    }

    private void validarInicializadorLista(
            TipoResuelto esperado,
            InicializadorListaAst lista
    ) {
        if (esperado.tipo()
                == TipoDato.ARREGLO) {

            TipoResuelto elemento =
                    reducirArreglo(esperado);

            for (ExpresionAst valor
                    : lista.valores()) {

                validarValorContraTipo(
                        elemento,
                        valor,
                        valor.posicion()
                );
            }

            return;
        }

        if (esperado.tipo()
                == TipoDato.ESTRUCTURA) {

            EstructuraAst estructura =
                    buscarEstructura(
                            esperado.declarado()
                    );

            if (estructura == null) {
                error(
                        lista.posicion(),
                        "La estructura '"
                                + esperado.declarado()
                                + "' no existe"
                );

                return;
            }

            if (estructura.atributos().size()
                    != lista.valores().size()) {

                error(
                        lista.posicion(),
                        "La estructura '"
                                + estructura.nombre()
                                + "' requiere "
                                + estructura.atributos().size()
                                + " valores pero se recibieron "
                                + lista.valores().size()
                );
            }

            int cantidad = Math.min(
                    estructura.atributos().size(),
                    lista.valores().size()
            );

            for (int indice = 0;
                 indice < cantidad;
                 indice++) {

                AtributoEstructuraAst atributo =
                        estructura.atributos().get(indice);

                TipoResuelto tipoAtributo =
                        construirTipoDeclaracion(
                                resolverTipo(
                                        atributo.tipo()
                                ),
                                atributo.dimensiones().size()
                        );

                ExpresionAst valor =
                        lista.valores().get(indice);

                validarValorContraTipo(
                        tipoAtributo,
                        valor,
                        valor.posicion()
                );
            }

            return;
        }

        error(
                lista.posicion(),
                "Un inicializador con {} solo puede utilizarse para arreglos o estructuras"
        );
    }

    private TipoResuelto tipoParametro(
            ParametroAst parametro
    ) {
        TipoResuelto base =
                resolverTipo(
                        parametro.tipo()
                );

        if (parametro.modo()
                == ParametroAst.Modo.REFERENCIA_ARREGLO) {

            return new TipoResuelto(
                    TipoDato.ARREGLO,
                    base.declarado() + "[]"
            );
        }

        return base;
    }

    private boolean compatibleParametro(
            ParametroAst parametro,
            TipoResuelto esperado,
            TipoResuelto actual
    ) {
        if (parametro.modo()
                == ParametroAst.Modo.REFERENCIA_ARREGLO) {

            if (actual.tipo()
                    != TipoDato.ARREGLO) {
                return false;
            }

            return baseArreglo(
                    esperado.declarado()
            ).equals(
                    baseArreglo(
                            actual.declarado()
                    )
            );
        }

        return esCompatible(
                esperado,
                actual
        );
    }

    private TipoResuelto construirTipoDeclaracion(
            TipoResuelto base,
            int dimensiones
    ) {
        if (dimensiones <= 0) {
            return base;
        }

        return new TipoResuelto(
                TipoDato.ARREGLO,
                base.declarado()
                        + "[]".repeat(dimensiones)
        );
    }

    private TipoResuelto reducirArreglo(
            TipoResuelto arreglo
    ) {
        String declarado =
                arreglo.declarado();

        if (!declarado.endsWith("[]")) {
            return TipoResuelto.desconocido();
        }

        String reducido =
                declarado.substring(
                        0,
                        declarado.length() - 2
                );

        if (reducido.endsWith("[]")) {
            return new TipoResuelto(
                    TipoDato.ARREGLO,
                    reducido
            );
        }

        return resolverTipoNombre(
                reducido
        );
    }

    private String baseArreglo(
            String nombre
    ) {
        String resultado = nombre;

        while (resultado.endsWith("[]")) {
            resultado = resultado.substring(
                    0,
                    resultado.length() - 2
            );
        }

        return resultado;
    }

    private TipoResuelto resolverTipo(
            TipoAst tipo
    ) {
        return resolverTipoNombre(
                tipo.nombre()
        );
    }

    private TipoResuelto resolverTipoNombre(
            String nombre
    ) {
        return switch (nombre) {
            case "entero" ->
                    new TipoResuelto(
                            TipoDato.ENTERO,
                            "entero"
                    );

            case "flotante" ->
                    new TipoResuelto(
                            TipoDato.DECIMAL,
                            "flotante"
                    );

            case "cadena" ->
                    new TipoResuelto(
                            TipoDato.CADENA,
                            "cadena"
                    );

            case "caracter" ->
                    new TipoResuelto(
                            TipoDato.CARACTER,
                            "caracter"
                    );

            case "bool" ->
                    new TipoResuelto(
                            TipoDato.BOOLEANO,
                            "bool"
                    );

            case "void" ->
                    TipoResuelto.voidTipo();

            default -> {
                EstructuraAst estructura =
                        buscarEstructura(nombre);

                if (estructura != null) {
                    yield new TipoResuelto(
                            TipoDato.ESTRUCTURA,
                            nombre
                    );
                }

                yield TipoResuelto.desconocido(nombre);
            }
        };
    }

    private EstructuraAst buscarEstructura(
            String nombre
    ) {
        for (Map<String, EstructuraAst> ambito
                : estructurasLocales) {

            EstructuraAst estructura =
                    ambito.get(nombre);

            if (estructura != null) {
                return estructura;
            }
        }

        return estructurasGlobales.get(nombre);
    }

    private boolean esCompatible(
            TipoResuelto destino,
            TipoResuelto origen
    ) {
        if (destino.tipo() == TipoDato.DESCONOCIDO
                || origen.tipo()
                == TipoDato.DESCONOCIDO) {

            return true;
        }

        if (destino.tipo() == TipoDato.ARREGLO
                || origen.tipo()
                == TipoDato.ARREGLO) {

            return destino.tipo()
                    == TipoDato.ARREGLO
                    && origen.tipo()
                    == TipoDato.ARREGLO
                    && destino.declarado()
                    .equals(origen.declarado());
        }

        if (destino.tipo() == TipoDato.ESTRUCTURA
                || origen.tipo()
                == TipoDato.ESTRUCTURA) {

            return destino.tipo()
                    == TipoDato.ESTRUCTURA
                    && origen.tipo()
                    == TipoDato.ESTRUCTURA
                    && destino.declarado()
                    .equals(origen.declarado());
        }

        if (destino.tipo()
                == origen.tipo()) {
            return true;
        }

        if (esNumerico(destino.tipo())
                && esNumerico(origen.tipo())) {

            return rangoNumerico(
                    destino.tipo()
            ) >= rangoNumerico(
                    origen.tipo()
            );
        }

        return false;
    }

    private boolean sonComparables(
            TipoResuelto primero,
            TipoResuelto segundo
    ) {
        if (primero.tipo() == TipoDato.DESCONOCIDO
                || segundo.tipo()
                == TipoDato.DESCONOCIDO) {

            return true;
        }

        if (esComplejo(primero.tipo())
                || esComplejo(segundo.tipo())) {
            return false;
        }

        if (esNumerico(primero.tipo())
                && esNumerico(segundo.tipo())) {
            return true;
        }

        return primero.tipo()
                == segundo.tipo();
    }

    private boolean esNumerico(
            TipoDato tipo
    ) {
        return tipo == TipoDato.ENTERO
                || tipo == TipoDato.DECIMAL
                || tipo == TipoDato.CARACTER;
    }

    private int rangoNumerico(
            TipoDato tipo
    ) {
        return switch (tipo) {
            case CARACTER -> 1;
            case ENTERO -> 2;
            case DECIMAL -> 3;
            default -> -1;
        };
    }

    private boolean esComplejo(
            TipoDato tipo
    ) {
        return tipo == TipoDato.ARREGLO
                || tipo == TipoDato.ESTRUCTURA
                || tipo == TipoDato.OBJETO
                || tipo == TipoDato.VOID;
    }

    private OptionalLong evaluarConstanteEntera(
            ExpresionAst expresion
    ) {
        if (expresion instanceof LiteralAst literal) {
            if (literal.tipo()
                    != LiteralAst.Tipo.ENTERO) {

                return OptionalLong.empty();
            }

            try {
                return OptionalLong.of(
                        Long.parseLong(
                                literal.lexema()
                        )
                );
            } catch (NumberFormatException excepcion) {
                return OptionalLong.empty();
            }
        }

        if (expresion instanceof UnariaAst unaria) {
            OptionalLong valor =
                    evaluarConstanteEntera(
                            unaria.expresion()
                    );

            if (valor.isEmpty()) {
                return OptionalLong.empty();
            }

            return switch (unaria.operador()) {
                case POSITIVO ->
                        valor;

                case NEGATIVO ->
                        OptionalLong.of(
                                -valor.getAsLong()
                        );

                case NEGACION ->
                        OptionalLong.empty();
            };
        }

        if (expresion instanceof BinariaAst binaria) {
            OptionalLong izquierda =
                    evaluarConstanteEntera(
                            binaria.izquierda()
                    );

            OptionalLong derecha =
                    evaluarConstanteEntera(
                            binaria.derecha()
                    );

            if (izquierda.isEmpty()
                    || derecha.isEmpty()) {
                return OptionalLong.empty();
            }

            long a = izquierda.getAsLong();
            long b = derecha.getAsLong();

            return switch (binaria.operador()) {
                case SUMA ->
                        OptionalLong.of(a + b);

                case RESTA ->
                        OptionalLong.of(a - b);

                case MULTIPLICACION ->
                        OptionalLong.of(a * b);

                case DIVISION -> {
                    if (b == 0) {
                        yield OptionalLong.empty();
                    }

                    yield OptionalLong.of(
                            a / b
                    );
                }

                default ->
                        OptionalLong.empty();
            };
        }

        return OptionalLong.empty();
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

    private record TipoResuelto(
            TipoDato tipo,
            String declarado
    ) {

        private static TipoResuelto desconocido() {
            return new TipoResuelto(
                    TipoDato.DESCONOCIDO,
                    "desconocido"
            );
        }

        private static TipoResuelto desconocido(
                String nombre
        ) {
            return new TipoResuelto(
                    TipoDato.DESCONOCIDO,
                    nombre
            );
        }

        private static TipoResuelto voidTipo() {
            return new TipoResuelto(
                    TipoDato.VOID,
                    "void"
            );
        }
    }
}