package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class BajadorArreglosProyecto {

    private static final Pattern NUMERO =
            Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private static final Pattern TEMPORAL =
            Pattern.compile(
                    "(?:(?:y|z|pig|mem|call|obj|str|arr)_)?t\\d+"
            );

    private PlanMemoriaProyecto plan;
    private ProgramaIntermedio destino;
    private MarcoStackProyecto marcoActual;
    private String claseZActual;
    private int siguienteTemporal;
    private int siguienteEtiqueta;

    public ProgramaIntermedio bajar(
            ProgramaIntermedio origen,
            PlanMemoriaProyecto plan
    ) {
        Objects.requireNonNull(
                origen,
                "El programa intermedio es obligatorio"
        );

        this.plan =
                Objects.requireNonNull(
                        plan,
                        "El plan de memoria es obligatorio"
                );

        destino =
                new ProgramaIntermedio();

        marcoActual = null;
        claseZActual = null;
        siguienteTemporal = 0;
        siguienteEtiqueta = 0;

        for (Cuarteta cuarteta
                : origen.cuartetas()) {

            procesar(
                    cuarteta
            );
        }

        return destino;
    }

    private void procesar(
            Cuarteta cuarteta
    ) {
        switch (cuarteta.operador()) {

            case INICIO_FUNCION ->
                    iniciarFuncion(
                            cuarteta
                    );

            case FIN_FUNCION ->
                    finalizarFuncion(
                            cuarteta
                    );

            case NUEVO_ARREGLO ->
                    bajarNuevoArreglo(
                            cuarteta
                    );

            case ASIGNAR ->
                    bajarAsignacion(
                            cuarteta
                    );

            case PARAMETRO,
                 IMPRIMIR,
                 RETORNAR,
                 NEGATIVO,
                 NEGAR,
                 SALTAR_SI_FALSO,
                 SALTAR_SI_VERDADERO ->
                    bajarPrimerArgumento(
                            cuarteta
                    );

            case SUMAR,
                 RESTAR,
                 MULTIPLICAR,
                 DIVIDIR,
                 MODULO,
                 IGUALDAD,
                 DIFERENTE,
                 MENOR,
                 MAYOR,
                 MENOR_IGUAL,
                 MAYOR_IGUAL,
                 AND,
                 OR,
                 COMPARAR ->
                    bajarDosArgumentos(
                            cuarteta
                    );

            case ESCRIBIR_STACK,
                 ESCRIBIR_HEAP ->
                    bajarSegundoArgumento(
                            cuarteta
                    );

            default ->
                    copiar(
                            cuarteta
                    );
        }
    }

    private void iniciarFuncion(
            Cuarteta cuarteta
    ) {
        marcoActual =
                plan.marco(
                        cuarteta.argumento1()
                ).orElse(
                        null
                );

        claseZActual =
                extraerClaseZ(
                        cuarteta.argumento1()
                );

        copiar(
                cuarteta
        );
    }

    private void finalizarFuncion(
            Cuarteta cuarteta
    ) {
        copiar(
                cuarteta
        );

        marcoActual = null;
        claseZActual = null;
    }

    private void bajarNuevoArreglo(
            Cuarteta cuarteta
    ) {
        List<String> dimensiones =
                separarDimensiones(
                        cuarteta.argumento2()
                );

        if (dimensiones.isEmpty()
                || esVacio(
                cuarteta.resultado()
        )) {

            copiar(
                    cuarteta
            );

            return;
        }

        List<String> valoresDimensiones =
                new ArrayList<>();

        for (String dimension
                : dimensiones) {

            valoresDimensiones.add(
                    resolverEscalar(
                            dimension
                    )
            );
        }

        String referencia =
                cuarteta.resultado();

        agregar(
                OperadorCuarteta.ASIGNAR,
                "H",
                null,
                referencia
        );

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                referencia + "+0",
                String.valueOf(
                        valoresDimensiones.size()
                ),
                null
        );

        for (int indice = 0;
             indice < valoresDimensiones.size();
             indice++) {

            agregar(
                    OperadorCuarteta.ESCRIBIR_HEAP,
                    referencia
                            + "+"
                            + (
                            indice + 1
                    ),
                    valoresDimensiones.get(
                            indice
                    ),
                    null
            );
        }

        String cantidadElementos =
                calcularCantidadElementos(
                        valoresDimensiones
                );

        int encabezado =
                1
                        + valoresDimensiones.size();

        String inicioDatos =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                referencia,
                String.valueOf(
                        encabezado
                ),
                inicioDatos
        );

        String finDatos =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                inicioDatos,
                cantidadElementos,
                finDatos
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                finDatos,
                null,
                "H"
        );

        inicializarDatos(
                inicioDatos,
                finDatos,
                valorInicialElemento(
                        cuarteta.argumento1()
                )
        );
    }

    private String calcularCantidadElementos(
            List<String> dimensiones
    ) {
        if (dimensiones.size() == 1) {
            return dimensiones.get(0);
        }

        String acumulado =
                dimensiones.get(0);

        for (int indice = 1;
             indice < dimensiones.size();
             indice++) {

            String temporal =
                    nuevoTemporal();

            agregar(
                    OperadorCuarteta.MULTIPLICAR,
                    acumulado,
                    dimensiones.get(
                            indice
                    ),
                    temporal
            );

            acumulado =
                    temporal;
        }

        return acumulado;
    }

    private void inicializarDatos(
            String inicio,
            String fin,
            String valorInicial
    ) {
        String cursor =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.ASIGNAR,
                inicio,
                null,
                cursor
        );

        String etiquetaInicio =
                nuevaEtiqueta();

        String etiquetaFin =
                nuevaEtiqueta();

        agregar(
                OperadorCuarteta.ETIQUETA,
                null,
                null,
                etiquetaInicio
        );

        String condicion =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.MENOR,
                cursor,
                fin,
                condicion
        );

        agregar(
                OperadorCuarteta.SALTAR_SI_FALSO,
                condicion,
                null,
                etiquetaFin
        );

        agregar(
                OperadorCuarteta.ESCRIBIR_HEAP,
                cursor,
                valorInicial,
                null
        );

        String siguiente =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                cursor,
                "1",
                siguiente
        );

        agregar(
                OperadorCuarteta.ASIGNAR,
                siguiente,
                null,
                cursor
        );

        agregar(
                OperadorCuarteta.SALTAR,
                null,
                null,
                etiquetaInicio
        );

        agregar(
                OperadorCuarteta.ETIQUETA,
                null,
                null,
                etiquetaFin
        );
    }

    private void bajarAsignacion(
            Cuarteta cuarteta
    ) {
        Optional<AccesoArreglo> destinoArreglo =
                analizarAcceso(
                        cuarteta.resultado()
                );

        String valor =
                materializar(
                        cuarteta.argumento1()
                );

        if (destinoArreglo.isPresent()) {

            Optional<DireccionArreglo> direccion =
                    calcularDireccion(
                            destinoArreglo.orElseThrow()
                    );

            if (direccion.isPresent()) {

                agregar(
                        OperadorCuarteta.ESCRIBIR_HEAP,
                        direccion.orElseThrow()
                                .direccion(),
                        valor,
                        null
                );

                return;
            }
        }

        agregar(
                OperadorCuarteta.ASIGNAR,
                valor,
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void bajarPrimerArgumento(
            Cuarteta cuarteta
    ) {
        agregar(
                cuarteta.operador(),
                materializar(
                        cuarteta.argumento1()
                ),
                cuarteta.argumento2(),
                cuarteta.resultado()
        );
    }

    private void bajarSegundoArgumento(
            Cuarteta cuarteta
    ) {
        agregar(
                cuarteta.operador(),
                cuarteta.argumento1(),
                materializar(
                        cuarteta.argumento2()
                ),
                cuarteta.resultado()
        );
    }

    private void bajarDosArgumentos(
            Cuarteta cuarteta
    ) {
        agregar(
                cuarteta.operador(),
                materializar(
                        cuarteta.argumento1()
                ),
                materializar(
                        cuarteta.argumento2()
                ),
                cuarteta.resultado()
        );
    }

    private String materializar(
            String valor
    ) {
        Optional<AccesoArreglo> acceso =
                analizarAcceso(
                        valor
                );

        if (acceso.isEmpty()) {
            return valor;
        }

        Optional<DireccionArreglo> direccion =
                calcularDireccion(
                        acceso.orElseThrow()
                );

        if (direccion.isEmpty()) {
            return valor;
        }

        String temporal =
                nuevoTemporal();

        DireccionArreglo accesoResuelto =
                direccion.orElseThrow();

        agregar(
                OperadorCuarteta.LEER_HEAP,
                accesoResuelto.direccion(),
                accesoResuelto.tipoElemento(),
                temporal
        );

        return temporal;
    }

    private Optional<DireccionArreglo> calcularDireccion(
            AccesoArreglo acceso
    ) {
        Optional<BaseArreglo> base =
                resolverBase(
                        acceso.base()
                );

        if (base.isEmpty()) {
            return Optional.empty();
        }

        BaseArreglo arreglo =
                base.orElseThrow();

        if (acceso.indices()
                .size()
                != arreglo.dimensiones()) {

            return Optional.empty();
        }

        List<String> indices =
                new ArrayList<>();

        for (String indice
                : acceso.indices()) {

            indices.add(
                    resolverEscalar(
                            indice
                    )
            );
        }

        String inicioDatos =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                arreglo.referencia(),
                String.valueOf(
                        1
                                + arreglo.dimensiones()
                ),
                inicioDatos
        );

        String desplazamiento =
                null;

        for (int indiceActual = 0;
             indiceActual < indices.size();
             indiceActual++) {

            String termino =
                    indices.get(
                            indiceActual
                    );

            String stride =
                    calcularStride(
                            arreglo,
                            indiceActual
                    );

            if (!stride.equals("1")) {

                String multiplicacion =
                        nuevoTemporal();

                agregar(
                        OperadorCuarteta.MULTIPLICAR,
                        termino,
                        stride,
                        multiplicacion
                );

                termino =
                        multiplicacion;
            }

            if (desplazamiento == null) {

                desplazamiento =
                        termino;

            } else {

                String suma =
                        nuevoTemporal();

                agregar(
                        OperadorCuarteta.SUMAR,
                        desplazamiento,
                        termino,
                        suma
                );

                desplazamiento =
                        suma;
            }
        }

        if (desplazamiento == null) {
            desplazamiento = "0";
        }

        String direccion =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.SUMAR,
                inicioDatos,
                desplazamiento,
                direccion
        );

        return Optional.of(
                new DireccionArreglo(
                        direccion,
                        arreglo.tipoElemento()
                )
        );
    }

    private String calcularStride(
            BaseArreglo arreglo,
            int indiceActual
    ) {
        if (indiceActual
                >= arreglo.dimensiones() - 1) {

            return "1";
        }

        String stride =
                null;

        for (int dimension =
             indiceActual + 1;
             dimension < arreglo.dimensiones();
             dimension++) {

            String direccionDimension =
                    nuevoTemporal();

            agregar(
                    OperadorCuarteta.SUMAR,
                    arreglo.referencia(),
                    String.valueOf(
                            dimension + 1
                    ),
                    direccionDimension
            );

            String valorDimension =
                    nuevoTemporal();

            agregar(
                    OperadorCuarteta.LEER_HEAP,
                    direccionDimension,
                    null,
                    valorDimension
            );

            if (stride == null) {

                stride =
                        valorDimension;

            } else {

                String producto =
                        nuevoTemporal();

                agregar(
                        OperadorCuarteta.MULTIPLICAR,
                        stride,
                        valorDimension,
                        producto
                );

                stride =
                        producto;
            }
        }

        return stride == null
                ? "1"
                : stride;
    }

    private Optional<BaseArreglo> resolverBase(
            String nombre
    ) {
        if (marcoActual == null
                || esVacio(
                nombre
        )) {

            return Optional.empty();
        }

        if (!nombre.contains(".")) {

            Optional<SlotStackProyecto> slot =
                    marcoActual.buscar(
                            nombre
                    );

            if (slot.isPresent()
                    && dimensionesTipo(
                    slot.orElseThrow()
                            .tipo()
            ) > 0) {

                return Optional.of(
                        new BaseArreglo(
                                leerStack(
                                        slot.orElseThrow()
                                ),
                                dimensionesTipo(
                                        slot.orElseThrow()
                                                .tipo()
                                ),
                                tipoElementoArreglo(
                                        slot.orElseThrow()
                                                .tipo()
                                )
                        )
                );
            }

            Optional<BaseArreglo> campoActual =
                    resolverCampoActual(
                            nombre
                    );

            if (campoActual.isPresent()) {
                return campoActual;
            }

            return Optional.empty();
        }

        int punto =
                nombre.indexOf('.');

        if (punto != nombre.lastIndexOf('.')) {
            return Optional.empty();
        }

        String objeto =
                nombre.substring(
                        0,
                        punto
                );

        String campo =
                nombre.substring(
                        punto + 1
                );

        Optional<SlotStackProyecto> slotObjeto =
                marcoActual.buscar(
                        objeto
                );

        if (slotObjeto.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                buscarLayoutTipo(
                        slotObjeto.orElseThrow()
                                .tipo()
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto.Campo> campoHeap =
                layout.orElseThrow()
                        .buscarCampo(
                                campo
                        );

        if (campoHeap.isEmpty()) {
            return Optional.empty();
        }

        int dimensiones =
                dimensionesTipo(
                        campoHeap.orElseThrow()
                                .tipo()
                );

        if (dimensiones <= 0) {
            return Optional.empty();
        }

        String objetoRef =
                leerStack(
                        slotObjeto.orElseThrow()
                );

        String arregloRef =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_HEAP,
                objetoRef
                        + "+"
                        + campoHeap.orElseThrow()
                        .desplazamiento(),
                campoHeap.orElseThrow()
                        .tipo(),
                arregloRef
        );

        return Optional.of(
                new BaseArreglo(
                        arregloRef,
                        dimensiones,
                        tipoElementoArreglo(
                                campoHeap.orElseThrow()
                                        .tipo()
                        )
                )
        );
    }

    private Optional<BaseArreglo> resolverCampoActual(
            String nombre
    ) {
        if (claseZActual == null
                || marcoActual == null) {

            return Optional.empty();
        }

        Optional<LayoutHeapProyecto> layout =
                plan.layout(
                        "Z::"
                                + claseZActual
                );

        if (layout.isEmpty()) {
            return Optional.empty();
        }

        Optional<LayoutHeapProyecto.Campo> campo =
                layout.orElseThrow()
                        .buscarCampo(
                                nombre
                        );

        if (campo.isEmpty()) {
            return Optional.empty();
        }

        int dimensiones =
                dimensionesTipo(
                        campo.orElseThrow()
                                .tipo()
                );

        if (dimensiones <= 0) {
            return Optional.empty();
        }

        Optional<SlotStackProyecto> thisSlot =
                marcoActual.thisSlot();

        if (thisSlot.isEmpty()) {
            return Optional.empty();
        }

        String thisRef =
                leerStack(
                        thisSlot.orElseThrow()
                );

        String arregloRef =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_HEAP,
                thisRef
                        + "+"
                        + campo.orElseThrow()
                        .desplazamiento(),
                campo.orElseThrow()
                        .tipo(),
                arregloRef
        );

        return Optional.of(
                new BaseArreglo(
                        arregloRef,
                        dimensiones,
                        tipoElementoArreglo(
                                campo.orElseThrow()
                                        .tipo()
                        )
                )
        );
    }

    private Optional<LayoutHeapProyecto> buscarLayoutTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {

            return Optional.empty();
        }

        String base =
                tipo.replace(
                        "[]",
                        ""
                );

        Optional<LayoutHeapProyecto> z =
                plan.layout(
                        "Z::"
                                + base
                );

        if (z.isPresent()) {
            return z;
        }

        return plan.layout(
                "Y::"
                        + base
        );
    }

    private String resolverEscalar(
            String valor
    ) {
        if (esVacio(
                valor
        )
                || NUMERO.matcher(
                valor
        ).matches()
                || valor.equals("P")
                || valor.equals("H")) {

            return valor;
        }

        if (marcoActual != null) {

            Optional<SlotStackProyecto> slot =
                    marcoActual.buscar(
                            valor
                    );

            if (slot.isPresent()) {

                return leerStack(
                        slot.orElseThrow()
                );
            }
        }

        if (TEMPORAL.matcher(
                valor
        ).matches()) {
            return valor;
        }

        return valor;
    }

    private String leerStack(
            SlotStackProyecto slot
    ) {
        String temporal =
                nuevoTemporal();

        agregar(
                OperadorCuarteta.LEER_STACK,
                slot.direccion(),
                slot.tipo(),
                temporal
        );

        return temporal;
    }

    private String tipoElementoArreglo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {

            return null;
        }

        String resultado =
                tipo.trim();

        while (resultado.endsWith("[]")) {
            resultado =
                    resultado.substring(
                            0,
                            resultado.length() - 2
                    );
        }

        return resultado;
    }

    private int dimensionesTipo(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {

            return 0;
        }

        int cantidad = 0;
        int indice = 0;

        while ((indice =
                tipo.indexOf(
                        "[]",
                        indice
                ))
                >= 0) {

            cantidad++;
            indice += 2;
        }

        return cantidad;
    }

    private List<String> separarDimensiones(
            String texto
    ) {
        if (esVacio(
                texto
        )) {
            return List.of();
        }

        String normalizado =
                texto.replace(
                        "x",
                        ","
                );

        List<String> resultado =
                new ArrayList<>();

        for (String dimension
                : normalizado.split(",")) {

            String limpia =
                    dimension.trim();

            if (!limpia.isEmpty()) {
                resultado.add(
                        limpia
                );
            }
        }

        return resultado;
    }

    private Optional<AccesoArreglo> analizarAcceso(
            String texto
    ) {
        if (texto == null
                || texto.isBlank()
                || !texto.contains("[")) {

            return Optional.empty();
        }

        int primerCorchete =
                texto.indexOf('[');

        if (primerCorchete <= 0) {
            return Optional.empty();
        }

        String base =
                texto.substring(
                        0,
                        primerCorchete
                );

        List<String> indices =
                new ArrayList<>();

        int posicion =
                primerCorchete;

        while (posicion
                < texto.length()) {

            if (texto.charAt(
                    posicion
            ) != '[') {

                return Optional.empty();
            }

            int cierre =
                    texto.indexOf(
                            ']',
                            posicion + 1
                    );

            if (cierre < 0) {
                return Optional.empty();
            }

            String indice =
                    texto.substring(
                            posicion + 1,
                            cierre
                    ).trim();

            if (indice.isEmpty()) {
                return Optional.empty();
            }

            indices.add(
                    indice
            );

            posicion =
                    cierre + 1;
        }

        if (indices.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new AccesoArreglo(
                        base,
                        indices
                )
        );
    }

    private String valorInicialElemento(
            String tipo
    ) {
        if (tipo == null
                || tipo.isBlank()) {

            return "0";
        }

        String normalizado =
                tipo.trim();

        return switch (normalizado) {
            case "entero",
                 "flotante",
                 "caracter",
                 "bool",
                 "int",
                 "double",
                 "char",
                 "boolean",
                 "numerus",
                 "decimalis",
                 "littera" ->
                    "0";

            default ->
                    "-1";
        };
    }

    private String extraerClaseZ(
            String funcion
    ) {
        if (funcion == null
                || funcion.equals("MAIOR")
                || !funcion.contains(".")) {

            return null;
        }

        int punto =
                funcion.indexOf('.');

        if (punto <= 0) {
            return null;
        }

        String clase =
                funcion.substring(
                        0,
                        punto
                );

        return plan.layout(
                "Z::"
                        + clase
        ).isPresent()
                ? clase
                : null;
    }

    private String nuevoTemporal() {
        return "arr_t"
                + siguienteTemporal++;
    }

    private String nuevaEtiqueta() {
        return "arr_L"
                + siguienteEtiqueta++;
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank()
                || valor.equals("-");
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

    private record AccesoArreglo(
            String base,
            List<String> indices
    ) {

        private AccesoArreglo {
            indices = List.copyOf(
                    indices
            );
        }
    }

    private record DireccionArreglo(
            String direccion,
            String tipoElemento
    ) {
    }

    private record BaseArreglo(
            String referencia,
            int dimensiones,
            String tipoElemento
    ) {
    }
}
