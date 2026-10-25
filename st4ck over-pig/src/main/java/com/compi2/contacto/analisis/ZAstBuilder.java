package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.Visibilidad;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.antlr.v4.runtime.Token;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ZAstBuilder {

    private final Path archivo;

    public ZAstBuilder(Path archivo) {
        this.archivo = Objects.requireNonNull(
                archivo,
                "El archivo es obligatorio"
        );
    }

    public ProgramaAst construir(
            ZetarianoParser.ArchivoContext contexto
    ) {
        ZAst.Clase clase = construirClase(contexto.definicionClase());

        List<NodoAst> elementos = List.of(clase);

        return new ProgramaAst(
                LenguajeFuente.ZETARIANO,
                elementos,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Clase construirClase(
            ZetarianoParser.DefinicionClaseContext contexto
    ) {
        List<ZAst.Miembro> miembros = contexto.miembroClase()
                .stream()
                .map(this::construirMiembro)
                .toList();

        Optional<String> padre = contexto.herencia() == null
                ? Optional.empty()
                : Optional.of(
                contexto.herencia().IDENTIFICADOR().getText()
        );

        return new ZAst.Clase(
                contexto.IDENTIFICADOR().getText(),
                miembros,
                posicion(contexto.getStart()),
                padre
        );
    }

    private Visibilidad construirVisibilidad(
            ZetarianoParser.ModificadorAccesoContext contexto
    ) {
        if (contexto == null) {
            return Visibilidad.DEFAULT;
        }

        if (contexto.KW_PUBLIC() != null) {
            return Visibilidad.PUBLICA;
        }

        if (contexto.KW_PRIVATE() != null) {
            return Visibilidad.PRIVADA;
        }

        return Visibilidad.PROTEGIDA;
    }

    private ZAst.Miembro construirMiembro(
            ZetarianoParser.MiembroClaseContext contexto
    ) {
        if (contexto.constructor() != null) {
            return construirConstructor(contexto.constructor());
        }

        if (contexto.metodo() != null) {
            return construirMetodo(contexto.metodo());
        }

        return construirAtributo(contexto.atributo());
    }

    private ZAst.Atributo construirAtributo(
            ZetarianoParser.AtributoContext contexto
    ) {
        Optional<ZAst.Inicializador> inicializador =
                contexto.inicializador() == null
                        ? Optional.empty()
                        : Optional.of(
                        construirInicializador(contexto.inicializador())
                );

        return new ZAst.Atributo(
                construirTipo(contexto.tipo()),
                contexto.IDENTIFICADOR().getText(),
                inicializador,
                posicion(contexto.getStart()),
                construirVisibilidad(contexto.modificadorAcceso())
        );
    }

    private ZAst.Constructor construirConstructor(
            ZetarianoParser.ConstructorContext contexto
    ) {
        return new ZAst.Constructor(
                contexto.IDENTIFICADOR().getText(),
                construirParametros(contexto.listaParametros()),
                construirBloque(contexto.bloque()),
                posicion(contexto.getStart()),
                construirVisibilidad(contexto.modificadorAcceso())
        );
    }

    private ZAst.Metodo construirMetodo(
            ZetarianoParser.MetodoContext contexto
    ) {
        Optional<ZAst.Tipo> retorno;

        if (contexto.tipoRetorno().KW_VOID() != null) {
            retorno = Optional.empty();
        } else {
            retorno = Optional.of(
                    construirTipo(contexto.tipoRetorno().tipo())
            );
        }

        return new ZAst.Metodo(
                retorno,
                contexto.IDENTIFICADOR().getText(),
                construirParametros(contexto.listaParametros()),
                construirBloque(contexto.bloque()),
                posicion(contexto.getStart()),
                construirVisibilidad(contexto.modificadorAcceso()),
                contexto.KW_OVERRIDE() != null
        );
    }

    private ZAst.Tipo construirTipo(
            ZetarianoParser.TipoContext contexto
    ) {
        return new ZAst.Tipo(
                contexto.tipoBase().getText(),
                contexto.corchetesVacios().size(),
                posicion(contexto.getStart())
        );
    }

    private List<ZAst.Parametro> construirParametros(
            ZetarianoParser.ListaParametrosContext contexto
    ) {
        if (contexto == null) {
            return List.of();
        }

        return contexto.parametro()
                .stream()
                .map(this::construirParametro)
                .toList();
    }

    private ZAst.Parametro construirParametro(
            ZetarianoParser.ParametroContext contexto
    ) {
        return new ZAst.Parametro(
                construirTipo(contexto.tipo()),
                contexto.IDENTIFICADOR().getText(),
                posicion(contexto.getStart())
        );
    }

    private ZAst.Bloque construirBloque(
            ZetarianoParser.BloqueContext contexto
    ) {
        List<ZAst.Sentencia> sentencias = contexto.sentencia()
                .stream()
                .map(this::construirSentencia)
                .toList();

        return new ZAst.Bloque(
                sentencias,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Sentencia construirSentencia(
            ZetarianoParser.SentenciaContext contexto
    ) {
        if (contexto.bloque() != null) {
            return construirBloque(contexto.bloque());
        }

        if (contexto.declaracionLocal() != null) {
            return construirDeclaracion(contexto.declaracionLocal());
        }

        if (contexto.expresion() != null) {
            return new ZAst.ExpresionSentencia(
                    construirExpresion(contexto.expresion()),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.sentenciaIf() != null) {
            return construirSi(contexto.sentenciaIf());
        }

        if (contexto.sentenciaSwitch() != null) {
            return construirSeleccion(contexto.sentenciaSwitch());
        }

        if (contexto.sentenciaFor() != null) {
            return construirPara(contexto.sentenciaFor());
        }

        if (contexto.sentenciaWhile() != null) {
            return construirMientras(contexto.sentenciaWhile());
        }

        if (contexto.sentenciaDoWhile() != null) {
            return construirHacerMientras(contexto.sentenciaDoWhile());
        }

        if (contexto.sentenciaReturn() != null) {
            return construirRetorno(contexto.sentenciaReturn());
        }

        if (contexto.KW_BREAK() != null) {
            return new ZAst.Romper(posicion(contexto.getStart()));
        }

        if (contexto.KW_CONTINUE() != null) {
            return new ZAst.Continuar(posicion(contexto.getStart()));
        }

        return new ZAst.Vacia(posicion(contexto.getStart()));
    }

    private ZAst.Declaracion construirDeclaracion(
            ZetarianoParser.DeclaracionLocalContext contexto
    ) {
        Optional<ZAst.Inicializador> inicializador =
                contexto.inicializador() == null
                        ? Optional.empty()
                        : Optional.of(
                        construirInicializador(contexto.inicializador())
                );

        return new ZAst.Declaracion(
                construirTipo(contexto.tipo()),
                contexto.IDENTIFICADOR().getText(),
                inicializador,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Si construirSi(
            ZetarianoParser.SentenciaIfContext contexto
    ) {
        List<ZetarianoParser.SentenciaContext> sentencias =
                contexto.sentencia();

        ZAst.Sentencia entonces = construirSentencia(sentencias.get(0));

        Optional<ZAst.Sentencia> sino = contexto.KW_ELSE() == null
                ? Optional.empty()
                : Optional.of(construirSentencia(sentencias.get(1)));

        return new ZAst.Si(
                construirExpresion(contexto.expresion()),
                entonces,
                sino,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Seleccion construirSeleccion(
            ZetarianoParser.SentenciaSwitchContext contexto
    ) {
        List<ZAst.CasoSeleccion> casos = contexto.bloqueSwitch()
                .stream()
                .map(this::construirCasoSeleccion)
                .toList();

        return new ZAst.Seleccion(
                construirExpresion(contexto.expresion()),
                casos,
                posicion(contexto.getStart())
        );
    }

    private ZAst.CasoSeleccion construirCasoSeleccion(
            ZetarianoParser.BloqueSwitchContext contexto
    ) {
        Optional<ZAst.Expresion> valor = contexto.KW_DEFAULT() != null
                ? Optional.empty()
                : Optional.of(construirExpresion(contexto.expresion()));

        List<ZAst.Sentencia> sentencias = contexto.sentencia()
                .stream()
                .map(this::construirSentencia)
                .toList();

        return new ZAst.CasoSeleccion(
                valor,
                sentencias,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Para construirPara(
            ZetarianoParser.SentenciaForContext contexto
    ) {
        Optional<NodoAst> inicializacion = Optional.empty();

        if (contexto.inicializacionFor() != null) {
            ZetarianoParser.InicializacionForContext inicio =
                    contexto.inicializacionFor();

            if (inicio.declaracionLocal() != null) {
                inicializacion = Optional.of(
                        construirDeclaracion(inicio.declaracionLocal())
                );
            } else {
                inicializacion = Optional.of(
                        construirListaExpresiones(inicio.listaExpresiones())
                );
            }
        }

        Optional<ZAst.Expresion> condicion = contexto.expresion() == null
                ? Optional.empty()
                : Optional.of(construirExpresion(contexto.expresion()));

        List<ZAst.Expresion> actualizaciones =
                contexto.listaExpresiones() == null
                        ? List.of()
                        : construirListaExpresiones(
                        contexto.listaExpresiones()
                ).expresiones();

        return new ZAst.Para(
                inicializacion,
                condicion,
                actualizaciones,
                construirSentencia(contexto.sentencia()),
                posicion(contexto.getStart())
        );
    }

    private ZAst.ListaExpresiones construirListaExpresiones(
            ZetarianoParser.ListaExpresionesContext contexto
    ) {
        List<ZAst.Expresion> expresiones = contexto.expresion()
                .stream()
                .map(this::construirExpresion)
                .toList();

        return new ZAst.ListaExpresiones(
                expresiones,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Mientras construirMientras(
            ZetarianoParser.SentenciaWhileContext contexto
    ) {
        return new ZAst.Mientras(
                construirExpresion(contexto.expresion()),
                construirSentencia(contexto.sentencia()),
                posicion(contexto.getStart())
        );
    }

    private ZAst.HacerMientras construirHacerMientras(
            ZetarianoParser.SentenciaDoWhileContext contexto
    ) {
        return new ZAst.HacerMientras(
                construirSentencia(contexto.sentencia()),
                construirExpresion(contexto.expresion()),
                posicion(contexto.getStart())
        );
    }

    private ZAst.Retorno construirRetorno(
            ZetarianoParser.SentenciaReturnContext contexto
    ) {
        Optional<ZAst.Expresion> expresion = contexto.expresion() == null
                ? Optional.empty()
                : Optional.of(construirExpresion(contexto.expresion()));

        return new ZAst.Retorno(
                expresion,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Inicializador construirInicializador(
            ZetarianoParser.InicializadorContext contexto
    ) {
        if (contexto.expresion() != null) {
            return construirExpresion(contexto.expresion());
        }

        return construirInicializadorLista(contexto.inicializadorLista());
    }

    private ZAst.InicializadorLista construirInicializadorLista(
            ZetarianoParser.InicializadorListaContext contexto
    ) {
        List<ZAst.Inicializador> valores = contexto.inicializador()
                .stream()
                .map(this::construirInicializador)
                .toList();

        return new ZAst.InicializadorLista(
                valores,
                posicion(contexto.getStart())
        );
    }

    private ZAst.Expresion construirExpresion(
            ZetarianoParser.ExpresionContext contexto
    ) {
        return construirAsignacion(contexto.expresionAsignacion());
    }

    private ZAst.Expresion construirAsignacion(
            ZetarianoParser.ExpresionAsignacionContext contexto
    ) {
        ZAst.Expresion izquierda =
                construirTernaria(contexto.expresionTernaria());

        if (contexto.operadorAsignacion() == null) {
            return izquierda;
        }

        return new ZAst.AsignacionExpresion(
                operadorAsignacion(contexto.operadorAsignacion().getText()),
                izquierda,
                construirAsignacion(contexto.expresionAsignacion()),
                posicion(contexto.getStart())
        );
    }

    private ZAst.Expresion construirTernaria(
            ZetarianoParser.ExpresionTernariaContext contexto
    ) {
        ZAst.Expresion condicion = construirOr(contexto.expresionOr());

        if (contexto.TERNARIO() == null) {
            return condicion;
        }

        return new ZAst.Ternaria(
                condicion,
                construirExpresion(contexto.expresion()),
                construirTernaria(contexto.expresionTernaria()),
                posicion(contexto.getStart())
        );
    }

    private ZAst.Expresion construirOr(
            ZetarianoParser.ExpresionOrContext contexto
    ) {
        List<ZetarianoParser.ExpresionAndContext> operandos =
                contexto.expresionAnd();

        ZAst.Expresion resultado = construirAnd(operandos.get(0));

        for (int i = 1; i < operandos.size(); i++) {
            resultado = new ZAst.Binaria(
                    ZAst.OperadorBinario.OR,
                    resultado,
                    construirAnd(operandos.get(i)),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirAnd(
            ZetarianoParser.ExpresionAndContext contexto
    ) {
        List<ZetarianoParser.ExpresionIgualdadContext> operandos =
                contexto.expresionIgualdad();

        ZAst.Expresion resultado = construirIgualdad(operandos.get(0));

        for (int i = 1; i < operandos.size(); i++) {
            resultado = new ZAst.Binaria(
                    ZAst.OperadorBinario.AND,
                    resultado,
                    construirIgualdad(operandos.get(i)),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirIgualdad(
            ZetarianoParser.ExpresionIgualdadContext contexto
    ) {
        List<ZetarianoParser.ExpresionRelacionalContext> operandos =
                contexto.expresionRelacional();

        ZAst.Expresion resultado = construirRelacional(operandos.get(0));

        for (int i = 1; i < operandos.size(); i++) {
            String operador = contexto.getChild((i * 2) - 1).getText();

            resultado = new ZAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirRelacional(operandos.get(i)),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirRelacional(
            ZetarianoParser.ExpresionRelacionalContext contexto
    ) {
        List<ZetarianoParser.ExpresionAditivaContext> operandos =
                contexto.expresionAditiva();

        ZAst.Expresion resultado = construirAditiva(operandos.get(0));

        for (int i = 1; i < operandos.size(); i++) {
            String operador = contexto.getChild((i * 2) - 1).getText();

            resultado = new ZAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirAditiva(operandos.get(i)),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirAditiva(
            ZetarianoParser.ExpresionAditivaContext contexto
    ) {
        List<ZetarianoParser.ExpresionMultiplicativaContext> operandos =
                contexto.expresionMultiplicativa();

        ZAst.Expresion resultado = construirMultiplicativa(operandos.get(0));

        for (int i = 1; i < operandos.size(); i++) {
            String operador = contexto.getChild((i * 2) - 1).getText();

            resultado = new ZAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirMultiplicativa(operandos.get(i)),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirMultiplicativa(
            ZetarianoParser.ExpresionMultiplicativaContext contexto
    ) {
        List<ZetarianoParser.ExpresionUnariaContext> operandos =
                contexto.expresionUnaria();

        ZAst.Expresion resultado = construirUnaria(operandos.get(0));

        for (int i = 1; i < operandos.size(); i++) {
            String operador = contexto.getChild((i * 2) - 1).getText();

            resultado = new ZAst.Binaria(
                    operadorBinario(operador),
                    resultado,
                    construirUnaria(operandos.get(i)),
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirUnaria(
            ZetarianoParser.ExpresionUnariaContext contexto
    ) {
        if (contexto.expresionPostfija() != null) {
            return construirPostfija(contexto.expresionPostfija());
        }

        String operador = contexto.getChild(0).getText();

        return new ZAst.Unaria(
                operadorUnario(operador),
                construirUnaria(contexto.expresionUnaria()),
                posicion(contexto.getStart())
        );
    }

    private ZAst.Expresion construirPostfija(
            ZetarianoParser.ExpresionPostfijaContext contexto
    ) {
        ZAst.Expresion resultado = construirPrimaria(contexto.primaria());

        for (ZetarianoParser.SufijoPostfijoContext sufijo
                : contexto.sufijoPostfijo()) {

            if (sufijo.PARENTESIS_ABRE() != null) {
                resultado = new ZAst.Llamada(
                        resultado,
                        construirArgumentos(sufijo.listaArgumentos()),
                        posicion(sufijo.getStart())
                );

                continue;
            }

            if (sufijo.CORCHETE_ABRE() != null) {
                resultado = new ZAst.AccesoArreglo(
                        resultado,
                        construirExpresion(sufijo.expresion()),
                        posicion(sufijo.getStart())
                );

                continue;
            }

            resultado = new ZAst.AccesoMiembro(
                    resultado,
                    sufijo.miembroAcceso().getText(),
                    posicion(sufijo.getStart())
            );
        }

        if (contexto.INCREMENTO() != null) {
            resultado = new ZAst.CambioPostfijo(
                    resultado,
                    ZAst.OperacionPostfija.INCREMENTO,
                    posicion(contexto.getStart())
            );
        }

        if (contexto.DECREMENTO() != null) {
            resultado = new ZAst.CambioPostfijo(
                    resultado,
                    ZAst.OperacionPostfija.DECREMENTO,
                    posicion(contexto.getStart())
            );
        }

        return resultado;
    }

    private ZAst.Expresion construirPrimaria(
            ZetarianoParser.PrimariaContext contexto
    ) {
        if (contexto.ENTERO() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.ENTERO,
                    contexto.ENTERO().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.DECIMAL() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.DECIMAL,
                    contexto.DECIMAL().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.CADENA() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.CADENA,
                    contexto.CADENA().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.CARACTER() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.CARACTER,
                    contexto.CARACTER().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.BOOL_TRUE() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.BOOLEANO,
                    "true",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.BOOL_FALSE() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.BOOLEANO,
                    "false",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.NULL_VALUE() != null) {
            return new ZAst.Literal(
                    ZAst.TipoLiteral.NULO,
                    "null",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_THIS() != null) {
            return new ZAst.Identificador(
                    "this",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_SUPER() != null) {
            return new ZAst.Identificador(
                    "super",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.IDENTIFICADOR() != null) {
            return new ZAst.Identificador(
                    contexto.IDENTIFICADOR().getText(),
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_PRINT() != null) {
            return new ZAst.Identificador(
                    "print",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_PRINTLN() != null) {
            return new ZAst.Identificador(
                    "println",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.KW_READLN() != null) {
            return new ZAst.Identificador(
                    "readln",
                    posicion(contexto.getStart())
            );
        }

        if (contexto.creacion() != null) {
            return construirCreacion(contexto.creacion());
        }

        return construirExpresion(contexto.expresion());
    }

    private ZAst.Expresion construirCreacion(
            ZetarianoParser.CreacionContext contexto
    ) {
        String tipo = contexto.tipoBase().getText();

        if (contexto.argumentosConstructor() != null) {
            return new ZAst.NuevoObjeto(
                    tipo,
                    construirArgumentos(
                            contexto.argumentosConstructor().listaArgumentos()
                    ),
                    posicion(contexto.getStart())
            );
        }

        List<ZAst.Expresion> dimensiones = contexto.dimensionCreacion()
                .stream()
                .map(dimension -> construirExpresion(dimension.expresion()))
                .toList();

        return new ZAst.NuevoArreglo(
                tipo,
                dimensiones,
                posicion(contexto.getStart())
        );
    }

    private List<ZAst.Expresion> construirArgumentos(
            ZetarianoParser.ListaArgumentosContext contexto
    ) {
        if (contexto == null) {
            return List.of();
        }

        return contexto.expresion()
                .stream()
                .map(this::construirExpresion)
                .toList();
    }

    private ZAst.OperadorAsignacion operadorAsignacion(String operador) {
        return switch (operador) {
            case "=" -> ZAst.OperadorAsignacion.ASIGNAR;
            case "+=" -> ZAst.OperadorAsignacion.SUMAR_ASIGNAR;
            case "-=" -> ZAst.OperadorAsignacion.RESTAR_ASIGNAR;
            case "*=" -> ZAst.OperadorAsignacion.MULTIPLICAR_ASIGNAR;
            case "/=" -> ZAst.OperadorAsignacion.DIVIDIR_ASIGNAR;
            default -> throw new IllegalArgumentException(
                    "Operador de asignacion no soportado: " + operador
            );
        };
    }

    private ZAst.OperadorBinario operadorBinario(String operador) {
        return switch (operador) {
            case "+" -> ZAst.OperadorBinario.SUMA;
            case "-" -> ZAst.OperadorBinario.RESTA;
            case "*" -> ZAst.OperadorBinario.MULTIPLICACION;
            case "/" -> ZAst.OperadorBinario.DIVISION;
            case "%" -> ZAst.OperadorBinario.MODULO;
            case "==" -> ZAst.OperadorBinario.IGUALDAD;
            case "!=" -> ZAst.OperadorBinario.DIFERENTE;
            case "<" -> ZAst.OperadorBinario.MENOR;
            case ">" -> ZAst.OperadorBinario.MAYOR;
            case "<=" -> ZAst.OperadorBinario.MENOR_IGUAL;
            case ">=" -> ZAst.OperadorBinario.MAYOR_IGUAL;
            case "&&" -> ZAst.OperadorBinario.AND;
            case "||" -> ZAst.OperadorBinario.OR;
            default -> throw new IllegalArgumentException(
                    "Operador binario no soportado: " + operador
            );
        };
    }

    private ZAst.OperadorUnario operadorUnario(String operador) {
        return switch (operador) {
            case "!" -> ZAst.OperadorUnario.NEGACION;
            case "+" -> ZAst.OperadorUnario.POSITIVO;
            case "-" -> ZAst.OperadorUnario.NEGATIVO;
            case "++" -> ZAst.OperadorUnario.INCREMENTO_PRE;
            case "--" -> ZAst.OperadorUnario.DECREMENTO_PRE;
            default -> throw new IllegalArgumentException(
                    "Operador unario no soportado: " + operador
            );
        };
    }

    private PosicionFuente posicion(Token token) {
        return new PosicionFuente(
                archivo,
                Math.max(token.getLine(), 1),
                Math.max(token.getCharPositionInLine(), 0)
        );
    }
}