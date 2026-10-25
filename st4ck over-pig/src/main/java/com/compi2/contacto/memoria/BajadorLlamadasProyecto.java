package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BajadorLlamadasProyecto {

    private PlanMemoriaProyecto plan;
    private ProgramaIntermedio destino;
    private MarcoStackProyecto marcoActual;
    private String funcionActual;
    private final List<ParametroPendiente> parametrosPendientes;
    private final List<FuncionDisponible> funcionesDisponibles;
    private int siguienteTemporal;
    private int siguienteTemporalObjeto;
    private int siguienteEtiquetaVirtual;
    private Map<String, Integer> tiposObjetoZ;

    public BajadorLlamadasProyecto() {
        parametrosPendientes = new ArrayList<>();
        funcionesDisponibles = new ArrayList<>();
    }

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen,
            PlanMemoriaProyecto plan
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        this.plan = Objects.requireNonNull(
                plan,
                "El plan de memoria es obligatorio"
        );

        destino = new ProgramaIntermedio();
        marcoActual = null;
        funcionActual = null;
        parametrosPendientes.clear();
        funcionesDisponibles.clear();
        siguienteTemporal = 0;
        siguienteTemporalObjeto = 0;
        siguienteEtiquetaVirtual = 0;
        tiposObjetoZ = new LinkedHashMap<>();
        Set<String> clases = new TreeSet<>();
        for (String nombre : plan.layoutsHeap().keySet()) {
            if (nombre.startsWith("Z::")) {
                clases.add(nombre.substring(3));
            }
        }
        int siguienteTipo = 1;
        for (String clase : clases) {
            tiposObjetoZ.put(clase, siguienteTipo++);
        }

        registrarFunciones(origen);

        for (Cuarteta cuarteta : origen.cuartetas()) {
            procesar(cuarteta);
        }

        vaciarParametrosRestantes();

        return destino;
    }

    private void registrarFunciones(
            ProgramaIntermedio programa
    ) {
        for (Cuarteta cuarteta : programa.cuartetas()) {
            if (cuarteta.operador()
                    != OperadorCuarteta.INICIO_FUNCION) {
                continue;
            }

            int cantidadParametros =
                    entero(
                            cuarteta.resultado(),
                            -1
                    );

            if (cantidadParametros < 0
                    || esVacio(cuarteta.argumento1())) {
                continue;
            }

            funcionesDisponibles.add(
                    new FuncionDisponible(
                            cuarteta.argumento1(),
                            cantidadParametros
                    )
            );
        }
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        switch (cuarteta.operador()) {
            case INICIO_FUNCION ->
                    iniciarFuncion(cuarteta);

            case FIN_FUNCION ->
                    finalizarFuncion(cuarteta);

            case PARAMETRO ->
                    registrarParametro(cuarteta);

            case LLAMAR ->
                    bajarLlamada(cuarteta);

            case NUEVO_OBJETO ->
                    bajarNuevoObjeto(cuarteta);

            default ->
                    copiar(cuarteta);
        }
    }

    private void iniciarFuncion(
            Cuarteta cuarteta
    ) {
        vaciarParametrosRestantes();

        funcionActual =
                cuarteta.argumento1();

        marcoActual =
                plan.marco(
                        funcionActual
                ).orElse(null);

        copiar(cuarteta);
    }

    private void finalizarFuncion(
            Cuarteta cuarteta
    ) {
        vaciarParametrosRestantes();

        copiar(cuarteta);

        marcoActual = null;
        funcionActual = null;
    }

    private void registrarParametro(
            Cuarteta cuarteta
    ) {
        parametrosPendientes.add(
                new ParametroPendiente(
                        cuarteta.argumento1(),
                        cuarteta.argumento2()
                )
        );
    }

    private void bajarLlamada(
            Cuarteta cuarteta
    ) {
        int cantidadExplicita =
                entero(
                        cuarteta.argumento2(),
                        0
                );

        List<ParametroPendiente> parametros =
                consumirUltimos(
                        cantidadExplicita
                );

        String destinoLlamada =
                cuarteta.argumento1();

        boolean llamadaConstructorPadre =
                esLlamadaConstructorPadre(destinoLlamada);

        Optional<MarcoStackProyecto> marcoDestino =
                plan.marco(
                        destinoLlamada
                );

        if (llamadaConstructorPadre && marcoDestino.isEmpty()
                && cantidadExplicita == 0) {
            return;
        }

        boolean llamadaInternaZ = false;
        boolean llamadaSuperZ = destinoLlamada != null
                && destinoLlamada.startsWith("super.");
        ReceptorMetodoZ receptorExplicito = null;

        if (marcoDestino.isEmpty()) {
            Optional<ReceptorMetodoZ> receptor =
                    resolverReceptorMetodoZ(
                            destinoLlamada,
                            cantidadExplicita
                    );

            if (receptor.isPresent()) {
                ReceptorMetodoZ resuelto =
                        receptor.orElseThrow();

                Optional<MarcoStackProyecto> marcoResuelto =
                        plan.marco(
                                resuelto.funcion()
                        );

                if (marcoResuelto.isPresent()) {
                    receptorExplicito = resuelto;
                    destinoLlamada =
                            resuelto.funcion();
                    marcoDestino =
                            marcoResuelto;
                }
            }
        }

        if (marcoDestino.isEmpty()
                && receptorExplicito == null) {

            Optional<String> resuelto = llamadaSuperZ
                    ? resolverMetodoEnClaseZ(
                            plan.padresZ().get(claseFuncionActual()),
                            destinoLlamada, cantidadExplicita)
                    : resolverMetodoInternoZ(
                            destinoLlamada, cantidadExplicita);

            if (resuelto.isPresent()) {
                destinoLlamada =
                        resuelto.orElseThrow();

                marcoDestino =
                        plan.marco(
                                destinoLlamada
                        );

                llamadaInternaZ =
                        marcoDestino.isPresent();
            }
        }

        if (marcoActual == null
                || marcoDestino.isEmpty()) {

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(cuarteta);

            return;
        }

        parametros =
                materializarParametros(
                        parametros
                );

        int cantidadTotal =
                cantidadExplicita;

        if (receptorExplicito != null) {
            String referenciaReceptor =
                    materializarReceptor(
                            receptorExplicito.receptor()
                    );

            if (referenciaReceptor == null) {
                copiarParametrosSimbolicos(
                        parametros
                );

                copiar(cuarteta);

                return;
            }

            List<ParametroPendiente> completos =
                    new ArrayList<>();

            completos.add(
                    new ParametroPendiente(
                            referenciaReceptor,
                            "0"
                    )
            );

            for (int indice = 0;
                 indice < parametros.size();
                 indice++) {

                ParametroPendiente parametro =
                        parametros.get(indice);

                int indiceOriginal =
                        entero(
                                parametro.indice(),
                                indice
                        );

                completos.add(
                        new ParametroPendiente(
                                parametro.valor(),
                                String.valueOf(
                                        indiceOriginal + 1
                                )
                        )
                );
            }

            parametros =
                    completos;

            cantidadTotal =
                    cantidadExplicita + 1;

        } else if (llamadaInternaZ || llamadaConstructorPadre) {
            Optional<SlotStackProyecto> thisSlot =
                    marcoActual.thisSlot();

            if (thisSlot.isEmpty()) {
                copiarParametrosSimbolicos(
                        parametros
                );

                copiar(cuarteta);

                return;
            }

            String referenciaThis =
                    nuevoTemporal();

            agregar(
                    OperadorCuarteta.LEER_STACK,
                    thisSlot.orElseThrow()
                            .direccion(),
                    null,
                    referenciaThis
            );

            List<ParametroPendiente> completos =
                    new ArrayList<>();

            completos.add(
                    new ParametroPendiente(
                            referenciaThis,
                            "0"
                    )
            );

            for (int indice = 0;
                 indice < parametros.size();
                 indice++) {

                ParametroPendiente parametro =
                        parametros.get(indice);

                int indiceOriginal =
                        entero(
                                parametro.indice(),
                                indice
                        );

                completos.add(
                        new ParametroPendiente(
                                parametro.valor(),
                                String.valueOf(
                                        indiceOriginal + 1
                                )
                        )
                );
            }

            parametros =
                    completos;

            cantidadTotal =
                    cantidadExplicita + 1;
        }

        int tamanoLlamador =
                marcoActual.tamano();

        for (int indiceLista = 0;
             indiceLista < parametros.size();
             indiceLista++) {

            ParametroPendiente parametro =
                    parametros.get(
                            indiceLista
                    );

            int indiceParametro =
                    entero(
                            parametro.indice(),
                            indiceLista
                    );

            int desplazamiento =
                    tamanoLlamador
                            + 1
                            + indiceParametro;

            agregar(
                    OperadorCuarteta.ESCRIBIR_STACK,
                    "P+" + desplazamiento,
                    parametro.valor(),
                    null
            );
        }

        moverPAdelante(
                tamanoLlamador
        );

        if (!emitirLlamadaVirtual(destinoLlamada, cantidadTotal,
                marcoDestino.orElseThrow(), llamadaConstructorPadre || llamadaSuperZ)) {
            agregar(
                    OperadorCuarteta.LLAMAR,
                    destinoLlamada,
                    String.valueOf(cantidadTotal),
                    null
            );
        }

        MarcoStackProyecto destinoMarco =
                marcoDestino.orElseThrow();

        boolean retornaValor =
                destinoMarco.retorno()
                        .map(
                                slot ->
                                        !slot.tipo()
                                                .equalsIgnoreCase(
                                                        "void"
                                                )
                        )
                        .orElse(false);

        if (retornaValor
                && !esVacio(
                cuarteta.resultado()
        )) {

            agregar(
                    OperadorCuarteta.LEER_STACK,
                    "P+0",
                    null,
                    cuarteta.resultado()
            );
        }

        moverPAtras(
                tamanoLlamador
        );
    }

    private boolean esLlamadaConstructorPadre(String destino) {
        String clase = claseFuncionActual();
        String claseDestino = extraerClaseConstructor(destino);

        return clase != null
                && claseDestino != null
                && !claseDestino.equals(clase)
                && funcionActual.contains(".<init>(");
    }

    private Optional<ReceptorMetodoZ> resolverReceptorMetodoZ(
            String destino,
            int cantidadExplicita
    ) {
        if (destino != null && destino.startsWith("@receptor@")) {
            int separador = destino.indexOf('@', "@receptor@".length());
            if (separador < 0) {
                return Optional.empty();
            }
            String referencia = destino.substring("@receptor@".length(), separador);
            String funcion = destino.substring(separador + 1);
            if (!esIdentificadorSimple(referencia)
                    || plan.marco(funcion).isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ReceptorMetodoZ(
                    funcion,
                    new OrigenReceptorZ(referencia, "", TipoOrigenReceptorZ.TEMPORAL,
                            null, null)
            ));
        }

        String receptor =
                nombreReceptor(
                        destino
                );

        if (receptor == null) {
            return Optional.empty();
        }

        Optional<OrigenReceptorZ> origen =
                resolverOrigenReceptorZ(
                        receptor
                );

        if (origen.isEmpty()) {
            return Optional.empty();
        }

        OrigenReceptorZ receptorResuelto =
                origen.orElseThrow();

        Optional<String> funcion =
                resolverMetodoEnClaseZ(
                        receptorResuelto.tipo(),
                        destino,
                        cantidadExplicita
                );

        if (funcion.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new ReceptorMetodoZ(
                        funcion.orElseThrow(),
                        receptorResuelto
                )
        );
    }

    private Optional<OrigenReceptorZ> resolverOrigenReceptorZ(
            String receptor
    ) {
        if (marcoActual == null
                || receptor == null
                || receptor.isBlank()) {
            return Optional.empty();
        }

        if (receptor.equals("this")) {
            String clase =
                    claseFuncionActual();

            Optional<SlotStackProyecto> thisSlot =
                    marcoActual.thisSlot();

            if (clase == null
                    || thisSlot.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(
                    new OrigenReceptorZ(
                            receptor,
                            clase,
                            TipoOrigenReceptorZ.THIS,
                            thisSlot.orElseThrow(),
                            null
                    )
            );
        }

        Optional<SlotStackProyecto> slot =
                marcoActual.buscar(
                        receptor
                );

        if (slot.isPresent()) {
            SlotStackProyecto encontrado =
                    slot.orElseThrow();

            String clase =
                    claseZDesdeTipo(
                            encontrado.tipo()
                    );

            if (clase != null) {
                return Optional.of(
                        new OrigenReceptorZ(
                                receptor,
                                clase,
                                TipoOrigenReceptorZ.STACK,
                                encontrado,
                                null
                        )
                );
            }
        }

        String claseActual =
                claseFuncionActual();

        if (claseActual == null) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                plan.layout(
                        "Z::" + claseActual
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto.Campo> campo =
                layout.orElseThrow()
                        .buscarCampo(
                                receptor
                        );

        if (campo.isEmpty()) {
            return Optional.empty();
        }

        LayoutHeapProyecto.Campo encontrado =
                campo.orElseThrow();

        String claseCampo =
                claseZDesdeTipo(
                        encontrado.tipo()
                );

        if (claseCampo == null) {
            return Optional.empty();
        }

        return Optional.of(
                new OrigenReceptorZ(
                        receptor,
                        claseCampo,
                        TipoOrigenReceptorZ.CAMPO,
                        null,
                        encontrado
                )
        );
    }

    private String materializarReceptor(
            OrigenReceptorZ receptor
    ) {
        return switch (receptor.origen()) {
            case STACK ->
                    leerSlot(
                            receptor.slot()
                    );

            case THIS ->
                    leerSlot(
                            receptor.slot()
                    );

            case CAMPO ->
                    leerCampoThis(
                            receptor.campo()
                    );

            case TEMPORAL -> receptor.nombre();
        };
    }

    private Optional<String> resolverMetodoInternoZ(
            String destino,
            int cantidadExplicita
    ) {
        String clase =
                claseFuncionActual();

        if (clase == null) {
            return Optional.empty();
        }

        return resolverMetodoEnClaseZ(
                clase,
                destino,
                cantidadExplicita
        );
    }

    private Optional<String> resolverMetodoEnClaseZ(
            String clase,
            String destino,
            int cantidadExplicita
    ) {
        String claseZ =
                claseZDesdeTipo(
                        clase
                );

        if (claseZ == null) {
            return Optional.empty();
        }

        String metodo =
                nombreMetodo(
                        destino
                );

        if (metodo == null
                || metodo.equals("<init>")) {
            return Optional.empty();
        }

        int cantidadEsperada =
                cantidadExplicita + 1;

        List<FuncionDisponible> candidatos =
                funcionesDisponibles.stream()
                        .filter(
                                funcion ->
                                        funcion.nombre()
                                                .startsWith(
                                                        claseZ + "."
                                                )
                        )
                        .filter(
                                funcion ->
                                        metodo.equals(
                                                nombreMetodo(
                                                        funcion.nombre()
                                                )
                                        )
                        )
                        .filter(
                                funcion ->
                                        funcion.cantidadParametros()
                                                == cantidadEsperada
                        )
                        .toList();

        if (candidatos.size() == 1) {
            return Optional.of(
                    candidatos.get(0)
                            .nombre()
            );
        }

        if (candidatos.size() > 1
                && destino.contains("(")) {

            String firma =
                    destino;

            int punto =
                    firma.lastIndexOf('.');

            if (punto >= 0) {
                firma =
                        firma.substring(
                                punto + 1
                        );
            }

            String firmaFinal =
                    firma;

            List<FuncionDisponible> exactos =
                    candidatos.stream()
                            .filter(
                                    funcion ->
                                            funcion.nombre()
                                                    .endsWith(
                                                            "."
                                                                    + firmaFinal
                                                    )
                            )
                            .toList();

            if (exactos.size() == 1) {
                return Optional.of(
                        exactos.get(0)
                                .nombre()
                );
            }
        }

        String padre = plan.padresZ().get(claseZ);
        if (padre != null && !padre.equals(claseZ)) {
            return resolverMetodoEnClaseZ(padre, destino, cantidadExplicita);
        }
        return Optional.empty();
    }

    private String claseFuncionActual() {
        if (funcionActual == null
                || funcionActual.equals("MAIOR")) {
            return null;
        }

        int punto =
                funcionActual.indexOf('.');

        if (punto <= 0) {
            return null;
        }

        String clase =
                funcionActual.substring(
                        0,
                        punto
                );

        return plan.layout(
                "Z::" + clase
        ).isPresent()
                ? clase
                : null;
    }

    private String claseZDesdeTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {
            return null;
        }

        String candidato =
                tipo.trim();

        if (plan.layout(
                "Z::" + candidato
        ).isPresent()) {
            return candidato;
        }

        return null;
    }

    private String nombreReceptor(
            String nombre
    ) {
        if (nombre == null
                || nombre.isBlank()) {
            return null;
        }

        int punto =
                nombre.lastIndexOf('.');

        if (punto <= 0) {
            return null;
        }

        String receptor =
                nombre.substring(
                        0,
                        punto
                );

        if (!esIdentificadorSimple(
                receptor
        )) {
            return null;
        }

        return receptor;
    }

    private String nombreMetodo(
            String nombre
    ) {
        if (nombre == null
                || nombre.isBlank()) {
            return null;
        }

        String resultado =
                nombre;

        int punto =
                resultado.lastIndexOf('.');

        if (punto >= 0
                && punto < resultado.length() - 1) {

            resultado =
                    resultado.substring(
                            punto + 1
                    );
        }

        int parentesis =
                resultado.indexOf('(');

        if (parentesis >= 0) {
            resultado =
                    resultado.substring(
                            0,
                            parentesis
                    );
        }

        return resultado;
    }

    private List<ParametroPendiente> materializarParametros(
            List<ParametroPendiente> parametros
    ) {
        List<ParametroPendiente> resultado =
                new ArrayList<>();

        for (ParametroPendiente parametro
                : parametros) {

            resultado.add(
                    new ParametroPendiente(
                            materializarValor(
                                    parametro.valor()
                            ),
                            parametro.indice()
                    )
            );
        }

        return resultado;
    }

    private String materializarValor(
            String valor
    ) {
        if (valor == null
                || valor.isBlank()
                || valor.equals("-")) {
            return valor;
        }

        if (valor.equals("null")) {
            return "-1";
        }

        if (!esIdentificadorSimple(
                valor
        )) {
            return valor;
        }

        if (valor.equals("this")) {
            if (marcoActual == null) {
                return valor;
            }

            Optional<SlotStackProyecto> thisSlot =
                    marcoActual.thisSlot();

            return thisSlot
                    .map(this::leerSlot)
                    .orElse(valor);
        }

        if (marcoActual != null) {
            Optional<SlotStackProyecto> slot =
                    marcoActual.buscar(
                            valor
                    );

            if (slot.isPresent()) {
                return leerSlot(
                        slot.orElseThrow()
                );
            }
        }

        String claseActual =
                claseFuncionActual();

        if (claseActual == null) {
            return valor;
        }

        Optional<LayoutHeapProyecto> layout =
                plan.layout(
                        "Z::" + claseActual
                );

        if (layout.isEmpty()) {
            return valor;
        }

        Optional<LayoutHeapProyecto.Campo> campo =
                layout.orElseThrow()
                        .buscarCampo(
                                valor
                        );

        if (campo.isEmpty()) {
            return valor;
        }

        String materializado =
                leerCampoThis(
                        campo.orElseThrow()
                );

        return materializado == null
                ? valor
                : materializado;
    }

    private String leerSlot(
            SlotStackProyecto slot
    ) {
        if (slot == null) {
            return null;
        }

        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_STACK,
                slot.direccion(),
                null,
                temporal
        );

        return temporal;
    }

    private String leerCampoThis(
            LayoutHeapProyecto.Campo campo
    ) {
        if (campo == null
                || marcoActual == null) {
            return null;
        }

        Optional<SlotStackProyecto> thisSlot =
                marcoActual.thisSlot();

        if (thisSlot.isEmpty()) {
            return null;
        }

        String referenciaThis =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_STACK,
                thisSlot.orElseThrow()
                        .direccion(),
                null,
                referenciaThis
        );

        String valorCampo =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_HEAP,
                referenciaThis
                        + "+"
                        + campo.desplazamiento(),
                null,
                valorCampo
        );

        return valorCampo;
    }

    private boolean esIdentificadorSimple(
            String valor
    ) {
        if (valor == null
                || valor.isBlank()) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(
                valor.charAt(0)
        )) {
            return false;
        }

        for (int indice = 1;
             indice < valor.length();
             indice++) {

            if (!Character.isJavaIdentifierPart(
                    valor.charAt(indice)
            )) {
                return false;
            }
        }

        return true;
    }

    private void registrarTipoObjetoZ(String referencia, String clase) {
        Integer tipo = tiposObjetoZ.get(clase);
        if (tipo != null) {
            agregar(OperadorCuarteta.ESCRIBIR_TIPO_OBJETO,
                    referencia, String.valueOf(tipo), null);
        }
    }

    private boolean emitirLlamadaVirtual(
            String destinoBase,
            int cantidad,
            MarcoStackProyecto marcoDestino,
            boolean constructorPadre
    ) {
        if (constructorPadre || marcoDestino.thisSlot().isEmpty()
                || cantidad < 1 || destinoBase == null
                || destinoBase.contains(".<init>(")) {
            return false;
        }

        int punto = destinoBase.indexOf('.');
        if (punto < 1) {
            return false;
        }
        String claseBase = destinoBase.substring(0, punto);
        String firma = destinoBase.substring(punto);
        if (!tiposObjetoZ.containsKey(claseBase)) {
            return false;
        }

        Map<String, String> opciones = new LinkedHashMap<>();
        for (String clase : tiposObjetoZ.keySet()) {
            if (clase.equals(claseBase)
                    || !heredaDeZ(clase, claseBase)) {
                continue;
            }
            String implementacion = resolverImplementacionVirtualZ(
                    clase, claseBase, firma);
            if (implementacion != null
                    && !implementacion.equals(destinoBase)) {
                opciones.put(clase, implementacion);
            }
        }
        if (opciones.isEmpty()) {
            return false;
        }

        String receptor = nuevoTemporal();
        String tipo = nuevoTemporal();
        agregar(OperadorCuarteta.LEER_STACK, "P+1", null, receptor);
        agregar(OperadorCuarteta.LEER_TIPO_OBJETO, receptor, null, tipo);

        Map<String, String> etiquetas = new LinkedHashMap<>();
        for (String clase : opciones.keySet()) {
            String etiqueta = nuevaEtiquetaVirtual();
            String condicion = nuevoTemporal();
            agregar(OperadorCuarteta.IGUALDAD, tipo,
                    String.valueOf(tiposObjetoZ.get(clase)), condicion);
            agregar(OperadorCuarteta.SALTAR_SI_VERDADERO,
                    condicion, null, etiqueta);
            etiquetas.put(clase, etiqueta);
        }

        String fin = nuevaEtiquetaVirtual();
        agregar(OperadorCuarteta.LLAMAR,
                destinoBase, String.valueOf(cantidad), null);
        agregar(OperadorCuarteta.SALTAR, null, null, fin);

        for (Map.Entry<String, String> entrada : etiquetas.entrySet()) {
            agregar(OperadorCuarteta.ETIQUETA, null, null, entrada.getValue());
            agregar(OperadorCuarteta.LLAMAR, opciones.get(entrada.getKey()),
                    String.valueOf(cantidad), null);
            agregar(OperadorCuarteta.SALTAR, null, null, fin);
        }
        agregar(OperadorCuarteta.ETIQUETA, null, null, fin);
        return true;
    }

    private boolean heredaDeZ(String descendiente, String ancestro) {
        Set<String> visitados = new java.util.HashSet<>();
        String actual = descendiente;
        while (actual != null && visitados.add(actual)) {
            if (actual.equals(ancestro)) {
                return true;
            }
            actual = plan.padresZ().get(actual);
        }
        return false;
    }

    private String resolverImplementacionVirtualZ(
            String claseActual,
            String claseBase,
            String firma
    ) {
        Set<String> visitados = new java.util.HashSet<>();
        String actual = claseActual;
        while (actual != null && visitados.add(actual)) {
            String candidato = actual + firma;
            if (plan.marco(candidato).isPresent()) {
                return candidato;
            }
            if (actual.equals(claseBase)) {
                break;
            }
            actual = plan.padresZ().get(actual);
        }
        return null;
    }

    private String nuevaEtiquetaVirtual() {
        return "call_virtual_L" + siguienteEtiquetaVirtual++;
    }

    private void bajarNuevoObjeto(
            Cuarteta cuarteta
    ) {
        int cantidad =
                entero(
                        cuarteta.argumento2(),
                        0
                );

        List<ParametroPendiente> parametros =
                consumirUltimos(
                        cantidad
                );

        String constructor =
                cuarteta.argumento1();

        String clase =
                extraerClaseConstructor(
                        constructor
                );

        if (clase == null
                && !esVacio(constructor)
                && plan.layout(
                "Z::" + constructor
        ).isPresent()) {

            clase =
                    constructor;
        }

        Optional<MarcoStackProyecto> marcoConstructor =
                plan.marco(
                        constructor
                );

        if (marcoConstructor.isEmpty()
                && clase != null) {

            Optional<String> constructorResuelto =
                    resolverConstructorZ(
                            clase,
                            cantidad
                    );

            if (constructorResuelto.isPresent()) {
                constructor =
                        constructorResuelto.orElseThrow();

                marcoConstructor =
                        plan.marco(
                                constructor
                        );
            }
        }

        Optional<LayoutHeapProyecto> layout =
                clase == null
                        ? Optional.empty()
                        : plan.layout(
                        "Z::" + clase
                );

        if (marcoActual == null
                || layout.isEmpty()) {

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(
                    cuarteta
            );

            return;
        }

        if (marcoConstructor.isEmpty()) {
            if (cantidad == 0) {
                bajarConstructorImplicito(
                        cuarteta,
                        layout.orElseThrow()
                );

                return;
            }

            copiarParametrosSimbolicos(
                    parametros
            );

            copiar(
                    cuarteta
            );

            return;
        }

        parametros =
                materializarParametros(
                        parametros
                );

        LayoutHeapProyecto layoutObjeto =
                layout.orElseThrow();

        String referenciaObjeto =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referenciaObjeto
        );

        registrarTipoObjetoZ(referenciaObjeto, clase);

        String nuevoHeap =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        Math.max(
                                1,
                                layoutObjeto.tamano()
                        )
                ),
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        for (LayoutHeapProyecto.Campo campo
                : layoutObjeto.campos()) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referenciaObjeto
                            + "+"
                            + campo.desplazamiento(),
                    valorInicialCampo(
                            campo.tipo()
                    ),
                    null
            );
        }

        int tamanoLlamador =
                marcoActual.tamano();

        agregar(
                OperadorCuarteta.ESCRIBIR_STACK,
                "P+"
                        + (
                        tamanoLlamador + 1
                ),
                referenciaObjeto,
                null
        );

        for (int indiceLista = 0;
             indiceLista < parametros.size();
             indiceLista++) {

            ParametroPendiente parametro =
                    parametros.get(
                            indiceLista
                    );

            int indiceParametro =
                    entero(
                            parametro.indice(),
                            indiceLista
                    );

            int desplazamiento =
                    tamanoLlamador
                            + 2
                            + indiceParametro;

            agregar(
                    OperadorCuarteta.ESCRIBIR_STACK,
                    "P+" + desplazamiento,
                    parametro.valor(),
                    null
            );
        }

        moverPAdelante(
                tamanoLlamador
        );

        agregar(
                OperadorCuarteta.LLAMAR,
                constructor,
                String.valueOf(
                        cantidad + 1
                ),
                null
        );

        if (!esVacio(
                cuarteta.resultado()
        )) {

            agregar(
                    OperadorCuarteta.LEER_STACK,
                    "P+0",
                    null,
                    cuarteta.resultado()
            );
        }

        moverPAtras(
                tamanoLlamador
        );
    }

    private void bajarConstructorImplicito(
            Cuarteta cuarteta,
            LayoutHeapProyecto layout
    ) {
        String referenciaObjeto =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referenciaObjeto
        );

        registrarTipoObjetoZ(referenciaObjeto, layout.nombre().substring(3));

        int tamano =
                Math.max(
                        1,
                        layout.tamano()
                );

        String nuevoHeap =
                nuevoTemporalObjeto();

        agregar(
                OperadorCuarteta.SUMAR,
                "H",
                String.valueOf(
                        tamano
                ),
                nuevoHeap
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                nuevoHeap,
                null,
                "H"
        );

        for (LayoutHeapProyecto.Campo campo
                : layout.campos()) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referenciaObjeto
                            + "+"
                            + campo.desplazamiento(),
                    valorInicialCampo(
                            campo.tipo()
                    ),
                    null
            );
        }

        if (!esVacio(
                cuarteta.resultado()
        )) {

            agregar(
                    OperadorCuarteta.ASIGNAR,
                    referenciaObjeto,
                    null,
                    cuarteta.resultado()
            );
        }
    }

    private String valorInicialCampo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {
            return "0";
        }

        return switch (tipo) {
            case "int",
                 "double",
                 "char",
                 "boolean" ->
                    "0";

            default ->
                    "-1";
        };
    }

    private Optional<String> resolverConstructorZ(
            String clase,
            int cantidadExplicita
    ) {
        if (clase == null
                || clase.isBlank()) {

            return Optional.empty();
        }

        int cantidadEsperada =
                cantidadExplicita + 1;

        List<FuncionDisponible> candidatos =
                funcionesDisponibles.stream()
                        .filter(
                                funcion ->
                                        funcion.nombre()
                                                .startsWith(
                                                        clase
                                                                + ".<init>("
                                                )
                        )
                        .filter(
                                funcion ->
                                        funcion.cantidadParametros()
                                                == cantidadEsperada
                        )
                        .toList();

        if (candidatos.size() == 1) {
            return Optional.of(
                    candidatos.get(0)
                            .nombre()
            );
        }

        return Optional.empty();
    }

    private String extraerClaseConstructor(
            String constructor
    ) {
        if (constructor == null
                || constructor.isBlank()) {
            return null;
        }

        int indice =
                constructor.indexOf(
                        ".<init>("
                );

        if (indice <= 0) {
            return null;
        }

        return constructor.substring(
                0,
                indice
        );
    }

    private void moverPAdelante(
            int cantidad
    ) {
        if (cantidad <= 0) {
            return;
        }

        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                "P",
                String.valueOf(
                        cantidad
                ),
                temporal
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporal,
                null,
                "P"
        );
    }

    private void moverPAtras(
            int cantidad
    ) {
        if (cantidad <= 0) {
            return;
        }

        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.RESTAR,
                "P",
                String.valueOf(
                        cantidad
                ),
                temporal
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                temporal,
                null,
                "P"
        );
    }

    private List<ParametroPendiente> consumirUltimos(
            int cantidad
    ) {
        if (cantidad <= 0) {
            return List.of();
        }

        int disponibles =
                parametrosPendientes.size();

        int inicio =
                Math.max(
                        0,
                        disponibles - cantidad
                );

        List<ParametroPendiente> resultado =
                new ArrayList<>(
                        parametrosPendientes.subList(
                                inicio,
                                disponibles
                        )
                );

        parametrosPendientes.subList(
                inicio,
                disponibles
        ).clear();

        return resultado;
    }

    private void copiarParametrosSimbolicos(
            List<ParametroPendiente> parametros
    ) {
        for (ParametroPendiente parametro
                : parametros) {

            agregar(
                    OperadorCuarteta.PARAMETRO,
                    parametro.valor(),
                    parametro.indice(),
                    null
            );
        }
    }

    private void vaciarParametrosRestantes() {
        copiarParametrosSimbolicos(
                new ArrayList<>(
                        parametrosPendientes
                )
        );

        parametrosPendientes.clear();
    }

    private int entero(
            String valor,
            int predeterminado
    ) {
        if (valor == null
                || valor.isBlank()
                || valor.equals("-")) {
            return predeterminado;
        }

        try {
            return Integer.parseInt(
                    valor
            );

        } catch (NumberFormatException excepcion) {
            return predeterminado;
        }
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
    }

    private String nuevoTemporal() {
        return "call_t"
                + siguienteTemporal++;
    }

    private String nuevoTemporalObjeto() {
        return "obj_t"
                + siguienteTemporalObjeto++;
    }

    private void copiar(
            Cuarteta cuarteta
    ) {
        agregar(
                cuarteta.operador(),
                cuarteta.argumento1(),
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void agregar(
            OperadorCuarteta operador,
            String argumento1,
            String argumento2,
            String resultado
    ) {
        destino.agregar(
                new Cuarteta(
                        operador,
                        argumento1,
                        argumento2,
                        resultado
                )
        );
    }

    private enum TipoOrigenReceptorZ {
        STACK,
        CAMPO,
        THIS,
        TEMPORAL
    }

    private record OrigenReceptorZ(
            String nombre,
            String tipo,
            TipoOrigenReceptorZ origen,
            SlotStackProyecto slot,
            LayoutHeapProyecto.Campo campo
    ) {
    }

    private record ReceptorMetodoZ(
            String funcion,
            OrigenReceptorZ receptor
    ) {
    }

    private record ParametroPendiente(
            String valor,
            String indice
    ) {
    }

    private record FuncionDisponible(
            String nombre,
            int cantidadParametros
    ) {
    }
}