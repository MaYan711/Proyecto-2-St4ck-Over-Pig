package com.compi2.contacto.memoria;

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
import com.compi2.contacto.ast.piglatin.PAst;
import com.compi2.contacto.ast.zetariano.ZAst;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlanificadorMemoriaProyecto {

    private final Map<String, MarcoStackProyecto> marcos;
    private final Map<String, LayoutHeapProyecto> layouts;
    private final Map<String, String> padresZ;

    public PlanificadorMemoriaProyecto() {
        marcos = new LinkedHashMap<>();
        layouts = new LinkedHashMap<>();
        padresZ = new LinkedHashMap<>();
    }

    public PlanMemoriaProyecto planificar(
            List<ProgramaAst> programasY,
            List<ProgramaAst> programasZ,
            List<ProgramaAst> programasPig
    ) {
        Objects.requireNonNull(programasY);
        Objects.requireNonNull(programasZ);
        Objects.requireNonNull(programasPig);

        marcos.clear();
        layouts.clear();
        padresZ.clear();

        planificarLayoutsY(
                programasY
        );

        planificarLayoutsZ(
                programasZ
        );

        planificarMarcosY(
                programasY
        );

        planificarMarcosZ(
                programasZ
        );

        planificarMarcosPig(
                programasPig
        );

        return new PlanMemoriaProyecto(
                marcos,
                layouts,
                padresZ
        );
    }

    private void planificarLayoutsY(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {

            for (NodoAst nodo : programa.elementos()) {

                if (nodo instanceof EstructuraAst estructura) {
                    registrarLayoutY(
                            "Y::" + estructura.nombre(),
                            estructura
                    );
                }

                if (nodo instanceof FuncionAst funcion) {
                    buscarEstructurasLocalesY(
                            funcion.cuerpo(),
                            "Y::"
                                    + funcion.nombre()
                                    + "::"
                    );
                }
            }
        }
    }

    private void registrarLayoutY(
            String nombre,
            EstructuraAst estructura
    ) {
        List<LayoutHeapProyecto.Campo> campos =
                new ArrayList<>();

        int desplazamiento = 0;

        for (var atributo
                : estructura.atributos()) {

            String tipo =
                    atributo.tipo()
                            .nombre()
                            + "[]".repeat(
                            atributo.dimensiones()
                                    .size()
                    );

            campos.add(
                    new LayoutHeapProyecto.Campo(
                            atributo.nombre(),
                            tipo,
                            desplazamiento++,
                            atributo.posicion()
                    )
            );
        }

        layouts.put(
                nombre,
                new LayoutHeapProyecto(
                        nombre,
                        LayoutHeapProyecto.Clase.ESTRUCTURA_Y,
                        campos
                )
        );
    }

    private void buscarEstructurasLocalesY(
            List<SentenciaAst> sentencias,
            String prefijo
    ) {
        for (SentenciaAst sentencia
                : sentencias) {

            if (sentencia
                    instanceof EstructuraAst estructura) {

                registrarLayoutY(
                        prefijo
                                + estructura.nombre(),
                        estructura
                );

                continue;
            }

            if (sentencia instanceof SiAst si) {

                for (SiAst.Rama rama
                        : si.ramas()) {

                    buscarEstructurasLocalesY(
                            rama.cuerpo(),
                            prefijo
                    );
                }

                buscarEstructurasLocalesY(
                        si.contrario(),
                        prefijo
                );

                continue;
            }

            if (sentencia instanceof ElegirAst elegir) {

                for (ElegirAst.Caso caso
                        : elegir.casos()) {

                    buscarEstructurasLocalesY(
                            caso.cuerpo(),
                            prefijo
                    );
                }

                buscarEstructurasLocalesY(
                        elegir.siempre(),
                        prefijo
                );

                continue;
            }

            if (sentencia instanceof ParaAst para) {

                buscarEstructurasLocalesY(
                        para.cuerpo(),
                        prefijo
                );

                continue;
            }

            if (sentencia instanceof MientrasAst mientras) {

                buscarEstructurasLocalesY(
                        mientras.cuerpo(),
                        prefijo
                );

                continue;
            }

            if (sentencia instanceof HacerMientrasAst hacer) {

                buscarEstructurasLocalesY(
                        hacer.cuerpo(),
                        prefijo
                );
            }
        }
    }

    private void planificarLayoutsZ(
            List<ProgramaAst> programas
    ) {
        Map<String, ZAst.Clase> clases =
                new LinkedHashMap<>();

        for (ProgramaAst programa : programas) {
            for (NodoAst nodo : programa.elementos()) {
                if (nodo instanceof ZAst.Clase clase) {
                    clases.putIfAbsent(
                            clase.nombre(),
                            clase
                    );
                }
            }
        }

        for (ZAst.Clase clase : clases.values()) {
            clase.padre().ifPresent(padre -> padresZ.put(clase.nombre(), padre));
            registrarLayoutZ(
                    clase,
                    clases,
                    new HashSet<>()
            );
        }
    }

    private LayoutHeapProyecto registrarLayoutZ(
            ZAst.Clase clase,
            Map<String, ZAst.Clase> clases,
            Set<String> visitadas
    ) {
        String nombre = "Z::" + clase.nombre();
        LayoutHeapProyecto existente = layouts.get(nombre);

        if (existente != null) {
            return existente;
        }

        if (!visitadas.add(clase.nombre())) {
            throw new IllegalStateException(
                    "No se puede planificar herencia circular: "
                            + clase.nombre()
            );
        }

        List<LayoutHeapProyecto.Campo> campos =
                new ArrayList<>();

        if (clase.padre().isPresent()) {
            String nombrePadre = clase.padre().orElseThrow();
            ZAst.Clase padre = clases.get(nombrePadre);

            if (padre != null) {
                LayoutHeapProyecto heredado = registrarLayoutZ(
                        padre,
                        clases,
                        visitadas
                );

                campos.addAll(heredado.campos());
            }
        }

        int desplazamiento = campos.size();

        for (ZAst.Miembro miembro : clase.miembros()) {
            if (miembro instanceof ZAst.Atributo atributo) {
                campos.add(
                        new LayoutHeapProyecto.Campo(
                                atributo.nombre(),
                                atributo.tipo().nombreCompleto(),
                                desplazamiento++,
                                atributo.posicion()
                        )
                );
            }
        }

        LayoutHeapProyecto resultado = new LayoutHeapProyecto(
                nombre,
                LayoutHeapProyecto.Clase.OBJETO_Z,
                campos
        );

        layouts.put(nombre, resultado);
        visitadas.remove(clase.nombre());

        return resultado;
    }

    private void planificarMarcosY(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {

            for (NodoAst nodo : programa.elementos()) {

                if (!(nodo
                        instanceof FuncionAst funcion)) {
                    continue;
                }

                ConstructorMarco constructor =
                        new ConstructorMarco(
                                funcion.nombre()
                        );

                String retorno =
                        funcion.retorno()
                                .map(
                                        tipo ->
                                                tipo.nombre()
                                )
                                .orElse(
                                        "void"
                                );

                constructor.agregar(
                        "$retorno",
                        retorno,
                        SlotStackProyecto.Clase.RETORNO,
                        funcion.posicion()
                );

                for (ParametroAst parametro
                        : funcion.parametros()) {

                    String tipo =
                            parametro.tipo()
                                    .nombre();

                    if (parametro.modo()
                            == ParametroAst.Modo.REFERENCIA_ARREGLO) {

                        tipo += "[]";
                    }

                    constructor.agregar(
                            parametro.nombre(),
                            tipo,
                            SlotStackProyecto.Clase.PARAMETRO,
                            parametro.posicion()
                    );
                }

                recolectarLocalesY(
                        funcion.cuerpo(),
                        constructor
                );

                marcos.put(
                        funcion.nombre(),
                        constructor.construir()
                );
            }
        }
    }

    private void recolectarLocalesY(
            List<SentenciaAst> sentencias,
            ConstructorMarco marco
    ) {
        for (SentenciaAst sentencia
                : sentencias) {

            if (sentencia
                    instanceof DeclaracionVariableAst declaracion) {

                marco.agregar(
                        declaracion.nombre(),
                        declaracion.tipo()
                                .nombre()
                                + "[]".repeat(
                                declaracion.dimensiones()
                                        .size()
                        ),
                        SlotStackProyecto.Clase.LOCAL,
                        declaracion.posicion()
                );

                continue;
            }

            if (sentencia instanceof SiAst si) {

                for (SiAst.Rama rama
                        : si.ramas()) {

                    recolectarLocalesY(
                            rama.cuerpo(),
                            marco
                    );
                }

                recolectarLocalesY(
                        si.contrario(),
                        marco
                );

                continue;
            }

            if (sentencia instanceof ElegirAst elegir) {

                for (ElegirAst.Caso caso
                        : elegir.casos()) {

                    recolectarLocalesY(
                            caso.cuerpo(),
                            marco
                    );
                }

                recolectarLocalesY(
                        elegir.siempre(),
                        marco
                );

                continue;
            }

            if (sentencia instanceof ParaAst para) {

                if (para.inicializacion()
                        instanceof DeclaracionVariableAst declaracion) {

                    marco.agregar(
                            declaracion.nombre(),
                            declaracion.tipo()
                                    .nombre()
                                    + "[]".repeat(
                                    declaracion.dimensiones()
                                            .size()
                            ),
                            SlotStackProyecto.Clase.LOCAL,
                            declaracion.posicion()
                    );
                }

                recolectarLocalesY(
                        para.cuerpo(),
                        marco
                );

                continue;
            }

            if (sentencia instanceof MientrasAst mientras) {

                recolectarLocalesY(
                        mientras.cuerpo(),
                        marco
                );

                continue;
            }

            if (sentencia instanceof HacerMientrasAst hacer) {

                recolectarLocalesY(
                        hacer.cuerpo(),
                        marco
                );
            }
        }
    }

    private void planificarMarcosZ(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {

            for (NodoAst nodo : programa.elementos()) {

                if (!(nodo
                        instanceof ZAst.Clase clase)) {

                    continue;
                }

                for (ZAst.Miembro miembro
                        : clase.miembros()) {

                    if (miembro
                            instanceof ZAst.Constructor constructor) {

                        planificarConstructorZ(
                                clase,
                                constructor
                        );

                        continue;
                    }

                    if (miembro
                            instanceof ZAst.Metodo metodo) {

                        planificarMetodoZ(
                                clase,
                                metodo
                        );
                    }
                }
            }
        }
    }

    private void planificarConstructorZ(
            ZAst.Clase clase,
            ZAst.Constructor constructor
    ) {
        String nombre =
                clase.nombre()
                        + ".<init>("
                        + tiposParametrosZ(
                        constructor.parametros()
                )
                        + ")";

        ConstructorMarco marco =
                new ConstructorMarco(
                        nombre
                );

        marco.agregar(
                "$retorno",
                clase.nombre(),
                SlotStackProyecto.Clase.RETORNO,
                constructor.posicion()
        );

        marco.agregar(
                "this",
                clase.nombre(),
                SlotStackProyecto.Clase.THIS,
                constructor.posicion()
        );

        for (ZAst.Parametro parametro
                : constructor.parametros()) {

            marco.agregar(
                    parametro.nombre(),
                    parametro.tipo()
                            .nombreCompleto(),
                    SlotStackProyecto.Clase.PARAMETRO,
                    parametro.posicion()
            );
        }

        recolectarLocalesZ(
                constructor.cuerpo(),
                marco
        );

        marcos.put(
                nombre,
                marco.construir()
        );
    }

    private void planificarMetodoZ(
            ZAst.Clase clase,
            ZAst.Metodo metodo
    ) {
        String nombre =
                clase.nombre()
                        + "."
                        + metodo.nombre()
                        + "("
                        + tiposParametrosZ(
                        metodo.parametros()
                )
                        + ")";

        ConstructorMarco marco =
                new ConstructorMarco(
                        nombre
                );

        String retorno =
                metodo.retorno()
                        .map(
                                ZAst.Tipo::nombreCompleto
                        )
                        .orElse(
                                "void"
                        );

        marco.agregar(
                "$retorno",
                retorno,
                SlotStackProyecto.Clase.RETORNO,
                metodo.posicion()
        );

        marco.agregar(
                "this",
                clase.nombre(),
                SlotStackProyecto.Clase.THIS,
                metodo.posicion()
        );

        for (ZAst.Parametro parametro
                : metodo.parametros()) {

            marco.agregar(
                    parametro.nombre(),
                    parametro.tipo()
                            .nombreCompleto(),
                    SlotStackProyecto.Clase.PARAMETRO,
                    parametro.posicion()
            );
        }

        recolectarLocalesZ(
                metodo.cuerpo(),
                marco
        );

        marcos.put(
                nombre,
                marco.construir()
        );
    }

    private void recolectarLocalesZ(
            ZAst.Sentencia sentencia,
            ConstructorMarco marco
    ) {
        if (sentencia
                instanceof ZAst.Declaracion declaracion) {

            marco.agregar(
                    declaracion.nombre(),
                    declaracion.tipo()
                            .nombreCompleto(),
                    SlotStackProyecto.Clase.LOCAL,
                    declaracion.posicion()
            );

            return;
        }

        if (sentencia
                instanceof ZAst.Bloque bloque) {

            for (ZAst.Sentencia interna
                    : bloque.sentencias()) {

                recolectarLocalesZ(
                        interna,
                        marco
                );
            }

            return;
        }

        if (sentencia instanceof ZAst.Si si) {

            recolectarLocalesZ(
                    si.entonces(),
                    marco
            );

            si.sino()
                    .ifPresent(
                            sino ->
                                    recolectarLocalesZ(
                                            sino,
                                            marco
                                    )
                    );

            return;
        }

        if (sentencia
                instanceof ZAst.Seleccion seleccion) {

            for (ZAst.CasoSeleccion caso
                    : seleccion.casos()) {

                for (ZAst.Sentencia interna
                        : caso.sentencias()) {

                    recolectarLocalesZ(
                            interna,
                            marco
                    );
                }
            }

            return;
        }

        if (sentencia instanceof ZAst.Para para) {

            para.inicializacion()
                    .ifPresent(
                            inicializacion -> {

                                if (inicializacion
                                        instanceof ZAst.Declaracion declaracion) {

                                    recolectarLocalesZ(
                                            declaracion,
                                            marco
                                    );
                                }
                            }
                    );

            recolectarLocalesZ(
                    para.cuerpo(),
                    marco
            );

            return;
        }

        if (sentencia instanceof ZAst.Mientras mientras) {

            recolectarLocalesZ(
                    mientras.cuerpo(),
                    marco
            );

            return;
        }

        if (sentencia
                instanceof ZAst.HacerMientras hacer) {

            recolectarLocalesZ(
                    hacer.cuerpo(),
                    marco
            );
        }
    }

    private void planificarMarcosPig(
            List<ProgramaAst> programas
    ) {
        for (ProgramaAst programa : programas) {

            for (NodoAst nodo : programa.elementos()) {

                if (!(nodo
                        instanceof PAst.Programa pig)) {

                    continue;
                }

                ConstructorMarco marco =
                        new ConstructorMarco(
                                "MAIOR"
                        );

                marco.agregar(
                        "$retorno",
                        "void",
                        SlotStackProyecto.Clase.RETORNO,
                        pig.posicion()
                );

                for (PAst.Declaracion global
                        : pig.globales()) {

                    marco.agregar(
                            global.nombre(),
                            global.tipo()
                                    .nombreCompleto(),
                            SlotStackProyecto.Clase.GLOBAL_PIG,
                            global.posicion()
                    );
                }

                recolectarLocalesPig(
                        pig.principal(),
                        marco
                );

                marcos.put(
                        "MAIOR",
                        marco.construir()
                );
            }
        }
    }

    private void recolectarLocalesPig(
            List<PAst.Sentencia> sentencias,
            ConstructorMarco marco
    ) {
        for (PAst.Sentencia sentencia
                : sentencias) {

            if (sentencia
                    instanceof PAst.Declaracion declaracion) {

                marco.agregar(
                        declaracion.nombre(),
                        declaracion.tipo()
                                .nombreCompleto(),
                        SlotStackProyecto.Clase.LOCAL,
                        declaracion.posicion()
                );

                continue;
            }

            if (sentencia instanceof PAst.Si si) {

                for (PAst.RamaSi rama
                        : si.ramas()) {

                    recolectarLocalesPig(
                            rama.cuerpo(),
                            marco
                    );
                }

                continue;
            }

            if (sentencia
                    instanceof PAst.Mientras mientras) {

                recolectarLocalesPig(
                        mientras.cuerpo(),
                        marco
                );

                continue;
            }

            if (sentencia
                    instanceof PAst.HacerMientras hacer) {

                recolectarLocalesPig(
                        hacer.cuerpo(),
                        marco
                );

                continue;
            }

            if (sentencia instanceof PAst.Para para) {

                para.inicializacion()
                        .ifPresent(
                                inicializacion -> {

                                    if (inicializacion
                                            instanceof PAst.Declaracion declaracion) {

                                        marco.agregar(
                                                declaracion.nombre(),
                                                declaracion.tipo()
                                                        .nombreCompleto(),
                                                SlotStackProyecto.Clase.LOCAL,
                                                declaracion.posicion()
                                        );
                                    }
                                }
                        );

                recolectarLocalesPig(
                        para.cuerpo(),
                        marco
                );
            }
        }
    }

    private String tiposParametrosZ(
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

    private static final class ConstructorMarco {

        private final String nombre;
        private final List<SlotStackProyecto> slots;
        private int desplazamiento;

        private ConstructorMarco(
                String nombre
        ) {
            this.nombre =
                    Objects.requireNonNull(
                            nombre
                    );

            slots =
                    new ArrayList<>();

            desplazamiento = 0;
        }

        private void agregar(
                String nombre,
                String tipo,
                SlotStackProyecto.Clase clase,
                com.compi2.contacto.ast.PosicionFuente posicion
        ) {
            slots.add(
                    new SlotStackProyecto(
                            nombre,
                            tipo,
                            desplazamiento++,
                            clase,
                            posicion
                    )
            );
        }

        private MarcoStackProyecto construir() {
            return new MarcoStackProyecto(
                    nombre,
                    slots
            );
        }
    }
}