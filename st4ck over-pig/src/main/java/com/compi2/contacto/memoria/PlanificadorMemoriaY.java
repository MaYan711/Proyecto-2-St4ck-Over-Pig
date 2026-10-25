package com.compi2.contacto.memoria;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PlanificadorMemoriaY {

    private static final Set<String> TIPOS_PRIMITIVOS =
            Set.of(
                    "entero",
                    "flotante",
                    "cadena",
                    "caracter",
                    "bool"
            );

    private final Set<String> estructurasGlobales;

    private final Deque<Set<String>>
            estructurasLocales;

    private PlanMemoriaY plan;
    private MarcoStackY marcoActual;

    private String funcionActual;
    private String ambitoActual;

    private int contadorBloques;

    public PlanificadorMemoriaY() {
        estructurasGlobales =
                new LinkedHashSet<>();

        estructurasLocales =
                new ArrayDeque<>();
    }

    public PlanMemoriaY planificar(
            List<ProgramaAst> programas
    ) {
        Objects.requireNonNull(
                programas,
                "Los programas son obligatorios"
        );

        plan = new PlanMemoriaY();

        estructurasGlobales.clear();
        estructurasLocales.clear();

        marcoActual = null;
        funcionActual = null;
        ambitoActual = "global";

        contadorBloques = 0;

        registrarNombresEstructurasGlobales(
                programas
        );

        registrarLayoutsGlobales(
                programas
        );

        for (ProgramaAst programa : programas) {
            for (NodoAst elemento
                    : programa.elementos()) {

                if (elemento instanceof FuncionAst funcion) {
                    planificarFuncion(funcion);
                }
            }
        }

        return plan;
    }

    private void registrarNombresEstructurasGlobales(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {
            for (NodoAst elemento
                    : programa.elementos()) {

                if (elemento
                        instanceof EstructuraAst estructura) {

                    estructurasGlobales.add(
                            estructura.nombre()
                    );
                }
            }
        }
    }

    private void registrarLayoutsGlobales(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {
            for (NodoAst elemento
                    : programa.elementos()) {

                if (elemento
                        instanceof EstructuraAst estructura) {

                    plan.agregarEstructura(
                            construirLayout(
                                    estructura,
                                    "global"
                            )
                    );
                }
            }
        }
    }

    private void planificarFuncion(
            FuncionAst funcion
    ) {
        funcionActual =
                funcion.nombre();

        marcoActual =
                new MarcoStackY(
                        funcion.nombre()
                );

        plan.agregarMarco(
                marcoActual
        );

        String tipoRetorno =
                funcion.retorno()
                        .map(tipo ->
                                tipo.nombre()
                        )
                        .orElse("void");

        marcoActual.reservar(
                "$retorno",
                ClaseSlotStackY.RETORNO,
                tipoRetorno,
                esReferenciaTipo(
                        tipoRetorno
                )
        );

        estructurasLocales.push(
                new HashSet<>()
        );

        for (ParametroAst parametro
                : funcion.parametros()) {

            planificarParametro(
                    parametro
            );
        }

        planificarBloque(
                funcion.cuerpo(),
                "funcion:"
                        + funcion.nombre()
        );

        estructurasLocales.pop();

        marcoActual = null;
        funcionActual = null;
        ambitoActual = "global";
    }

    private void planificarParametro(
            ParametroAst parametro
    ) {
        boolean referencia =
                parametro.modo()
                        != ParametroAst.Modo.VALOR;

        ClaseSlotStackY clase =
                referencia
                        ? ClaseSlotStackY.PARAMETRO_REFERENCIA
                        : ClaseSlotStackY.PARAMETRO_VALOR;

        String tipo =
                parametro.tipo().nombre();

        if (parametro.modo()
                == ParametroAst.Modo.REFERENCIA_ARREGLO) {

            tipo += "[]";
        }

        SlotStackY slot =
                marcoActual.reservar(
                        parametro.nombre(),
                        clase,
                        tipo,
                        referencia
                );

        plan.asociar(
                parametro,
                slot
        );
    }

    private void planificarBloque(
            List<SentenciaAst> sentencias,
            String nombreAmbito
    ) {
        String ambitoAnterior =
                ambitoActual;

        ambitoActual =
                nombreAmbito
                        + "/bloque:"
                        + contadorBloques++;

        estructurasLocales.push(
                new HashSet<>()
        );

        registrarEstructurasDelBloque(
                sentencias
        );

        for (SentenciaAst sentencia
                : sentencias) {

            planificarSentencia(
                    sentencia
            );
        }

        estructurasLocales.pop();

        ambitoActual =
                ambitoAnterior;
    }

    private void registrarEstructurasDelBloque(
            List<SentenciaAst> sentencias
    ) {
        Set<String> locales =
                estructurasLocales.peek();

        for (SentenciaAst sentencia
                : sentencias) {

            if (!(sentencia
                    instanceof EstructuraAst estructura)) {
                continue;
            }

            locales.add(
                    estructura.nombre()
            );

            plan.agregarEstructura(
                    construirLayout(
                            estructura,
                            ambitoActual
                    )
            );
        }
    }

    private void planificarSentencia(
            SentenciaAst sentencia
    ) {
        if (sentencia instanceof EstructuraAst) {
            return;
        }

        if (sentencia
                instanceof DeclaracionVariableAst declaracion) {

            planificarDeclaracion(
                    declaracion
            );

            return;
        }

        if (sentencia instanceof SiAst condicion) {

            for (SiAst.Rama rama
                    : condicion.ramas()) {

                planificarBloque(
                        rama.cuerpo(),
                        ambitoActual + "/si"
                );
            }

            if (!condicion.contrario().isEmpty()) {
                planificarBloque(
                        condicion.contrario(),
                        ambitoActual + "/contrario"
                );
            }

            return;
        }

        if (sentencia instanceof ElegirAst elegir) {

            for (ElegirAst.Caso caso
                    : elegir.casos()) {

                planificarBloque(
                        caso.cuerpo(),
                        ambitoActual + "/caso"
                );
            }

            if (!elegir.siempre().isEmpty()) {
                planificarBloque(
                        elegir.siempre(),
                        ambitoActual + "/siempre"
                );
            }

            return;
        }

        if (sentencia instanceof ParaAst para) {

            estructurasLocales.push(
                    new HashSet<>()
            );

            String anterior =
                    ambitoActual;

            ambitoActual =
                    anterior
                            + "/para:"
                            + contadorBloques++;

            planificarSentencia(
                    para.inicializacion()
            );

            planificarBloque(
                    para.cuerpo(),
                    ambitoActual + "/cuerpo"
            );

            planificarSentencia(
                    para.actualizacion()
            );

            ambitoActual =
                    anterior;

            estructurasLocales.pop();

            return;
        }

        if (sentencia
                instanceof MientrasAst mientras) {

            planificarBloque(
                    mientras.cuerpo(),
                    ambitoActual + "/mientras"
            );

            return;
        }

        if (sentencia
                instanceof HacerMientrasAst hacer) {

            planificarBloque(
                    hacer.cuerpo(),
                    ambitoActual + "/hacer"
            );
        }
    }

    private void planificarDeclaracion(
            DeclaracionVariableAst declaracion
    ) {
        boolean referencia =
                declaracion.esArreglo()
                        || esReferenciaTipo(
                        declaracion.tipo()
                                .nombre()
                );

        ClaseSlotStackY clase =
                referencia
                        ? ClaseSlotStackY.VARIABLE_REFERENCIA
                        : ClaseSlotStackY.VARIABLE_VALOR;

        String tipo =
                declaracion.tipo()
                        .nombre();

        if (declaracion.esArreglo()) {
            tipo += "[]".repeat(
                    declaracion.dimensiones()
                            .size()
            );
        }

        SlotStackY slot =
                marcoActual.reservar(
                        declaracion.nombre(),
                        clase,
                        tipo,
                        referencia
                );

        plan.asociar(
                declaracion,
                slot
        );
    }

    private LayoutEstructuraY construirLayout(
            EstructuraAst estructura,
            String ambito
    ) {
        List<CampoHeapY> campos =
                new ArrayList<>();

        int desplazamiento = 0;

        for (AtributoEstructuraAst atributo
                : estructura.atributos()) {

            boolean referencia =
                    atributo.esArreglo()
                            || esReferenciaTipo(
                            atributo.tipo()
                                    .nombre()
                    );

            String tipo =
                    atributo.tipo()
                            .nombre();

            if (atributo.esArreglo()) {
                tipo += "[]".repeat(
                        atributo.dimensiones()
                                .size()
                );
            }

            campos.add(
                    new CampoHeapY(
                            atributo.nombre(),
                            desplazamiento,
                            tipo,
                            referencia,
                            atributo.dimensiones()
                                    .size()
                    )
            );

            desplazamiento++;
        }

        return new LayoutEstructuraY(
                estructura.nombre(),
                ambito,
                campos
        );
    }

    private boolean esReferenciaTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {
            return false;
        }

        if (tipo.endsWith("[]")) {
            return true;
        }

        if (TIPOS_PRIMITIVOS.contains(tipo)
                || tipo.equals("void")) {
            return false;
        }

        for (Set<String> ambito
                : estructurasLocales) {

            if (ambito.contains(tipo)) {
                return true;
            }
        }

        return estructurasGlobales.contains(
                tipo
        );
    }
}