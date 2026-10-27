
package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.Visibilidad;
import com.compi2.contacto.ast.zetariano.ZAst;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class JerarquiaClasesZ {
    private final Map<String, ZAst.Clase> clases = new LinkedHashMap<>();

    public JerarquiaClasesZ(List<ProgramaAst> programas) {
        Objects.requireNonNull(programas, "Los programas son obligatorios");
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

    public boolean mismoPaquete(String primera, String segunda) {
        ZAst.Clase clasePrimera = clases.get(primera);
        ZAst.Clase claseSegunda = clases.get(segunda);
        if (clasePrimera == null || claseSegunda == null) {
            return false;
        }

        Path archivoPrimero = clasePrimera.posicion().archivo();
        Path archivoSegundo = claseSegunda.posicion().archivo();
        if (archivoPrimero == null || archivoSegundo == null) {
            return false;
        }

        Path carpetaPrimera = archivoPrimero.toAbsolutePath().normalize().getParent();
        Path carpetaSegunda = archivoSegundo.toAbsolutePath().normalize().getParent();
        return carpetaPrimera != null && carpetaPrimera.equals(carpetaSegunda);
    }

    public boolean accesible(Visibilidad visibilidad, String propietario, String origen) {
        if (visibilidad == null) {
            return false;
        }
        if (propietario != null && propietario.equals(origen)) {
            return true;
        }
        if (visibilidad == Visibilidad.PUBLICA) {
            return true;
        }
        if (propietario == null || origen == null) {
            return false;
        }
        if (visibilidad == Visibilidad.PRIVADA) {
            return false;
        }
        if (mismoPaquete(propietario, origen)) {
            return true;
        }
        return visibilidad == Visibilidad.PROTEGIDA
                && distancia(origen, propietario) >= 0;
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
                    if (!actual.nombre().equals(clase)
                            && !accesible(metodo.visibilidad(), actual.nombre(), clase)) {
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
