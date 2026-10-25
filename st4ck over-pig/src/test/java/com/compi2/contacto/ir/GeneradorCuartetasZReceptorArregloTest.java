package com.compi2.contacto.ir;

import com.compi2.contacto.ast.PosicionFuente;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.Visibilidad;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.c3d.GeneradorCodigoC;
import com.compi2.contacto.memoria.BajadorArreglosProyecto;
import com.compi2.contacto.memoria.BajadorCadenasProyecto;
import com.compi2.contacto.memoria.BajadorInicializadoresProyecto;
import com.compi2.contacto.memoria.BajadorLlamadasProyecto;
import com.compi2.contacto.memoria.BajadorMemoriaProyecto;
import com.compi2.contacto.memoria.PlanificadorMemoriaProyecto;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.semantica.EnlacesZ;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorCuartetasZReceptorArregloTest {
    private static final PosicionFuente POS = new PosicionFuente(Path.of("Banco.z"), 1, 0);

    @Test
    void materializaElReceptorIndexadoSoloUnaVez() {
        CasoBanco caso = casoBanco(false);
        ProgramaIntermedio intermedio = new GeneradorCuartetasZ()
                .generar(List.of(caso.ast()), caso.enlaces());

        assertEquals(1, intermedio.cuartetas().stream()
                .filter(c -> c.operador() == OperadorCuarteta.ASIGNAR
                        && c.argumento1() != null
                        && c.argumento1().startsWith("cuentas["))
                .count());
        assertTrue(intermedio.cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LLAMAR
                        && c.argumento1().startsWith("@receptor@")
                        && c.argumento1().endsWith("@Cuenta.getNumero()")));
    }

    @Test
    void cargaElObjetoDelArregloYRespetaSobrescritura() {
        CasoBanco caso = casoBanco(true);
        ProgramaIntermedio origen = new GeneradorCuartetasZ()
                .generar(List.of(caso.ast()), caso.enlaces());

        ProgramaIntermedio protegido = new ProgramaIntermedio();
        for (Cuarteta cuarteta : origen.cuartetas()) {
            protegido.agregar(new Cuarteta(
                    cuarteta.operador(),
                    proteger(cuarteta.argumento1()),
                    proteger(cuarteta.argumento2()),
                    proteger(cuarteta.resultado())));
        }
        protegido.agregar(new Cuarteta(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        protegido.agregar(new Cuarteta(OperadorCuarteta.RETORNAR, null, null, null));
        protegido.agregar(new Cuarteta(OperadorCuarteta.FIN_FUNCION, "MAIOR", null, null));

        var plan = new PlanificadorMemoriaProyecto()
                .planificar(List.of(), List.of(caso.ast()), List.of());
        var memoria = new BajadorMemoriaProyecto().bajar(protegido, plan);
        memoria = new BajadorArreglosProyecto().bajar(memoria, plan);
        memoria = new BajadorInicializadoresProyecto().bajar(memoria, plan);
        memoria = new BajadorLlamadasProyecto().bajar(memoria, plan);
        memoria = new BajadorCadenasProyecto().bajar(memoria);

        assertTrue(memoria.cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LEER_HEAP
                        && "Cuenta".equals(c.argumento2())));
        assertTrue(memoria.cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LLAMAR
                        && "Cuenta.getNumero()".equals(c.argumento1())));
        assertTrue(memoria.cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LLAMAR
                        && "CuentaHija.getNumero()".equals(c.argumento1())));
        assertTrue(memoria.cuartetas().stream()
                .anyMatch(c -> c.operador() == OperadorCuarteta.LEER_TIPO_OBJETO));
        assertFalse(memoria.cuartetas().stream()
                .anyMatch(c -> c.argumento1() != null && c.argumento1().contains("@receptor@")));
        String codigo = new GeneradorCodigoC().generar(memoria);
        assertTrue(codigo.contains("fn_Cuenta_getNumero__"));
        assertTrue(codigo.contains("fn_CuentaHija_getNumero__"));
    }

    private String proteger(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replaceAll("(?<![A-Za-z0-9_])t(\\d+)\\b", "z_t$1");
    }

    private CasoBanco casoBanco(boolean conHija) {
        ZAst.Tipo entero = tipo("int", 0);
        ZAst.Llamada llamada = new ZAst.Llamada(
                new ZAst.AccesoMiembro(
                        new ZAst.AccesoArreglo(
                                new ZAst.Identificador("cuentas", POS),
                                new ZAst.Identificador("i", POS), POS),
                        "getNumero", POS), List.of(), POS);
        ZAst.Metodo getter = metodo("getNumero", entero, 7, false);
        ZAst.Clase cuenta = new ZAst.Clase("Cuenta", List.of(getter), POS);
        ZAst.Metodo probar = new ZAst.Metodo(Optional.of(entero), "probar", List.of(),
                new ZAst.Bloque(List.of(
                        new ZAst.Declaracion(entero, "i", Optional.of(
                                new ZAst.Literal(ZAst.TipoLiteral.ENTERO, "0", POS)), POS),
                        new ZAst.Retorno(Optional.of(llamada), POS)), POS), POS);
        ZAst.Clase banco = new ZAst.Clase("Banco", List.of(
                new ZAst.Atributo(tipo("Cuenta", 1), "cuentas", Optional.empty(), POS),
                probar), POS);
        List<ZAst.Clase> clases = new java.util.ArrayList<>(List.of(cuenta, banco));
        if (conHija) {
            clases.add(new ZAst.Clase("CuentaHija", List.of(
                    metodo("getNumero", entero, 9, true)), POS,
                    Optional.of("Cuenta")));
        }
        ProgramaAst programa = new ProgramaAst(LenguajeFuente.ZETARIANO,
                List.copyOf(clases), POS);
        EnlacesZ enlaces = new EnlacesZ();
        enlaces.registrarLlamada(llamada, "Cuenta.getNumero()");
        return new CasoBanco(programa, enlaces);
    }

    private ZAst.Metodo metodo(String nombre, ZAst.Tipo tipo, int valor,
                              boolean sobrescrito) {
        return new ZAst.Metodo(Optional.of(tipo), nombre, List.of(),
                new ZAst.Bloque(List.of(new ZAst.Retorno(Optional.of(
                        new ZAst.Literal(ZAst.TipoLiteral.ENTERO,
                                String.valueOf(valor), POS)), POS)), POS), POS,
                Visibilidad.PUBLICA, sobrescrito);
    }

    private ZAst.Tipo tipo(String nombre, int dimensiones) {
        return new ZAst.Tipo(nombre, dimensiones, POS);
    }

    private record CasoBanco(ProgramaAst ast, EnlacesZ enlaces) {
    }
}
