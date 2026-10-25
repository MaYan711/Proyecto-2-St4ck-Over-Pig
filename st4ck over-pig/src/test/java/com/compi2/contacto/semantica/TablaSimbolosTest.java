package com.compi2.contacto.semantica;

import com.compi2.contacto.ast.PosicionFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablaSimbolosTest {

    @Test
    void buscaSimbolosEnAmbitosPadre() {
        TablaSimbolos tabla = new TablaSimbolos();
        Simbolo global = simbolo("global");

        assertTrue(tabla.declarar(global));
        tabla.entrar("funcion");
        assertTrue(tabla.buscar("global").isPresent());
        tabla.salir();
    }

    @Test
    void rechazaDuplicadosEnElMismoAmbito() {
        TablaSimbolos tabla = new TablaSimbolos();

        assertTrue(tabla.declarar(simbolo("valor")));
        assertFalse(tabla.declarar(simbolo("valor")));
    }

    private Simbolo simbolo(String nombre) {
        return new Simbolo(
                nombre,
                CategoriaSimbolo.VARIABLE,
                TipoDato.ENTERO,
                "entero",
                new PosicionFuente(Path.of("Funciones.y"), 1, 0)
        );
    }
}

