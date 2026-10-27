package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorRiscVSaltosLargosTest {

    @Test
    void saltaErroresLejanosConTrampolinCercano() {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        programa.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        programa.agregar(c(OperadorCuarteta.IMPRIMIR, "300", "println", "-"));
        programa.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));

        String ensamblador = new GeneradorRiscV().generar(programa);
        assertTrue(ensamblador.contains("ble t4, t5, rv_salto_corto_0"));
        assertTrue(ensamblador.contains("j rv_error_recursion"));
        assertFalse(Pattern.compile("(?m)^\\s*b(?:eq|ne|lt|ge|gt|le|eqz|nez|ltz|gez)\\s+[^\\n]*,\\s*rv_error_")
                .matcher(ensamblador).find());
    }

    @Test
    void saltaEtiquetaLejanaSinDesbordarElRangoCondicional() {
        ProgramaIntermedio programa = new ProgramaIntermedio();
        programa.agregar(c(OperadorCuarteta.INICIO_FUNCION, "MAIOR", "void", "0"));
        programa.agregar(c(OperadorCuarteta.SALTAR_SI_FALSO, "false", "-", "FINAL"));
        for (int i = 0; i < 700; i++) {
            programa.agregar(c(OperadorCuarteta.ASIGNAR, String.valueOf(i), "-", "tmp_" + i));
        }
        programa.agregar(c(OperadorCuarteta.ETIQUETA, "-", "-", "FINAL"));
        programa.agregar(c(OperadorCuarteta.IMPRIMIR, "300", "println", "-"));
        programa.agregar(c(OperadorCuarteta.FIN_FUNCION, "MAIOR", "-", "-"));

        String ensamblador = new GeneradorRiscV().generar(programa);
        assertTrue(ensamblador.contains("bnez t0, rv_salto_corto_"));
        assertTrue(ensamblador.contains("j rv_lbl_0_0"));
        Matcher definiciones = Pattern.compile("(?m)^(rv_salto_corto_\\d+):$").matcher(ensamblador);
        Set<String> nombres = new HashSet<>();
        int total = 0;
        while (definiciones.find()) {
            assertTrue(nombres.add(definiciones.group(1)));
            total++;
        }
        assertTrue(total > 1);
        assertEquals(total, nombres.size());
    }

    private Cuarteta c(OperadorCuarteta operacion, String a, String b, String resultado) {
        return new Cuarteta(operacion, a, b, resultado);
    }
}
