package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.Visibilidad;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ValidadorHerenciaZ {

    public List<Diagnostico> validar(List<ProgramaAst> programas) {
        Map<String, ZAst.Clase> clases = obtenerClases(programas);
        JerarquiaClasesZ jerarquia = new JerarquiaClasesZ(programas);
        List<Diagnostico> diagnosticos = new ArrayList<>();

        validarPadres(clases, diagnosticos);
        validarCiclos(clases, diagnosticos);

        if (!diagnosticos.isEmpty()) {
            return List.copyOf(diagnosticos);
        }

        for (ZAst.Clase clase : clases.values()) {
            validarMetodos(clase, clases, jerarquia, diagnosticos);
        }

        return List.copyOf(diagnosticos);
    }

    private Map<String, ZAst.Clase> obtenerClases(
            List<ProgramaAst> programas
    ) {
        Map<String, ZAst.Clase> clases = new LinkedHashMap<>();

        for (ProgramaAst programa : programas) {
            for (var nodo : programa.elementos()) {
                if (nodo instanceof ZAst.Clase clase) {
                    clases.putIfAbsent(clase.nombre(), clase);
                }
            }
        }

        return clases;
    }

    private void validarPadres(
            Map<String, ZAst.Clase> clases,
            List<Diagnostico> diagnosticos
    ) {
        for (ZAst.Clase clase : clases.values()) {
            if (clase.padre().isEmpty()) {
                continue;
            }

            String padre = clase.padre().orElseThrow();

            if (!clases.containsKey(padre)) {
                error(
                        diagnosticos,
                        clase.posicion(),
                        "La clase padre '" + padre + "' no existe"
                );
            }
        }
    }

    private void validarCiclos(
            Map<String, ZAst.Clase> clases,
            List<Diagnostico> diagnosticos
    ) {
        for (ZAst.Clase clase : clases.values()) {
            Set<String> visitadas = new HashSet<>();
            ZAst.Clase actual = clase;

            while (actual != null) {
                if (!visitadas.add(actual.nombre())) {
                    error(
                            diagnosticos,
                            clase.posicion(),
                            "Herencia circular en la jerarquia de '"
                                    + clase.nombre() + "'"
                    );

                    break;
                }

                actual = actual.padre()
                        .map(clases::get)
                        .orElse(null);
            }
        }
    }

    private void validarMetodos(
            ZAst.Clase clase,
            Map<String, ZAst.Clase> clases,
            JerarquiaClasesZ jerarquia,
            List<Diagnostico> diagnosticos
    ) {
        for (ZAst.Miembro miembro : clase.miembros()) {
            if (!(miembro instanceof ZAst.Metodo metodo)) {
                continue;
            }

            ZAst.Metodo heredado = buscarMetodoHeredado(
                    clase,
                    metodo,
                    clases,
                    jerarquia
            );

            if (heredado == null) {
                if (metodo.sobrescrito()) {
                    error(
                            diagnosticos,
                            metodo.posicion(),
                            "@Override no corresponde: no existe un metodo "
                                    + "heredable con la firma "
                                    + firma(metodo)
                    );
                }

                continue;
            }

            if (!metodo.sobrescrito()) {
                error(
                        diagnosticos,
                        metodo.posicion(),
                        "El metodo " + firma(metodo)
                                + " sobrescribe un metodo heredado "
                                + "y requiere @Override"
                );
            }

            if (nivelVisibilidad(metodo.visibilidad())
                    < nivelVisibilidad(heredado.visibilidad())) {
                error(
                        diagnosticos,
                        metodo.posicion(),
                        "El metodo " + firma(metodo)
                                + " no puede reducir la visibilidad heredada"
                );
            }

            if (!retornoCompatible(metodo, heredado, clases)) {
                error(
                        diagnosticos,
                        metodo.posicion(),
                        "El retorno de " + firma(metodo)
                                + " no es compatible con el metodo heredado"
                );
            }
        }
    }

    private ZAst.Metodo buscarMetodoHeredado(
            ZAst.Clase clase,
            ZAst.Metodo metodo,
            Map<String, ZAst.Clase> clases,
            JerarquiaClasesZ jerarquia
    ) {
        ZAst.Clase actual = clase.padre()
                .map(clases::get)
                .orElse(null);

        while (actual != null) {
            for (ZAst.Miembro miembro : actual.miembros()) {
                if (miembro instanceof ZAst.Metodo candidato
                        && firma(candidato).equals(firma(metodo))
                        && jerarquia.accesible(
                        candidato.visibilidad(),
                        actual.nombre(),
                        clase.nombre())) {
                    return candidato;
                }
            }

            actual = actual.padre()
                    .map(clases::get)
                    .orElse(null);
        }

        return null;
    }

    private boolean retornoCompatible(
            ZAst.Metodo metodo,
            ZAst.Metodo heredado,
            Map<String, ZAst.Clase> clases
    ) {
        if (metodo.esVoid() || heredado.esVoid()) {
            return metodo.esVoid() && heredado.esVoid();
        }

        ZAst.Tipo recibido = metodo.retorno().orElseThrow();
        ZAst.Tipo esperado = heredado.retorno().orElseThrow();

        if (recibido.nombreCompleto().equals(esperado.nombreCompleto())) {
            return true;
        }

        if (recibido.esArreglo() || esperado.esArreglo()) {
            return false;
        }

        if (!clases.containsKey(recibido.nombreBase())
                || !clases.containsKey(esperado.nombreBase())) {
            return false;
        }

        return esSubtipo(
                recibido.nombreBase(),
                esperado.nombreBase(),
                clases
        );
    }

    private boolean esSubtipo(
            String recibido,
            String esperado,
            Map<String, ZAst.Clase> clases
    ) {
        Set<String> visitadas = new HashSet<>();
        ZAst.Clase actual = clases.get(recibido);

        while (actual != null && visitadas.add(actual.nombre())) {
            if (actual.nombre().equals(esperado)) {
                return true;
            }

            actual = actual.padre()
                    .map(clases::get)
                    .orElse(null);
        }

        return false;
    }

    private int nivelVisibilidad(Visibilidad visibilidad) {
        return switch (visibilidad) {
            case PUBLICA -> 3;
            case PROTEGIDA -> 2;
            case DEFAULT -> 1;
            case PRIVADA -> 0;
        };
    }

    private String firma(ZAst.Metodo metodo) {
        String parametros = String.join(
                ",",
                metodo.parametros()
                        .stream()
                        .map(parametro -> parametro.tipo().nombreCompleto())
                        .toList()
        );

        return metodo.nombre() + "(" + parametros + ")";
    }

    private void error(
            List<Diagnostico> diagnosticos,
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
}