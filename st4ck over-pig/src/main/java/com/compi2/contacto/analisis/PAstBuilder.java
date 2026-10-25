package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.piglatin.PigLatinParser;
import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.antlr.v4.runtime.Token;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PAstBuilder {

    private final Path archivo;

    public PAstBuilder(
            Path archivo
    ) {
        this.archivo = Objects.requireNonNull(
                archivo,
                "El archivo es obligatorio"
        );
    }

    public ProgramaAst construir(
            PigLatinParser.ArchivoContext contexto
    ) {
        List<PAst.Importacion> importaciones =
                contexto.importacion()
                        .stream()
                        .map(this::construirImportacion)
                        .toList();

        List<PAst.Declaracion> globales =
                contexto.seccionVariables() == null
                        ? List.of()
                        : contexto.seccionVariables()
                        .declaracionGlobal()
                        .stream()
                        .map(this::construirDeclaracionGlobal)
                        .toList();

        List<PAst.Sentencia> principal =
                contexto.seccionPrincipal()
                        .sentencia()
                        .stream()
                        .map(this::construirSentencia)
                        .toList();

        PAst.Programa programaPig =
                new PAst.Programa(
                        importaciones,
                        globales,
                        principal,
                        posicion(contexto.getStart())
                );

        return new ProgramaAst(
                LenguajeFuente.PIG_LATIN,
                List.of(programaPig),
                posicion(contexto.getStart())
        );
    }

    private PAst.Importacion construirImportacion(
            PigLatinParser.ImportacionContext contexto
    ) {
        String ruta =
                contexto.rutaImportacion()
                        .getText();

        String extension =
                contexto.rutaImportacion()
                        .extensionImportacion()
                        .getText();

        return new PAst.Importacion(
                ruta,
                extension,
                posicion(contexto.getStart())
        );
    }

    private PAst.Declaracion construirDeclaracionGlobal(
            PigLatinParser.DeclaracionGlobalContext contexto
    ) {
        if (contexto.declaracionVariable() != null) {
            return construirDeclaracionVariable(
                    contexto.declaracionVariable()
            );
        }

        return construirDeclaracionArreglo(
                contexto.declaracionArreglo()
        );
    }

    private PAst.Declaracion construirDeclaracionVariable(
            PigLatinParser.DeclaracionVariableContext contexto
    ) {
        return construirDeclaracionVariable(
                contexto.IDENTIFICADOR().getText(),
                contexto.definicionVariable(),
                posicion(contexto.getStart())
        );
    }

    private PAst.Declaracion construirDeclaracionVariable(
            String nombre,
            PigLatinParser.DefinicionVariableContext contexto,
            PosicionFuente posicion
    ) {
        if (contexto.tipoDato() != null) {
            Optional<PAst.Inicializador> inicializador =
                    contexto.expresion() == null
                            ? Optional.empty()
                            : Optional.of(
                            construirExpresion(
                                    contexto.expresion()
                            )
                    );

            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(
                            contexto.tipoDato().getText(),
                            0,
                            posicion
                    ),
                    List.of(),
                    inicializador,
                    posicion
            );
        }

        if (contexto.BOOL_VERUM() != null
                || contexto.BOOL_FALSUS() != null) {

            boolean verdadero =
                    contexto.BOOL_VERUM() != null;

            PAst.Literal literal =
                    new PAst.Literal(
                            PAst.TipoLiteral.BOOLEANO,
                            verdadero
                                    ? contexto.BOOL_VERUM().getText()
                                    : contexto.BOOL_FALSUS().getText(),
                            posicion
                    );

            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(
                            "bool",
                            0,
                            posicion
                    ),
                    List.of(),
                    Optional.of(literal),
                    posicion
            );
        }

        if (contexto.KW_NULL() != null) {
            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(contexto.IDENTIFICADOR(0).getText(), 0, posicion),
                    List.of(),
                    Optional.of(new PAst.Literal(PAst.TipoLiteral.NULO, "null", posicion)),
                    posicion
            );
        }

        if (contexto.KW_NOVUS() != null) {
            String tipo = contexto.IDENTIFICADOR(0).getText();
            String tipoDeclarado = contexto.IDENTIFICADOR().size() > 1
                    ? contexto.IDENTIFICADOR(1).getText() : tipo;

            PAst.NuevoObjeto nuevo =
                    new PAst.NuevoObjeto(
                            tipo,
                            construirArgumentos(
                                    contexto.argumentos()
                            ),
                            posicion
                    );

            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(
                            tipoDeclarado,
                            0,
                            posicion
                    ),
                    List.of(),
                    Optional.of(nuevo),
                    posicion
            );
        }

        String tipo =
                contexto.IDENTIFICADOR(0)
                        .getText();

        Optional<PAst.Inicializador> inicializador =
                contexto.inicializadorCompuesto() == null
                        ? Optional.empty()
                        : Optional.of(
                        construirInicializadorCompuesto(
                                contexto.inicializadorCompuesto()
                        )
                );

        return new PAst.Declaracion(
                nombre,
                new PAst.Tipo(
                        tipo,
                        0,
                        posicion
                ),
                List.of(),
                inicializador,
                posicion
        );
    }

    private PAst.Declaracion construirDeclaracionArreglo(
            PigLatinParser.DeclaracionArregloContext contexto
    ) {
        List<PAst.Expresion> dimensiones =
                contexto.dimensionDeclaracion()
                        .stream()
                        .map(
                                dimension ->
                                        construirExpresion(
                                                dimension.expresion()
                                        )
                        )
                        .toList();

        String tipo =
                contexto.tipoDatoArreglo()
                        .getText();

        Optional<PAst.Inicializador> inicializador =
                contexto.inicializadorCompuesto() == null
                        ? Optional.empty()
                        : Optional.of(
                        construirInicializadorCompuesto(
                                contexto.inicializadorCompuesto()
                        )
                );

        return new PAst.Declaracion(
                contexto.IDENTIFICADOR()
                        .getText(),
                new PAst.Tipo(
                        tipo,
                        dimensiones.size(),
                        posicion(contexto.getStart())
                ),
                dimensiones,
                inicializador,
                posicion(contexto.getStart())
        );
    }

    private PAst.Sentencia construirSentencia(
            PigLatinParser.SentenciaContext contexto
    ) {
        if (contexto.declaracionVariable() != null) {
            return construirDeclaracionVariable(
                    contexto.declaracionVariable()
            );
        }

        if (contexto.declaracionArreglo() != null) {
            return construirDeclaracionArreglo(
                    contexto.declaracionArreglo()
            );
        }

        if (contexto.sentenciaSi() != null) {
            return construirSi(
                    contexto.sentenciaSi()
            );
        }

        if (contexto.sentenciaMientras() != null) {
            return construirMientras(
                    contexto.sentenciaMientras()
            );
        }

        if (contexto.sentenciaHacerMientras() != null) {
            return construirHacerMientras(
                    contexto.sentenciaHacerMientras()
            );
        }

        if (contexto.sentenciaPara() != null) {
            return construirPara(
                    contexto.sentenciaPara()
            );
        }

        if (contexto.sentenciaEntrada() != null) {
            return construirEntrada(
                    contexto.sentenciaEntrada()
            );
        }

        if (contexto.sentenciaSalida() != null) {
            return construirSalida(
                    contexto.sentenciaSalida()
            );
        }

        if (contexto.sentenciaInterrupcion() != null) {
            return construirInterrupcion(
                    contexto.sentenciaInterrupcion()
            );
        }

        PAst.Expresion expresion =
                construirExpresion(
                        contexto.sentenciaExpresion()
                                .expresion()
                );

        return new PAst.ExpresionSentencia(
                expresion,
                posicion(contexto.getStart())
        );
    }

    private PAst.Entrada construirEntrada(
            PigLatinParser.SentenciaEntradaContext contexto
    ) {
        Optional<PAst.Expresion> destino =
                contexto.accesoAsignable() == null
                        ? Optional.empty()
                        : Optional.of(
                        construirAccesoAsignable(
                                contexto.accesoAsignable()
                        )
                );

        return new PAst.Entrada(
                destino,
                posicion(contexto.getStart())
        );
    }

    private PAst.Salida construirSalida(
            PigLatinParser.SentenciaSalidaContext contexto
    ) {
        List<PAst.Expresion> expresiones =
                contexto.expresion()
                        .stream()
                        .map(this::construirExpresion)
                        .toList();

        return new PAst.Salida(
                expresiones,
                posicion(contexto.getStart())
        );
    }

    private PAst.Sentencia construirInterrupcion(
            PigLatinParser.SentenciaInterrupcionContext contexto
    ) {
        if (contexto.KW_PERGE() != null) {
            return new PAst.Continuar(
                    posicion(contexto.getStart())
            );
        }

        return new PAst.Romper(
                posicion(contexto.getStart())
        );
    }

    private PAst.Si construirSi(
            PigLatinParser.SentenciaSiContext contexto
    ) {
        List<PAst.RamaSi> ramas =
                new ArrayList<>();

        ramas.add(
                new PAst.RamaSi(
                        Optional.of(
                                construirExpresion(
                                        contexto.expresion()
                                )
                        ),
                        construirBloque(
                                contexto.bloque()
                        ),
                        posicion(contexto.getStart())
                )
        );

        for (PigLatinParser.RamaAliterContext rama
                : contexto.ramaAliter()) {

            Optional<PAst.Expresion> condicion =
                    rama.expresion() == null
                            ? Optional.empty()
                            : Optional.of(
                            construirExpresion(
                                    rama.expresion()
                            )
                    );

            ramas.add(
                    new PAst.RamaSi(
                            condicion,
                            construirBloque(
                                    rama.bloque()
                            ),
                            posicion(rama.getStart())
                    )
            );
        }

        return new PAst.Si(
                ramas,
                posicion(contexto.getStart())
        );
    }

    private PAst.Mientras construirMientras(
            PigLatinParser.SentenciaMientrasContext contexto
    ) {
        return new PAst.Mientras(
                construirExpresion(
                        contexto.expresion()
                ),
                construirBloque(
                        contexto.bloque()
                ),
                posicion(contexto.getStart())
        );
    }

    private PAst.HacerMientras construirHacerMientras(
            PigLatinParser.SentenciaHacerMientrasContext contexto
    ) {
        return new PAst.HacerMientras(
                construirBloque(
                        contexto.bloque()
                ),
                construirExpresion(
                        contexto.expresion()
                ),
                posicion(contexto.getStart())
        );
    }

    private PAst.Para construirPara(
            PigLatinParser.SentenciaParaContext contexto
    ) {
        Optional<NodoAst> inicializacion =
                Optional.empty();

        if (contexto.inicializacionPara() != null) {
            PigLatinParser.InicializacionParaContext inicio =
                    contexto.inicializacionPara();

            if (inicio.declaracionPara() != null) {
                inicializacion = Optional.of(
                        construirDeclaracionPara(
                                inicio.declaracionPara()
                        )
                );
            } else {
                inicializacion = Optional.of(
                        construirExpresion(
                                inicio.expresion()
                        )
                );
            }
        }

        Optional<PAst.Expresion> condicion =
                contexto.condicion == null
                        ? Optional.empty()
                        : Optional.of(
                        construirExpresion(
                                contexto.condicion
                        )
                );

        Optional<PAst.Expresion> actualizacion =
                contexto.actualizacion == null
                        ? Optional.empty()
                        : Optional.of(
                        construirExpresion(
                                contexto.actualizacion
                        )
                );

        return new PAst.Para(
                inicializacion,
                condicion,
                actualizacion,
                construirBloque(
                        contexto.bloque()
                ),
                posicion(contexto.getStart())
        );
    }

    private PAst.Declaracion construirDeclaracionPara(
            PigLatinParser.DeclaracionParaContext contexto
    ) {
        return construirDeclaracionPara(
                contexto.IDENTIFICADOR().getText(),
                contexto.definicionVariableSinFin(),
                posicion(contexto.getStart())
        );
    }

    private PAst.Declaracion construirDeclaracionPara(
            String nombre,
            PigLatinParser.DefinicionVariableSinFinContext contexto,
            PosicionFuente posicion
    ) {
        if (contexto.tipoDato() != null) {
            Optional<PAst.Inicializador> inicializador =
                    contexto.expresion() == null
                            ? Optional.empty()
                            : Optional.of(
                            construirExpresion(
                                    contexto.expresion()
                            )
                    );

            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(
                            contexto.tipoDato().getText(),
                            0,
                            posicion
                    ),
                    List.of(),
                    inicializador,
                    posicion
            );
        }

        if (contexto.BOOL_VERUM() != null
                || contexto.BOOL_FALSUS() != null) {

            boolean verdadero =
                    contexto.BOOL_VERUM() != null;

            PAst.Literal literal =
                    new PAst.Literal(
                            PAst.TipoLiteral.BOOLEANO,
                            verdadero
                                    ? contexto.BOOL_VERUM().getText()
                                    : contexto.BOOL_FALSUS().getText(),
                            posicion
                    );

            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(
                            "bool",
                            0,
                            posicion
                    ),
                    List.of(),
                    Optional.of(literal),
                    posicion
            );
        }

        if (contexto.KW_NULL() != null) {
            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(contexto.IDENTIFICADOR(0).getText(), 0, posicion),
                    List.of(),
                    Optional.of(new PAst.Literal(PAst.TipoLiteral.NULO, "null", posicion)),
                    posicion
            );
        }

        if (contexto.KW_NOVUS() != null) {
            String tipo = contexto.IDENTIFICADOR(0).getText();
            String tipoDeclarado = contexto.IDENTIFICADOR().size() > 1
                    ? contexto.IDENTIFICADOR(1).getText() : tipo;

            return new PAst.Declaracion(
                    nombre,
                    new PAst.Tipo(
                            tipoDeclarado,
                            0,
                            posicion
                    ),
                    List.of(),
                    Optional.of(
                            new PAst.NuevoObjeto(
                                    tipo,
                                    construirArgumentos(
                                            contexto.argumentos()
                                    ),
                                    posicion
                            )
                    ),
                    posicion
            );
        }

        String tipo =
                contexto.IDENTIFICADOR(0)
                        .getText();

        Optional<PAst.Inicializador> inicializador =
                contexto.inicializadorCompuesto() == null
                        ? Optional.empty()
                        : Optional.of(
                        construirInicializadorCompuesto(
                                contexto.inicializadorCompuesto()
                        )
                );

        return new PAst.Declaracion(
                nombre,
                new PAst.Tipo(
                        tipo,
                        0,
                        posicion
                ),
                List.of(),
                inicializador,
                posicion
        );
    }

    private List<PAst.Sentencia> construirBloque(
            PigLatinParser.BloqueContext contexto
    ) {
        return contexto.sentencia()
                .stream()
                .map(this::construirSentencia)
                .toList();
    }

    private PAst.InicializadorLista construirInicializadorCompuesto(
            PigLatinParser.InicializadorCompuestoContext contexto
    ) {
        List<PAst.Inicializador> valores =
                contexto.inicializador()
                        .stream()
                        .map(this::construirInicializador)
                        .toList();

        return new PAst.InicializadorLista(
                valores,
                posicion(contexto.getStart())
        );
    }

    private PAst.Inicializador construirInicializador(
            PigLatinParser.InicializadorContext contexto
    ) {
        if (contexto.expresion() != null) {
            return construirExpresion(
                    contexto.expresion()
            );
        }

        return construirInicializadorCompuesto(
                contexto.inicializadorCompuesto()
        );
    }

    private PAst.Expresion construirExpresion(
            PigLatinParser.ExpresionContext contexto
    ) {
        return construirAsignacion(
                contexto.expresionAsignacion()
        );
    }

    private PAst.Expresion construirAsignacion(
            PigLatinParser.ExpresionAsignacionContext contexto
    ) {
        PAst.Expresion izquierda =
                construirOr(
                        contexto.expresionOr()
                );

        if (contexto.expresionAsignacion() == null) {
            return izquierda;
        }

        return new PAst.Asignacion(
                izquierda,
                construirAsignacion(
                        contexto.expresionAsignacion()
                ),
                posicion(contexto.getStart())
        );
    }

    private PAst.Expresion construirOr(
            PigLatinParser.ExpresionOrContext contexto
    ) {
        List<PigLatinParser.ExpresionAndContext> operandos =
                contexto.expresionAnd();

        PAst.Expresion resultado =
                construirAnd(
                        operandos.get(0)
                );

        for (int i = 1; i < operandos.size(); i++) {
            resultado = new PAst.Binaria(
                    PAst.OperadorBinario.OR,
                    resultado,
                    construirAnd(
                            operandos.get(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirAnd(
            PigLatinParser.ExpresionAndContext contexto
    ) {
        List<PigLatinParser.ExpresionIgualdadContext> operandos =
                contexto.expresionIgualdad();

        PAst.Expresion resultado =
                construirIgualdad(
                        operandos.get(0)
                );

        for (int i = 1; i < operandos.size(); i++) {
            resultado = new PAst.Binaria(
                    PAst.OperadorBinario.AND,
                    resultado,
                    construirIgualdad(
                            operandos.get(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirIgualdad(
            PigLatinParser.ExpresionIgualdadContext contexto
    ) {
        List<PigLatinParser.ExpresionRelacionalContext> operandos =
                contexto.expresionRelacional();

        PAst.Expresion resultado =
                construirRelacional(
                        operandos.get(0)
                );

        for (int i = 1; i < operandos.size(); i++) {
            String operador =
                    contexto.getChild(
                            i * 2 - 1
                    ).getText();

            resultado = new PAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirRelacional(
                            operandos.get(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirRelacional(
            PigLatinParser.ExpresionRelacionalContext contexto
    ) {
        List<PigLatinParser.ExpresionAditivaContext> operandos =
                contexto.expresionAditiva();

        PAst.Expresion resultado =
                construirAditiva(
                        operandos.get(0)
                );

        for (int i = 1; i < operandos.size(); i++) {
            String operador =
                    contexto.getChild(
                            i * 2 - 1
                    ).getText();

            resultado = new PAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirAditiva(
                            operandos.get(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirAditiva(
            PigLatinParser.ExpresionAditivaContext contexto
    ) {
        List<PigLatinParser.ExpresionMultiplicativaContext> operandos =
                contexto.expresionMultiplicativa();

        PAst.Expresion resultado =
                construirMultiplicativa(
                        operandos.get(0)
                );

        for (int i = 1; i < operandos.size(); i++) {
            String operador =
                    contexto.getChild(
                            i * 2 - 1
                    ).getText();

            resultado = new PAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirMultiplicativa(
                            operandos.get(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirMultiplicativa(
            PigLatinParser.ExpresionMultiplicativaContext contexto
    ) {
        List<PigLatinParser.ExpresionUnariaContext> operandos =
                contexto.expresionUnaria();

        PAst.Expresion resultado =
                construirUnaria(
                        operandos.get(0)
                );

        for (int i = 1; i < operandos.size(); i++) {
            String operador =
                    contexto.getChild(
                            i * 2 - 1
                    ).getText();

            resultado = new PAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirUnaria(
                            operandos.get(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirUnaria(
            PigLatinParser.ExpresionUnariaContext contexto
    ) {
        if (contexto.expresionPostfija() != null) {
            return construirPostfija(
                    contexto.expresionPostfija()
            );
        }

        String operador =
                contexto.getChild(0)
                        .getText();

        return new PAst.Unaria(
                operadorUnario(operador),
                construirUnaria(
                        contexto.expresionUnaria()
                ),
                posicion(contexto.getStart())
        );
    }

    private PAst.Expresion construirPostfija(
            PigLatinParser.ExpresionPostfijaContext contexto
    ) {
        PAst.Expresion resultado =
                construirPrimaria(
                        contexto.primaria()
                );

        for (PigLatinParser.SufijoPostfijoContext sufijo
                : contexto.sufijoPostfijo()) {

            if (sufijo.argumentos() != null) {
                resultado = new PAst.Llamada(
                        resultado,
                        construirArgumentos(
                                sufijo.argumentos()
                        ),
                        posicion(sufijo.getStart())
                );

                continue;
            }

            if (sufijo.CORCHETE_ABRE() != null) {
                resultado = new PAst.AccesoArreglo(
                        resultado,
                        construirExpresion(
                                sufijo.expresion()
                        ),
                        posicion(sufijo.getStart())
                );

                continue;
            }

            resultado = new PAst.AccesoMiembro(
                    resultado,
                    sufijo.IDENTIFICADOR()
                            .getText(),
                    posicion(sufijo.getStart())
            );
        }

        if (contexto.INCREMENTO() != null) {
            resultado = new PAst.CambioPostfijo(
                    resultado,
                    PAst.OperacionPostfija.INCREMENTO,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.DECREMENTO() != null) {
            resultado = new PAst.CambioPostfijo(
                    resultado,
                    PAst.OperacionPostfija.DECREMENTO,
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private PAst.Expresion construirPrimaria(
            PigLatinParser.PrimariaContext contexto
    ) {
        if (contexto.ENTERO() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.ENTERO,
                    contexto.ENTERO().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.DECIMAL() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.DECIMAL,
                    contexto.DECIMAL().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.CADENA() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.CADENA,
                    contexto.CADENA().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.CARACTER() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.CARACTER,
                    contexto.CARACTER().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.BOOL_VERUM() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.BOOLEANO,
                    contexto.BOOL_VERUM().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.BOOL_FALSUS() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.BOOLEANO,
                    contexto.BOOL_FALSUS().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_NULL() != null) {
            return new PAst.Literal(
                    PAst.TipoLiteral.NULO,
                    "null",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.IDENTIFICADOR() != null) {
            return new PAst.Identificador(
                    contexto.IDENTIFICADOR()
                            .getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.creacionObjeto() != null) {
            return construirCreacionObjeto(
                    contexto.creacionObjeto()
            );
        }

        return construirExpresion(
                contexto.expresion()
        );
    }

    private PAst.NuevoObjeto construirCreacionObjeto(
            PigLatinParser.CreacionObjetoContext contexto
    ) {
        return new PAst.NuevoObjeto(
                contexto.IDENTIFICADOR()
                        .getText(),
                construirArgumentos(
                        contexto.argumentos()
                ),
                posicion(contexto.getStart())
        );
    }

    private List<PAst.Expresion> construirArgumentos(
            PigLatinParser.ArgumentosContext contexto
    ) {
        if (contexto == null
                || contexto.listaArgumentos() == null) {

            return List.of();
        }

        return contexto.listaArgumentos()
                .expresion()
                .stream()
                .map(this::construirExpresion)
                .toList();
    }

    private PAst.Expresion construirAccesoAsignable(
            PigLatinParser.AccesoAsignableContext contexto
    ) {
        PAst.Expresion resultado =
                new PAst.Identificador(
                        contexto.IDENTIFICADOR()
                                .getText(),
                        posicion(contexto.getStart())
                );

        for (PigLatinParser.SufijoAsignableContext sufijo
                : contexto.sufijoAsignable()) {

            if (sufijo.CORCHETE_ABRE() != null) {
                resultado = new PAst.AccesoArreglo(
                        resultado,
                        construirExpresion(
                                sufijo.expresion()
                        ),
                        posicion(sufijo.getStart())
                );

            } else {
                resultado = new PAst.AccesoMiembro(
                        resultado,
                        sufijo.IDENTIFICADOR()
                                .getText(),
                        posicion(sufijo.getStart())
                );
            }
        }

        return resultado;
    }

    private PAst.OperadorBinario operadorBinario(
            String operador
    ) {
        return switch (operador) {
            case "+" ->
                    PAst.OperadorBinario.SUMA;

            case "-" ->
                    PAst.OperadorBinario.RESTA;

            case "*" ->
                    PAst.OperadorBinario.MULTIPLICACION;

            case "/" ->
                    PAst.OperadorBinario.DIVISION;

            case "==" ->
                    PAst.OperadorBinario.IGUALDAD;

            case "!=" ->
                    PAst.OperadorBinario.DIFERENTE;

            case "<" ->
                    PAst.OperadorBinario.MENOR;

            case ">" ->
                    PAst.OperadorBinario.MAYOR;

            case "<=" ->
                    PAst.OperadorBinario.MENOR_IGUAL;

            case ">=" ->
                    PAst.OperadorBinario.MAYOR_IGUAL;

            case "&&" ->
                    PAst.OperadorBinario.AND;

            case "||" ->
                    PAst.OperadorBinario.OR;

            default ->
                    throw new IllegalArgumentException(
                            "Operador Pig Latin no soportado: "
                                    + operador
                    );
        };
    }

    private PAst.OperadorUnario operadorUnario(
            String operador
    ) {
        return switch (operador) {
            case "!" ->
                    PAst.OperadorUnario.NEGACION;

            case "+" ->
                    PAst.OperadorUnario.POSITIVO;

            case "-" ->
                    PAst.OperadorUnario.NEGATIVO;

            case "++" ->
                    PAst.OperadorUnario.INCREMENTO_PRE;

            case "--" ->
                    PAst.OperadorUnario.DECREMENTO_PRE;

            default ->
                    throw new IllegalArgumentException(
                            "Operador Pig Latin no soportado: "
                                    + operador
                    );
        };
    }

    private PosicionFuente posicion(
            Token token
    ) {
        return new PosicionFuente(
                archivo,
                Math.max(
                        token.getLine(),
                        1
                ),
                Math.max(
                        token.getCharPositionInLine(),
                        0
                )
        );
    }
}