package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.AtributoEstructuraAst;
import com.compi2.contacto.ast.DeclaracionVariableAst;
import com.compi2.contacto.ast.ElegirAst;
import com.compi2.contacto.ast.EstructuraAst;
import com.compi2.contacto.ast.FuncionAst;
import com.compi2.contacto.ast.HacerMientrasAst;
import com.compi2.contacto.ast.MientrasAst;
import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ParametroAst;
import com.compi2.contacto.ast.ParaAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.SentenciaAst;
import com.compi2.contacto.ast.SiAst;
import com.compi2.contacto.ast.TipoAst;
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.ast.zetariano.ZAst;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConstructorTablaSimbolosProyecto {

    private final Set<String> estructurasY;
    private final Set<String> clasesZ;

    public ConstructorTablaSimbolosProyecto() {
        estructurasY = new HashSet<>();
        clasesZ = new HashSet<>();
    }

    public TablaSimbolos construir(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ,
            List<ProgramaAst> programasPig
    ) {
        Objects.requireNonNull(programasY);
        Objects.requireNonNull(programasZ);
        Objects.requireNonNull(programasPig);

        TablaSimbolos tabla =
                new TablaSimbolos();

        estructurasY.clear();
        clasesZ.clear();

        recolectarTipos(
                programasY,
                programasZ
        );

        registrarY(
                tabla,
                programasY
        );

        registrarZ(
                tabla,
                programasZ
        );

        registrarPig(
                tabla,
                programasPig
        );

        return tabla;
    }

    private void recolectarTipos(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ
    ) {
        for (ProgramaAst programa : programasY) {
            for (NodoAst nodo : programa.elementos()) {
                if (nodo instanceof EstructuraAst estructura) {
                    estructurasY.add(
                            estructura.nombre()
                    );
                }
            }
        }

        for (ProgramaAst programa : programasZ) {
            for (NodoAst nodo : programa.elementos()) {
                if (nodo instanceof ZAst.Clase clase) {
                    clasesZ.add(
                            clase.nombre()
                    );
                }
            }
        }
    }

    private void registrarY(
            TablaSimbolos tabla,
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {

            for (NodoAst nodo : programa.elementos()) {

                if (nodo instanceof EstructuraAst estructura) {
                    registrarEstructuraY(
                            tabla,
                            estructura,
                            "Y::estructura::"
                                    + estructura.nombre()
                    );

                    continue;
                }

                if (nodo instanceof FuncionAst funcion) {
                    registrarFuncionY(
                            tabla,
                            funcion
                    );
                }
            }
        }
    }

    private void registrarEstructuraY(
            TablaSimbolos tabla,
            EstructuraAst estructura,
            String ruta
    ) {
        declarar(
                tabla,
                ruta,
                CategoriaSimbolo.ESTRUCTURA,
                TipoDato.ESTRUCTURA,
                estructura.nombre(),
                estructura.posicion()
        );

        for (AtributoEstructuraAst atributo
                : estructura.atributos()) {

            int dimensiones =
                    atributo.dimensiones()
                            .size();

            declarar(
                    tabla,
                    ruta
                            + "::atributo::"
                            + atributo.nombre(),
                    CategoriaSimbolo.ATRIBUTO,
                    tipoY(
                            atributo.tipo(),
                            dimensiones
                    ),
                    nombreTipoY(
                            atributo.tipo(),
                            dimensiones
                    ),
                    atributo.posicion()
            );
        }
    }

    private void registrarFuncionY(
            TablaSimbolos tabla,
            FuncionAst funcion
    ) {
        String ruta =
                "Y::funcion::"
                        + funcion.nombre();

        String retorno =
                funcion.retorno()
                        .map(
                                TipoAst::nombre
                        )
                        .orElse(
                                "void"
                        );

        declarar(
                tabla,
                ruta,
                CategoriaSimbolo.FUNCION,
                funcion.retorno()
                        .map(
                                tipo ->
                                        tipoY(
                                                tipo,
                                                0
                                        )
                        )
                        .orElse(
                                TipoDato.VOID
                        ),
                retorno,
                funcion.posicion()
        );

        for (ParametroAst parametro
                : funcion.parametros()) {

            int dimensiones =
                    parametro.modo()
                            == ParametroAst.Modo.REFERENCIA_ARREGLO
                            ? 1
                            : 0;

            declarar(
                    tabla,
                    ruta
                            + "::parametro::"
                            + parametro.nombre(),
                    CategoriaSimbolo.PARAMETRO,
                    tipoY(
                            parametro.tipo(),
                            dimensiones
                    ),
                    nombreTipoY(
                            parametro.tipo(),
                            dimensiones
                    ),
                    parametro.posicion()
            );
        }

        registrarSentenciasY(
                tabla,
                funcion.cuerpo(),
                ruta + "::cuerpo"
        );
    }

    private void registrarSentenciasY(
            TablaSimbolos tabla,
            List<SentenciaAst> sentencias,
            String ruta
    ) {
        int indice = 0;

        for (SentenciaAst sentencia
                : sentencias) {

            registrarSentenciaY(
                    tabla,
                    sentencia,
                    ruta
                            + "::"
                            + indice
            );

            indice++;
        }
    }

    private void registrarSentenciaY(
            TablaSimbolos tabla,
            SentenciaAst sentencia,
            String ruta
    ) {
        if (sentencia
                instanceof DeclaracionVariableAst declaracion) {

            int dimensiones =
                    declaracion.dimensiones()
                            .size();

            declarar(
                    tabla,
                    ruta
                            + "::variable::"
                            + declaracion.nombre(),
                    CategoriaSimbolo.VARIABLE,
                    tipoY(
                            declaracion.tipo(),
                            dimensiones
                    ),
                    nombreTipoY(
                            declaracion.tipo(),
                            dimensiones
                    ),
                    declaracion.posicion()
            );

            return;
        }

        if (sentencia
                instanceof EstructuraAst estructura) {

            registrarEstructuraY(
                    tabla,
                    estructura,
                    ruta
                            + "::estructura::"
                            + estructura.nombre()
            );

            return;
        }

        if (sentencia
                instanceof SiAst si) {

            int indice = 0;

            for (SiAst.Rama rama
                    : si.ramas()) {

                registrarSentenciasY(
                        tabla,
                        rama.cuerpo(),
                        ruta
                                + "::si::"
                                + indice
                );

                indice++;
            }

            registrarSentenciasY(
                    tabla,
                    si.contrario(),
                    ruta
                            + "::contrario"
            );

            return;
        }

        if (sentencia
                instanceof ElegirAst elegir) {

            int indice = 0;

            for (ElegirAst.Caso caso
                    : elegir.casos()) {

                registrarSentenciasY(
                        tabla,
                        caso.cuerpo(),
                        ruta
                                + "::caso::"
                                + indice
                );

                indice++;
            }

            registrarSentenciasY(
                    tabla,
                    elegir.siempre(),
                    ruta
                            + "::siempre"
            );

            return;
        }

        if (sentencia
                instanceof ParaAst para) {

            registrarSentenciaY(
                    tabla,
                    para.inicializacion(),
                    ruta
                            + "::para::inicio"
            );

            registrarSentenciasY(
                    tabla,
                    para.cuerpo(),
                    ruta
                            + "::para::cuerpo"
            );

            registrarSentenciaY(
                    tabla,
                    para.actualizacion(),
                    ruta
                            + "::para::actualizacion"
            );

            return;
        }

        if (sentencia
                instanceof MientrasAst mientras) {

            registrarSentenciasY(
                    tabla,
                    mientras.cuerpo(),
                    ruta
                            + "::mientras"
            );

            return;
        }

        if (sentencia
                instanceof HacerMientrasAst hacer) {

            registrarSentenciasY(
                    tabla,
                    hacer.cuerpo(),
                    ruta
                            + "::hacer"
            );
        }
    }

    private void registrarZ(
            TablaSimbolos tabla,
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa
                : programas) {

            for (NodoAst nodo
                    : programa.elementos()) {

                if (!(nodo
                        instanceof ZAst.Clase clase)) {
                    continue;
                }

                registrarClaseZ(
                        tabla,
                        clase
                );
            }
        }
    }

    private void registrarClaseZ(
            TablaSimbolos tabla,
            ZAst.Clase clase
    ) {
        String ruta =
                "Z::clase::"
                        + clase.nombre();

        declarar(
                tabla,
                ruta,
                CategoriaSimbolo.CLASE,
                TipoDato.OBJETO,
                clase.nombre(),
                clase.posicion()
        );

        for (ZAst.Miembro miembro
                : clase.miembros()) {

            if (miembro
                    instanceof ZAst.Atributo atributo) {

                declarar(
                        tabla,
                        ruta
                                + "::atributo::"
                                + atributo.nombre(),
                        CategoriaSimbolo.ATRIBUTO,
                        tipoZ(
                                atributo.tipo()
                        ),
                        atributo.tipo()
                                .nombreCompleto(),
                        atributo.posicion()
                );

                continue;
            }

            if (miembro
                    instanceof ZAst.Constructor constructor) {

                registrarConstructorZ(
                        tabla,
                        clase,
                        constructor,
                        ruta
                );

                continue;
            }

            if (miembro
                    instanceof ZAst.Metodo metodo) {

                registrarMetodoZ(
                        tabla,
                        metodo,
                        ruta
                );
            }
        }
    }

    private void registrarConstructorZ(
            TablaSimbolos tabla,
            ZAst.Clase clase,
            ZAst.Constructor constructor,
            String rutaClase
    ) {
        String firma =
                firmaZ(
                        constructor.nombre(),
                        constructor.parametros()
                );

        String ruta =
                rutaClase
                        + "::constructor::"
                        + firma;

        declarar(
                tabla,
                ruta,
                CategoriaSimbolo.CONSTRUCTOR,
                TipoDato.OBJETO,
                clase.nombre(),
                constructor.posicion()
        );

        registrarParametrosZ(
                tabla,
                constructor.parametros(),
                ruta
        );

        registrarSentenciaZ(
                tabla,
                constructor.cuerpo(),
                ruta
                        + "::cuerpo"
        );
    }

    private void registrarMetodoZ(
            TablaSimbolos tabla,
            ZAst.Metodo metodo,
            String rutaClase
    ) {
        String firma =
                firmaZ(
                        metodo.nombre(),
                        metodo.parametros()
                );

        String ruta =
                rutaClase
                        + "::metodo::"
                        + firma;

        String retorno =
                metodo.retorno()
                        .map(
                                ZAst.Tipo::nombreCompleto
                        )
                        .orElse(
                                "void"
                        );

        declarar(
                tabla,
                ruta,
                CategoriaSimbolo.METODO,
                metodo.retorno()
                        .map(
                                this::tipoZ
                        )
                        .orElse(
                                TipoDato.VOID
                        ),
                retorno,
                metodo.posicion()
        );

        registrarParametrosZ(
                tabla,
                metodo.parametros(),
                ruta
        );

        registrarSentenciaZ(
                tabla,
                metodo.cuerpo(),
                ruta
                        + "::cuerpo"
        );
    }

    private void registrarParametrosZ(
            TablaSimbolos tabla,
            List<ZAst.Parametro> parametros,
            String ruta
    ) {
        for (ZAst.Parametro parametro
                : parametros) {

            declarar(
                    tabla,
                    ruta
                            + "::parametro::"
                            + parametro.nombre(),
                    CategoriaSimbolo.PARAMETRO,
                    tipoZ(
                            parametro.tipo()
                    ),
                    parametro.tipo()
                            .nombreCompleto(),
                    parametro.posicion()
            );
        }
    }

    private void registrarSentenciaZ(
            TablaSimbolos tabla,
            ZAst.Sentencia sentencia,
            String ruta
    ) {
        if (sentencia
                instanceof ZAst.Declaracion declaracion) {

            declarar(
                    tabla,
                    ruta
                            + "::variable::"
                            + declaracion.nombre(),
                    CategoriaSimbolo.VARIABLE,
                    tipoZ(
                            declaracion.tipo()
                    ),
                    declaracion.tipo()
                            .nombreCompleto(),
                    declaracion.posicion()
            );

            return;
        }

        if (sentencia
                instanceof ZAst.Bloque bloque) {

            int indice = 0;

            for (ZAst.Sentencia interna
                    : bloque.sentencias()) {

                registrarSentenciaZ(
                        tabla,
                        interna,
                        ruta
                                + "::"
                                + indice
                );

                indice++;
            }

            return;
        }

        if (sentencia
                instanceof ZAst.Si si) {

            registrarSentenciaZ(
                    tabla,
                    si.entonces(),
                    ruta
                            + "::if"
            );

            si.sino()
                    .ifPresent(
                            sino ->
                                    registrarSentenciaZ(
                                            tabla,
                                            sino,
                                            ruta
                                                    + "::else"
                                    )
                    );

            return;
        }

        if (sentencia
                instanceof ZAst.Seleccion seleccion) {

            int indice = 0;

            for (ZAst.CasoSeleccion caso
                    : seleccion.casos()) {

                int interno = 0;

                for (ZAst.Sentencia sentenciaCaso
                        : caso.sentencias()) {

                    registrarSentenciaZ(
                            tabla,
                            sentenciaCaso,
                            ruta
                                    + "::case::"
                                    + indice
                                    + "::"
                                    + interno
                    );

                    interno++;
                }

                indice++;
            }

            return;
        }

        if (sentencia
                instanceof ZAst.Para para) {

            para.inicializacion()
                    .ifPresent(
                            inicio -> {

                                if (inicio
                                        instanceof ZAst.Declaracion declaracion) {

                                    registrarSentenciaZ(
                                            tabla,
                                            declaracion,
                                            ruta
                                                    + "::for::inicio"
                                    );
                                }
                            }
                    );

            registrarSentenciaZ(
                    tabla,
                    para.cuerpo(),
                    ruta
                            + "::for::cuerpo"
            );

            return;
        }

        if (sentencia
                instanceof ZAst.Mientras mientras) {

            registrarSentenciaZ(
                    tabla,
                    mientras.cuerpo(),
                    ruta
                            + "::while"
            );

            return;
        }

        if (sentencia
                instanceof ZAst.HacerMientras hacer) {

            registrarSentenciaZ(
                    tabla,
                    hacer.cuerpo(),
                    ruta
                            + "::do"
            );
        }
    }

    private void registrarPig(
            TablaSimbolos tabla,
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa
                : programas) {

            for (NodoAst nodo
                    : programa.elementos()) {

                if (!(nodo
                        instanceof PAst.Programa pig)) {
                    continue;
                }

                String archivo =
                        pig.posicion()
                                .archivo()
                                .getFileName()
                                .toString();

                String ruta =
                        "PIG::"
                                + archivo;

                for (PAst.Declaracion global
                        : pig.globales()) {

                    registrarDeclaracionPig(
                            tabla,
                            global,
                            ruta
                                    + "::global"
                    );
                }

                int indice = 0;

                for (PAst.Sentencia sentencia
                        : pig.principal()) {

                    registrarSentenciaPig(
                            tabla,
                            sentencia,
                            ruta
                                    + "::MAIOR::"
                                    + indice
                    );

                    indice++;
                }
            }
        }
    }

    private void registrarDeclaracionPig(
            TablaSimbolos tabla,
            PAst.Declaracion declaracion,
            String ruta
    ) {
        declarar(
                tabla,
                ruta
                        + "::variable::"
                        + declaracion.nombre(),
                CategoriaSimbolo.VARIABLE,
                tipoPig(
                        declaracion.tipo()
                ),
                declaracion.tipo()
                        .nombreCompleto(),
                declaracion.posicion()
        );
    }

    private void registrarSentenciaPig(
            TablaSimbolos tabla,
            PAst.Sentencia sentencia,
            String ruta
    ) {
        if (sentencia
                instanceof PAst.Declaracion declaracion) {

            registrarDeclaracionPig(
                    tabla,
                    declaracion,
                    ruta
            );

            return;
        }

        if (sentencia
                instanceof PAst.Si si) {

            int indice = 0;

            for (PAst.RamaSi rama
                    : si.ramas()) {

                int interno = 0;

                for (PAst.Sentencia sentenciaRama
                        : rama.cuerpo()) {

                    registrarSentenciaPig(
                            tabla,
                            sentenciaRama,
                            ruta
                                    + "::si::"
                                    + indice
                                    + "::"
                                    + interno
                    );

                    interno++;
                }

                indice++;
            }

            return;
        }

        if (sentencia
                instanceof PAst.Mientras mientras) {

            registrarSentenciasPig(
                    tabla,
                    mientras.cuerpo(),
                    ruta
                            + "::dum"
            );

            return;
        }

        if (sentencia
                instanceof PAst.HacerMientras hacer) {

            registrarSentenciasPig(
                    tabla,
                    hacer.cuerpo(),
                    ruta
                            + "::facere"
            );

            return;
        }

        if (sentencia
                instanceof PAst.Para para) {

            para.inicializacion()
                    .ifPresent(
                            inicio -> {

                                if (inicio
                                        instanceof PAst.Declaracion declaracion) {

                                    registrarDeclaracionPig(
                                            tabla,
                                            declaracion,
                                            ruta
                                                    + "::per::inicio"
                                    );
                                }
                            }
                    );

            registrarSentenciasPig(
                    tabla,
                    para.cuerpo(),
                    ruta
                            + "::per::cuerpo"
            );
        }
    }

    private void registrarSentenciasPig(
            TablaSimbolos tabla,
            List<PAst.Sentencia> sentencias,
            String ruta
    ) {
        int indice = 0;

        for (PAst.Sentencia sentencia
                : sentencias) {

            registrarSentenciaPig(
                    tabla,
                    sentencia,
                    ruta
                            + "::"
                            + indice
            );

            indice++;
        }
    }

    private TipoDato tipoY(
            TipoAst tipo,
            int dimensiones
    ) {
        if (dimensiones > 0) {
            return TipoDato.ARREGLO;
        }

        return switch (
                tipo.nombre()
                ) {
            case "entero" ->
                    TipoDato.ENTERO;

            case "flotante" ->
                    TipoDato.DECIMAL;

            case "cadena" ->
                    TipoDato.CADENA;

            case "caracter" ->
                    TipoDato.CARACTER;

            case "bool" ->
                    TipoDato.BOOLEANO;

            default -> {
                if (estructurasY.contains(
                        tipo.nombre()
                )) {
                    yield TipoDato.ESTRUCTURA;
                }

                yield TipoDato.DESCONOCIDO;
            }
        };
    }

    private String nombreTipoY(
            TipoAst tipo,
            int dimensiones
    ) {
        return tipo.nombre()
                + "[]".repeat(
                dimensiones
        );
    }

    private TipoDato tipoZ(
            ZAst.Tipo tipo
    ) {
        if (tipo.esArreglo()) {
            return TipoDato.ARREGLO;
        }

        return switch (
                tipo.nombreBase()
                ) {
            case "int" ->
                    TipoDato.ENTERO;

            case "double" ->
                    TipoDato.DECIMAL;

            case "char" ->
                    TipoDato.CARACTER;

            case "boolean" ->
                    TipoDato.BOOLEANO;

            case "String" ->
                    TipoDato.CADENA;

            default -> {
                if (clasesZ.contains(
                        tipo.nombreBase()
                )) {
                    yield TipoDato.OBJETO;
                }

                yield TipoDato.DESCONOCIDO;
            }
        };
    }

    private TipoDato tipoPig(
            PAst.Tipo tipo
    ) {
        if (tipo.esArreglo()) {
            return TipoDato.ARREGLO;
        }

        return switch (
                tipo.nombreBase()
                ) {
            case "numerus" ->
                    TipoDato.ENTERO;

            case "decimalis" ->
                    TipoDato.DECIMAL;

            case "textum" ->
                    TipoDato.CADENA;

            case "littera" ->
                    TipoDato.CARACTER;

            case "bool" ->
                    TipoDato.BOOLEANO;

            default -> {
                if (estructurasY.contains(
                        tipo.nombreBase()
                )) {
                    yield TipoDato.ESTRUCTURA;
                }

                if (clasesZ.contains(
                        tipo.nombreBase()
                )) {
                    yield TipoDato.OBJETO;
                }

                yield TipoDato.DESCONOCIDO;
            }
        };
    }

    private String firmaZ(
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
                        .collect(
                                Collectors.joining(",")
                        );

        return nombre
                + "("
                + tipos
                + ")";
    }

    private void declarar(
            TablaSimbolos tabla,
            String nombre,
            CategoriaSimbolo categoria,
            TipoDato tipo,
            String tipoDeclarado,
            com.compi2.contacto.ast.PosicionFuente posicion
    ) {
        tabla.declarar(
                new Simbolo(
                        nombre,
                        categoria,
                        tipo,
                        tipoDeclarado,
                        posicion
                )
        );
    }
}