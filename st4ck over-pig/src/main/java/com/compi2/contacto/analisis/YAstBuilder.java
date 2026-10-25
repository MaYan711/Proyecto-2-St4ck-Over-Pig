package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.y.YParser;
import com.compi2.contacto.antlr.y.YParserBaseVisitor;
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
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.antlr.v4.runtime.Token;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class YAstBuilder extends YParserBaseVisitor<NodoAst> {

    private final Path archivo;

    public YAstBuilder(Path archivo) {
        this.archivo = Objects.requireNonNull(
                archivo,
                "El archivo es obligatorio"
        );
    }

    public ProgramaAst construir(
            YParser.ArchivoContext contexto
    ) {
        return (ProgramaAst) visitArchivo(contexto);
    }

    @Override
    public NodoAst visitArchivo(
            YParser.ArchivoContext contexto
    ) {
        List<NodoAst> elementos = new ArrayList<>();

        if (contexto.seccionEstructuras() != null) {
            for (YParser.DefinicionEstructuraContext estructura
                    : contexto.seccionEstructuras()
                    .definicionEstructura()) {

                elementos.add(
                        construirEstructura(estructura)
                );
            }
        }

        for (YParser.DefinicionFuncionContext funcion
                : contexto.seccionFunciones()
                .definicionFuncion()) {

            elementos.add(
                    construirFuncion(funcion)
            );
        }

        return new ProgramaAst(
                LenguajeFuente.Y,
                elementos,
                posicion(contexto.getStart())
        );
    }

    private EstructuraAst construirEstructura(
            YParser.DefinicionEstructuraContext contexto
    ) {
        List<AtributoEstructuraAst> atributos =
                new ArrayList<>();

        for (YParser.AtributoEstructuraContext atributo
                : contexto.atributoEstructura()) {

            atributos.add(
                    construirAtributo(atributo)
            );
        }

        return new EstructuraAst(
                contexto.IDENTIFICADOR().getText(),
                atributos,
                posicion(contexto.getStart())
        );
    }

    private AtributoEstructuraAst construirAtributo(
            YParser.AtributoEstructuraContext contexto
    ) {
        List<ExpresionAst> dimensiones =
                construirDimensiones(
                        contexto.dimensionArreglo()
                );

        return new AtributoEstructuraAst(
                contexto.IDENTIFICADOR().getText(),
                construirTipo(contexto.tipoDato()),
                dimensiones,
                posicion(contexto.getStart())
        );
    }

    private FuncionAst construirFuncion(
            YParser.DefinicionFuncionContext contexto
    ) {
        List<ParametroAst> parametros =
                new ArrayList<>();

        if (contexto.listaParametros() != null) {
            for (YParser.ParametroContext parametro
                    : contexto.listaParametros().parametro()) {

                parametros.add(
                        construirParametro(parametro)
                );
            }
        }

        Optional<TipoAst> retorno = Optional.empty();

        if (contexto.retornoFuncion() != null) {
            retorno = Optional.of(
                    construirTipo(
                            contexto.retornoFuncion().tipoDato()
                    )
            );
        }

        return new FuncionAst(
                contexto.IDENTIFICADOR().getText(),
                parametros,
                retorno,
                construirBloque(contexto.bloque()),
                posicion(contexto.getStart())
        );
    }

    private ParametroAst construirParametro(
            YParser.ParametroContext contexto
    ) {
        if (contexto.parametroValor() != null) {
            YParser.ParametroValorContext parametro =
                    contexto.parametroValor();

            return new ParametroAst(
                    parametro.IDENTIFICADOR().getText(),
                    construirTipo(parametro.tipoDato()),
                    ParametroAst.Modo.VALOR,
                    posicion(parametro.getStart())
            );
        }

        if (contexto.parametroArreglo() != null) {
            YParser.ParametroArregloContext parametro =
                    contexto.parametroArreglo();

            return new ParametroAst(
                    parametro.IDENTIFICADOR().getText(),
                    construirTipo(parametro.tipoDato()),
                    ParametroAst.Modo.REFERENCIA_ARREGLO,
                    posicion(parametro.getStart())
            );
        }

        YParser.ParametroEstructuraContext parametro =
                contexto.parametroEstructura();

        return new ParametroAst(
                parametro.IDENTIFICADOR().getText(),
                construirTipo(parametro.tipoDato()),
                ParametroAst.Modo.REFERENCIA_ESTRUCTURA,
                posicion(parametro.getStart())
        );
    }

    private TipoAst construirTipo(
            YParser.TipoDatoContext contexto
    ) {
        return new TipoAst(
                contexto.getText(),
                posicion(contexto.getStart())
        );
    }

    private List<ExpresionAst> construirDimensiones(
            List<YParser.DimensionArregloContext> contextos
    ) {
        List<ExpresionAst> dimensiones =
                new ArrayList<>();

        for (YParser.DimensionArregloContext dimension
                : contextos) {

            dimensiones.add(
                    construirExpresion(
                            dimension.expresion()
                    )
            );
        }

        return dimensiones;
    }

    private List<SentenciaAst> construirBloque(
            YParser.BloqueContext contexto
    ) {
        List<SentenciaAst> sentencias =
                new ArrayList<>();

        for (YParser.SentenciaContext sentencia
                : contexto.sentencia()) {

            sentencias.add(
                    construirSentencia(sentencia)
            );
        }

        return sentencias;
    }

    private SentenciaAst construirSentencia(
            YParser.SentenciaContext contexto
    ) {
        if (contexto.definicionEstructura() != null) {
            return construirEstructura(
                    contexto.definicionEstructura()
            );
        }

        if (contexto.sentenciaSi() != null) {
            return construirSi(
                    contexto.sentenciaSi()
            );
        }

        if (contexto.sentenciaElegir() != null) {
            return construirElegir(
                    contexto.sentenciaElegir()
            );
        }

        if (contexto.sentenciaPara() != null) {
            return construirPara(
                    contexto.sentenciaPara()
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

        return construirSentenciaSimple(
                contexto.sentenciaSimple()
        );
    }

    private SentenciaAst construirSentenciaSimple(
            YParser.SentenciaSimpleContext contexto
    ) {
        if (contexto.declaracionVariable() != null) {
            return construirDeclaracion(
                    contexto.declaracionVariable()
            );
        }

        if (contexto.asignacion() != null) {
            return construirAsignacion(
                    contexto.asignacion()
            );
        }

        if (contexto.incrementoDecremento() != null) {
            return construirCambioUnidad(
                    contexto.incrementoDecremento()
            );
        }

        if (contexto.llamadaFuncion() != null) {
            LlamadaAst llamada = construirLlamadaFuncion(
                    contexto.llamadaFuncion()
            );

            return new ExpresionSentenciaAst(
                    llamada,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.llamadaLeer() != null) {
            LeerAst leer = new LeerAst(
                    posicion(
                            contexto.llamadaLeer().getStart()
                    )
            );

            return new ExpresionSentenciaAst(
                    leer,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.sentenciaImprimir() != null) {
            return new ImprimirAst(
                    construirExpresion(
                            contexto.sentenciaImprimir()
                                    .expresion()
                    ),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.sentenciaRetornar() != null) {
            return new RetornarAst(
                    construirExpresion(
                            contexto.sentenciaRetornar()
                                    .expresion()
                    ),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_ROMPER() != null) {
            return new RomperAst(
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_CONTINUAR() != null) {
            return new ContinuarAst(
                    posicion(contexto.getStart())
            );
        }

        throw new IllegalStateException(
                "Sentencia Y? no reconocida: "
                        + contexto.getText()
        );
    }

    private DeclaracionVariableAst construirDeclaracion(
            YParser.DeclaracionVariableContext contexto
    ) {
        Optional<ExpresionAst> inicializador =
                Optional.empty();

        if (contexto.inicializador() != null) {
            inicializador = Optional.of(
                    construirInicializador(
                            contexto.inicializador()
                    )
            );
        }

        return new DeclaracionVariableAst(
                contexto.IDENTIFICADOR().getText(),
                construirTipo(contexto.tipoDato()),
                construirDimensiones(
                        contexto.dimensionArreglo()
                ),
                inicializador,
                posicion(contexto.getStart())
        );
    }

    private AsignacionAst construirAsignacion(
            YParser.AsignacionContext contexto
    ) {
        return new AsignacionAst(
                construirAccesoAsignable(
                        contexto.accesoAsignable()
                ),
                construirInicializador(
                        contexto.inicializador()
                ),
                posicion(contexto.getStart())
        );
    }

    private CambioUnidadAst construirCambioUnidad(
            YParser.IncrementoDecrementoContext contexto
    ) {
        CambioUnidadAst.Operacion operacion =
                contexto.INCREMENTO() != null
                        ? CambioUnidadAst.Operacion.INCREMENTO
                        : CambioUnidadAst.Operacion.DECREMENTO;

        return new CambioUnidadAst(
                construirAccesoAsignable(
                        contexto.accesoAsignable()
                ),
                operacion,
                posicion(contexto.getStart())
        );
    }

    private ExpresionAst construirAccesoAsignable(
            YParser.AccesoAsignableContext contexto
    ) {
        ExpresionAst resultado =
                new IdentificadorAst(
                        contexto.IDENTIFICADOR().getText(),
                        posicion(contexto.getStart())
                );

        for (YParser.SufijoAsignableContext sufijo
                : contexto.sufijoAsignable()) {

            if (sufijo.CORCHETE_ABRE() != null) {
                resultado = new AccesoArregloAst(
                        resultado,
                        construirExpresion(
                                sufijo.expresion()
                        ),
                        posicion(sufijo.getStart())
                );
            } else {
                resultado = new AccesoAtributoAst(
                        resultado,
                        sufijo.IDENTIFICADOR().getText(),
                        posicion(sufijo.getStart())
                );
            }
        }

        return resultado;
    }

    private ExpresionAst construirInicializador(
            YParser.InicializadorContext contexto
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

    private InicializadorListaAst construirInicializadorCompuesto(
            YParser.InicializadorCompuestoContext contexto
    ) {
        List<ExpresionAst> valores =
                new ArrayList<>();

        for (YParser.InicializadorContext valor
                : contexto.inicializador()) {

            valores.add(
                    construirInicializador(valor)
            );
        }

        return new InicializadorListaAst(
                valores,
                posicion(contexto.getStart())
        );
    }

    private SiAst construirSi(
            YParser.SentenciaSiContext contexto
    ) {
        List<SiAst.Rama> ramas = new ArrayList<>();

        ramas.add(
                new SiAst.Rama(
                        construirExpresion(
                                contexto.expresion()
                        ),
                        construirBloque(
                                contexto.bloque()
                        ),
                        posicion(contexto.getStart())
                )
        );

        for (YParser.RamaSinoContext rama
                : contexto.ramaSino()) {

            ramas.add(
                    new SiAst.Rama(
                            construirExpresion(
                                    rama.expresion()
                            ),
                            construirBloque(
                                    rama.bloque()
                            ),
                            posicion(rama.getStart())
                    )
            );
        }

        List<SentenciaAst> contrario =
                new ArrayList<>();

        if (contexto.ramaContrario() != null) {
            contrario.addAll(
                    construirBloque(
                            contexto.ramaContrario()
                                    .bloque()
                    )
            );
        }

        return new SiAst(
                ramas,
                contrario,
                posicion(contexto.getStart())
        );
    }

    private ElegirAst construirElegir(
            YParser.SentenciaElegirContext contexto
    ) {
        List<ElegirAst.Caso> casos =
                new ArrayList<>();

        for (YParser.RamaCasoContext caso
                : contexto.ramaCaso()) {

            casos.add(
                    new ElegirAst.Caso(
                            construirExpresion(
                                    caso.expresion()
                            ),
                            construirBloque(
                                    caso.bloque()
                            ),
                            posicion(caso.getStart())
                    )
            );
        }

        List<SentenciaAst> siempre =
                new ArrayList<>();

        if (contexto.ramaSiempre() != null) {
            siempre.addAll(
                    construirBloque(
                            contexto.ramaSiempre()
                                    .bloque()
                    )
            );
        }

        return new ElegirAst(
                construirExpresion(
                        contexto.expresion()
                ),
                casos,
                siempre,
                posicion(contexto.getStart())
        );
    }

    private ParaAst construirPara(
            YParser.SentenciaParaContext contexto
    ) {
        SentenciaAst inicializacion =
                construirInicializacionPara(
                        contexto.inicializacionPara()
                );

        SentenciaAst actualizacion =
                construirActualizacionPara(
                        contexto.actualizacionPara()
                );

        return new ParaAst(
                inicializacion,
                construirExpresion(
                        contexto.expresion()
                ),
                actualizacion,
                construirBloque(
                        contexto.bloque()
                ),
                posicion(contexto.getStart())
        );
    }

    private SentenciaAst construirInicializacionPara(
            YParser.InicializacionParaContext contexto
    ) {
        if (contexto.declaracionVariable() != null) {
            return construirDeclaracion(
                    contexto.declaracionVariable()
            );
        }

        return construirAsignacion(
                contexto.asignacion()
        );
    }

    private SentenciaAst construirActualizacionPara(
            YParser.ActualizacionParaContext contexto
    ) {
        if (contexto.incrementoDecremento() != null) {
            return construirCambioUnidad(
                    contexto.incrementoDecremento()
            );
        }

        if (contexto.asignacion() != null) {
            return construirAsignacion(
                    contexto.asignacion()
            );
        }

        LlamadaAst llamada = construirLlamadaFuncion(
                contexto.llamadaFuncion()
        );

        return new ExpresionSentenciaAst(
                llamada,
                posicion(contexto.getStart())
        );
    }

    private MientrasAst construirMientras(
            YParser.SentenciaMientrasContext contexto
    ) {
        return new MientrasAst(
                construirExpresion(
                        contexto.expresion()
                ),
                construirBloque(
                        contexto.bloque()
                ),
                posicion(contexto.getStart())
        );
    }

    private HacerMientrasAst construirHacerMientras(
            YParser.SentenciaHacerMientrasContext contexto
    ) {
        return new HacerMientrasAst(
                construirBloque(
                        contexto.bloque()
                ),
                construirExpresion(
                        contexto.expresion()
                ),
                posicion(contexto.getStart())
        );
    }

    private LlamadaAst construirLlamadaFuncion(
            YParser.LlamadaFuncionContext contexto
    ) {
        IdentificadorAst objetivo =
                new IdentificadorAst(
                        contexto.IDENTIFICADOR().getText(),
                        posicion(contexto.getStart())
                );

        return new LlamadaAst(
                objetivo,
                construirArgumentos(
                        contexto.listaArgumentos()
                ),
                posicion(contexto.getStart())
        );
    }

    private List<ExpresionAst> construirArgumentos(
            YParser.ListaArgumentosContext contexto
    ) {
        List<ExpresionAst> argumentos =
                new ArrayList<>();

        if (contexto == null) {
            return argumentos;
        }

        for (YParser.ExpresionContext expresion
                : contexto.expresion()) {

            argumentos.add(
                    construirExpresion(expresion)
            );
        }

        return argumentos;
    }

    private ExpresionAst construirExpresion(
            YParser.ExpresionContext contexto
    ) {
        return construirOr(
                contexto.expresionOr()
        );
    }

    private ExpresionAst construirOr(
            YParser.ExpresionOrContext contexto
    ) {
        ExpresionAst resultado =
                construirAnd(
                        contexto.expresionAnd(0)
                );

        for (int i = 1;
             i < contexto.expresionAnd().size();
             i++) {

            resultado = new BinariaAst(
                    resultado,
                    BinariaAst.Operador.OR,
                    construirAnd(
                            contexto.expresionAnd(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ExpresionAst construirAnd(
            YParser.ExpresionAndContext contexto
    ) {
        ExpresionAst resultado =
                construirIgualdad(
                        contexto.expresionIgualdad(0)
                );

        for (int i = 1;
             i < contexto.expresionIgualdad().size();
             i++) {

            resultado = new BinariaAst(
                    resultado,
                    BinariaAst.Operador.AND,
                    construirIgualdad(
                            contexto.expresionIgualdad(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ExpresionAst construirIgualdad(
            YParser.ExpresionIgualdadContext contexto
    ) {
        ExpresionAst resultado =
                construirRelacional(
                        contexto.expresionRelacional(0)
                );

        for (int i = 1;
             i < contexto.expresionRelacional().size();
             i++) {

            String operador =
                    contexto.getChild(2 * i - 1).getText();

            resultado = new BinariaAst(
                    resultado,
                    operador.equals("==")
                            ? BinariaAst.Operador.IGUALDAD
                            : BinariaAst.Operador.DIFERENTE,
                    construirRelacional(
                            contexto.expresionRelacional(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ExpresionAst construirRelacional(
            YParser.ExpresionRelacionalContext contexto
    ) {
        ExpresionAst resultado =
                construirAditiva(
                        contexto.expresionAditiva(0)
                );

        for (int i = 1;
             i < contexto.expresionAditiva().size();
             i++) {

            String operador =
                    contexto.getChild(2 * i - 1).getText();

            resultado = new BinariaAst(
                    resultado,
                    operadorRelacional(operador),
                    construirAditiva(
                            contexto.expresionAditiva(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private BinariaAst.Operador operadorRelacional(
            String operador
    ) {
        return switch (operador) {
            case "<" -> BinariaAst.Operador.MENOR;
            case ">" -> BinariaAst.Operador.MAYOR;
            case "<=" -> BinariaAst.Operador.MENOR_IGUAL;
            case ">=" -> BinariaAst.Operador.MAYOR_IGUAL;

            default -> throw new IllegalStateException(
                    "Operador relacional desconocido: "
                            + operador
            );
        };
    }

    private ExpresionAst construirAditiva(
            YParser.ExpresionAditivaContext contexto
    ) {
        ExpresionAst resultado =
                construirMultiplicativa(
                        contexto.expresionMultiplicativa(0)
                );

        for (int i = 1;
             i < contexto.expresionMultiplicativa().size();
             i++) {

            String operador =
                    contexto.getChild(2 * i - 1).getText();

            resultado = new BinariaAst(
                    resultado,
                    operador.equals("+")
                            ? BinariaAst.Operador.SUMA
                            : BinariaAst.Operador.RESTA,
                    construirMultiplicativa(
                            contexto.expresionMultiplicativa(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ExpresionAst construirMultiplicativa(
            YParser.ExpresionMultiplicativaContext contexto
    ) {
        ExpresionAst resultado =
                construirUnaria(
                        contexto.expresionUnaria(0)
                );

        for (int i = 1;
             i < contexto.expresionUnaria().size();
             i++) {

            String operador =
                    contexto.getChild(2 * i - 1).getText();

            resultado = new BinariaAst(
                    resultado,
                    operador.equals("*")
                            ? BinariaAst.Operador.MULTIPLICACION
                            : BinariaAst.Operador.DIVISION,
                    construirUnaria(
                            contexto.expresionUnaria(i)
                    ),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ExpresionAst construirUnaria(
            YParser.ExpresionUnariaContext contexto
    ) {
        if (contexto.expresionPostfija() != null) {
            return construirPostfija(
                    contexto.expresionPostfija()
            );
        }

        String operador =
                contexto.getChild(0).getText();

        UnariaAst.Operador tipoOperador =
                switch (operador) {
                    case "!" -> UnariaAst.Operador.NEGACION;
                    case "+" -> UnariaAst.Operador.POSITIVO;
                    case "-" -> UnariaAst.Operador.NEGATIVO;

                    default -> throw new IllegalStateException(
                            "Operador unario desconocido: "
                                    + operador
                    );
                };

        return new UnariaAst(
                tipoOperador,
                construirUnaria(
                        contexto.expresionUnaria()
                ),
                posicion(contexto.getStart())
        );
    }

    private ExpresionAst construirPostfija(
            YParser.ExpresionPostfijaContext contexto
    ) {
        ExpresionAst resultado =
                construirPrimaria(
                        contexto.primaria()
                );

        for (YParser.SufijoPostfijoContext sufijo
                : contexto.sufijoPostfijo()) {

            if (sufijo.PARENTESIS_ABRE() != null) {
                resultado = new LlamadaAst(
                        resultado,
                        construirArgumentos(
                                sufijo.listaArgumentos()
                        ),
                        posicion(sufijo.getStart())
                );

                continue;
            }

            if (sufijo.CORCHETE_ABRE() != null) {
                resultado = new AccesoArregloAst(
                        resultado,
                        construirExpresion(
                                sufijo.expresion()
                        ),
                        posicion(sufijo.getStart())
                );

                continue;
            }

            resultado = new AccesoAtributoAst(
                    resultado,
                    sufijo.IDENTIFICADOR().getText(),
                    posicion(sufijo.getStart())
            );
        }

        if (contexto.INCREMENTO() != null) {
            resultado = new CambioUnidadExpresionAst(
                    resultado,
                    CambioUnidadExpresionAst.Operacion.INCREMENTO,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.DECREMENTO() != null) {
            resultado = new CambioUnidadExpresionAst(
                    resultado,
                    CambioUnidadExpresionAst.Operacion.DECREMENTO,
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ExpresionAst construirPrimaria(
            YParser.PrimariaContext contexto
    ) {
        if (contexto.ENTERO() != null) {
            return new LiteralAst(
                    contexto.ENTERO().getText(),
                    LiteralAst.Tipo.ENTERO,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.DECIMAL() != null) {
            return new LiteralAst(
                    contexto.DECIMAL().getText(),
                    LiteralAst.Tipo.DECIMAL,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.CADENA() != null) {
            return new LiteralAst(
                    contexto.CADENA().getText(),
                    LiteralAst.Tipo.CADENA,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.CARACTER() != null) {
            return new LiteralAst(
                    contexto.CARACTER().getText(),
                    LiteralAst.Tipo.CARACTER,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.BOOL_VERDADERO() != null) {
            return new LiteralAst(
                    contexto.BOOL_VERDADERO().getText(),
                    LiteralAst.Tipo.BOOLEANO,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.BOOL_FALSO() != null) {
            return new LiteralAst(
                    contexto.BOOL_FALSO().getText(),
                    LiteralAst.Tipo.BOOLEANO,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_LEER() != null) {
            return new LeerAst(
                    posicion(contexto.getStart())
            );
        }

        if (contexto.IDENTIFICADOR() != null) {
            return new IdentificadorAst(
                    contexto.IDENTIFICADOR().getText(),
                    posicion(contexto.getStart())
            );
        }

        return construirExpresion(
                contexto.expresion()
        );
    }

    private PosicionFuente posicion(
            Token token
    ) {
        return new PosicionFuente(
                archivo,
                Math.max(token.getLine(), 1),
                Math.max(
                        token.getCharPositionInLine(),
                        0
                )
        );
    }
}