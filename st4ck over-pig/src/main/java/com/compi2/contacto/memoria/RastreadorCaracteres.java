package com.compi2.contacto.memoria;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RastreadorCaracteres {

    private final Set<String> caracteres = new HashSet<>();
    private final Map<String, String> retornos = new HashMap<>();
    private boolean retornoCaracterPendiente;

    public void iniciar(ProgramaIntermedio programa) {
        caracteres.clear();
        retornos.clear();
        retornoCaracterPendiente = false;

        for (Cuarteta cuarteta : programa.cuartetas()) {
            if (cuarteta.operador()
                    == com.compi2.contacto.ir.OperadorCuarteta.INICIO_FUNCION) {
                retornos.put(
                        cuarteta.argumento1(),
                        cuarteta.argumento2()
                );
            }
        }
    }

    public boolean esCaracter(String valor) {
        if (valor == null) {
            return false;
        }

        String texto = valor.trim();

        return caracteres.contains(valor)
                || (texto.length() >= 3
                && texto.startsWith("'")
                && texto.endsWith("'"));
    }

    public void registrar(Cuarteta cuarteta) {
        switch (cuarteta.operador()) {
            case INICIO_FUNCION, FIN_FUNCION -> {
                caracteres.clear();
                retornoCaracterPendiente = false;
            }

            case DECLARAR, DECLARAR_PARAMETRO, LEER ->
                    actualizar(
                            cuarteta.resultado(),
                            esTipoCaracter(cuarteta.argumento1())
                    );

            case LEER_HEAP ->
                    actualizar(
                            cuarteta.resultado(),
                            esTipoCaracter(cuarteta.argumento2())
                    );

            case LEER_STACK -> {
                boolean retorno = esRetorno(cuarteta.argumento1());

                actualizar(
                        cuarteta.resultado(),
                        esTipoCaracter(cuarteta.argumento2())
                                || (retorno && retornoCaracterPendiente)
                );

                if (retorno) {
                    retornoCaracterPendiente = false;
                }
            }

            case ASIGNAR ->
                    actualizar(
                            cuarteta.resultado(),
                            esCaracter(cuarteta.argumento1())
                    );

            case LLAMAR -> {
                boolean caracter = esTipoCaracter(
                        retornos.get(cuarteta.argumento1())
                );

                actualizar(cuarteta.resultado(), caracter);
                retornoCaracterPendiente = caracter;
            }

            case SUMAR, RESTAR, MULTIPLICAR, DIVIDIR, MODULO,
                 NEGATIVO, NEGAR, IGUALDAD, DIFERENTE,
                 MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL,
                 AND, OR, COMPARAR ->
                    actualizar(cuarteta.resultado(), false);

            default -> {
            }
        }
    }

    private void actualizar(String destino, boolean caracter) {
        if (destino == null || destino.isBlank()) {
            return;
        }

        if (caracter) {
            caracteres.add(destino);
        } else {
            caracteres.remove(destino);
        }
    }

    private boolean esTipoCaracter(String tipo) {
        return tipo != null
                && (tipo.equalsIgnoreCase("char")
                || tipo.equalsIgnoreCase("caracter")
                || tipo.equalsIgnoreCase("littera"));
    }

    private boolean esRetorno(String direccion) {
        if (direccion == null) {
            return false;
        }

        String normalizada = direccion.replace(" ", "");

        return normalizada.equals("P")
                || normalizada.equals("P+0");
    }
}