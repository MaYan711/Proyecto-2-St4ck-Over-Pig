package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.Visibilidad;
import com.compi2.contacto.ast.zetariano.ZAst;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JerarquiaClasesZ {
    private final Map<String, ZAst.Clase> clases = new LinkedHashMap<>();

    public JerarquiaClasesZ(List<ProgramaAst> programas) {
        for (ProgramaAst programa : programas) {
            for (NodoAst nodo : programa.elementos()) {
                if (nodo instanceof ZAst.Clase clase) {
                    clases.putIfAbsent(clase.nombre(), clase);
                }
            }
        }
    }

    public List<ZAst.Clase> recorrido(String nombre) {
        List<ZAst.Clase> resultado = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        ZAst.Clase actual = clases.get(nombre);
        while (actual != null && vistas.add(actual.nombre())) {
            resultado.add(actual);
            actual = actual.padre().map(clases::get).orElse(null);
        }
        return resultado;
    }

    public int distancia(String hija, String padre) {
        List<ZAst.Clase> recorrido = recorrido(hija);
        for (int i = 0; i < recorrido.size(); i++) {
            if (recorrido.get(i).nombre().equals(padre)) {
                return i;
            }
        }
        return -1;
    }

    public String propietario(ZAst.Miembro miembro) {
        for (ZAst.Clase clase : clases.values()) {
            for (ZAst.Miembro propio : clase.miembros()) {
                if (propio == miembro) {
                    return clase.nombre();
                }
            }
        }
        return null;
    }

    public boolean accesible(Visibilidad visibilidad, String propietario, String origen) {
        if (propietario != null && propietario.equals(origen)) {
            return true;
        }
        return visibilidad == Visibilidad.PUBLICA
                || (visibilidad == Visibilidad.PROTEGIDA
                && origen != null && distancia(origen, propietario) >= 0);
    }

    public Map<String, ZAst.Atributo> atributos(String clase) {
        Map<String, ZAst.Atributo> resultado = new LinkedHashMap<>();
        for (ZAst.Clase actual : recorrido(clase)) {
            for (ZAst.Miembro miembro : actual.miembros()) {
                if (miembro instanceof ZAst.Atributo atributo) {
                    resultado.putIfAbsent(atributo.nombre(), atributo);
                }
            }
        }
        return resultado;
    }

    public List<ZAst.Metodo> metodos(String clase, String nombre) {
        Map<String, ZAst.Metodo> resultado = new LinkedHashMap<>();
        for (ZAst.Clase actual : recorrido(clase)) {
            for (ZAst.Miembro miembro : actual.miembros()) {
                if (miembro instanceof ZAst.Metodo metodo && metodo.nombre().equals(nombre)) {
                    boolean heredable = metodo.visibilidad() == Visibilidad.PUBLICA
                            || metodo.visibilidad() == Visibilidad.PROTEGIDA;
                    if (!actual.nombre().equals(clase) && !heredable) {
                        continue;
                    }
                    String firma = metodo.nombre() + "(" + String.join(",",
                            metodo.parametros().stream()
                                    .map(p -> p.tipo().nombreCompleto()).toList()) + ")";
                    resultado.putIfAbsent(firma, metodo);
                }
            }
        }
        return List.copyOf(resultado.values());
    }
}
