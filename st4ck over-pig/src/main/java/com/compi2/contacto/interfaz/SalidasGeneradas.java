package com.compi2.contacto.interfaz;

import com.compi2.contacto.c3d.GeneradorCodigoC;
import com.compi2.contacto.c3d.GeneradorCodigoTresDirecciones;
import com.compi2.contacto.c3d.GeneradorRiscV;
import com.compi2.contacto.ir.ProgramaIntermedio;
import java.util.Objects;
import java.util.function.Function;

final class SalidasGeneradas {

    record Salida(String codigo, String error) {
        Salida {
            Objects.requireNonNull(codigo);
            Objects.requireNonNull(error);
        }

        boolean generada() {
            return error.isEmpty() && !codigo.isBlank();
        }

        String mostrar(String nombre) {
            if (generada()) {
                return codigo;
            }
            return "No se pudo generar " + nombre + ":\n" + error;
        }
    }

    private final Salida c3d;
    private final Salida c;
    private final Salida riscV;

    private SalidasGeneradas(Salida c3d, Salida c, Salida riscV) {
        this.c3d = c3d;
        this.c = c;
        this.riscV = riscV;
    }

    static SalidasGeneradas generar(ProgramaIntermedio programa) {
        return generar(programa,
                new GeneradorCodigoTresDirecciones()::generar,
                new GeneradorCodigoC()::generar,
                new GeneradorRiscV()::generar);
    }

    static SalidasGeneradas generar(
            ProgramaIntermedio programa,
            Function<ProgramaIntermedio, String> generadorC3D,
            Function<ProgramaIntermedio, String> generadorC,
            Function<ProgramaIntermedio, String> generadorRiscV) {
        Objects.requireNonNull(programa);
        Objects.requireNonNull(generadorC3D);
        Objects.requireNonNull(generadorC);
        Objects.requireNonNull(generadorRiscV);
        Salida c3d = intentar(programa, generadorC3D);
        Salida c = intentar(programa, generadorC);
        Salida riscV = intentar(programa, generadorRiscV);
        return new SalidasGeneradas(c3d, c, riscV);
    }

    private static Salida intentar(
            ProgramaIntermedio programa,
            Function<ProgramaIntermedio, String> generador) {
        try {
            String codigo = generador.apply(programa);
            if (codigo == null || codigo.isBlank()) {
                return new Salida("", "El generador devolvio codigo vacio");
            }
            return new Salida(codigo, "");
        } catch (RuntimeException excepcion) {
            String detalle = excepcion.getMessage();
            if (detalle == null || detalle.isBlank()) {
                detalle = excepcion.getClass().getSimpleName();
            }
            return new Salida("", detalle);
        }
    }

    Salida c3d() {
        return c3d;
    }

    Salida c() {
        return c;
    }

    Salida riscV() {
        return riscV;
    }

    String estado() {
        boolean completa = c3d.generada() && c.generada() && riscV.generada();
        return "Analisis valido | Generacion " + (completa ? "completa" : "INCOMPLETA")
                + " | RISC-V: " + (riscV.generada() ? "generado" : "NO GENERADO")
                + " | C: " + (c.generada() ? "generado" : "NO GENERADO")
                + " | C3D: " + (c3d.generada() ? "generado" : "NO GENERADO");
    }
}
