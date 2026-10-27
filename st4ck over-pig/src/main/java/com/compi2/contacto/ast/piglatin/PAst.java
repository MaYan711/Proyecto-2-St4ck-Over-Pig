package com.compi2.contacto.ast.piglatin;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.PosicionFuente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PAst {

    private PAst() {
    }

    public interface Sentencia extends NodoAst {
    }

    public interface Inicializador extends NodoAst {
    }

    public interface Expresion extends Inicializador {
    }

    public record Programa(
            List<Importacion> importaciones,
            List<Declaracion> globales,
            List<Sentencia> principal,
            PosicionFuente posicion
    ) implements NodoAst {

        public Programa {
            importaciones = List.copyOf(importaciones);
            globales = List.copyOf(globales);
            principal = List.copyOf(principal);
            Objects.requireNonNull(posicion);
        }
    }

    public record Importacion(
            String ruta,
            String extension,
            PosicionFuente posicion
    ) implements NodoAst {

        public Importacion {
            Objects.requireNonNull(ruta);
            Objects.requireNonNull(extension);
            Objects.requireNonNull(posicion);
        }

        public boolean esY() {
            return extension.equals("y");
        }

        public boolean esZ() {
            return extension.equals("z");
        }
    }

    public record Tipo(
            String nombreBase,
            int dimensiones,
            PosicionFuente posicion
    ) implements NodoAst {

        public Tipo {
            Objects.requireNonNull(nombreBase);
            Objects.requireNonNull(posicion);

            if (dimensiones < 0) {
                throw new IllegalArgumentException(
                        "Las dimensiones no pueden ser negativas"
                );
            }
        }

        public boolean esArreglo() {
            return dimensiones > 0;
        }

        public String nombreCompleto() {
            return nombreBase
                    + "[]".repeat(dimensiones);
        }
    }

    public record Declaracion(
            String nombre,
            Tipo tipo,
            List<Expresion> dimensiones,
            Optional<Inicializador> inicializador,
            PosicionFuente posicion
    ) implements Sentencia {

        public Declaracion {
            Objects.requireNonNull(nombre);
            Objects.requireNonNull(tipo);
            dimensiones = List.copyOf(dimensiones);
            Objects.requireNonNull(inicializador);
            Objects.requireNonNull(posicion);
        }

        public boolean esArreglo() {
            return tipo.esArreglo();
        }
    }

    public record ExpresionSentencia(
            Expresion expresion,
            PosicionFuente posicion
    ) implements Sentencia {

        public ExpresionSentencia {
            Objects.requireNonNull(expresion);
            Objects.requireNonNull(posicion);
        }
    }

    public record Entrada(
            Optional<Expresion> destino,
            PosicionFuente posicion
    ) implements Sentencia {

        public Entrada {
            Objects.requireNonNull(destino);
            Objects.requireNonNull(posicion);
        }
    }

    public record Salida(
            List<Expresion> expresiones,
            PosicionFuente posicion
    ) implements Sentencia {

        public Salida {
            expresiones = List.copyOf(expresiones);
            Objects.requireNonNull(posicion);
        }
    }

    public record RamaSi(
            Optional<Expresion> condicion,
            List<Sentencia> cuerpo,
            PosicionFuente posicion
    ) implements NodoAst {

        public RamaSi {
            Objects.requireNonNull(condicion);
            cuerpo = List.copyOf(cuerpo);
            Objects.requireNonNull(posicion);
        }

        public boolean esContrario() {
            return condicion.isEmpty();
        }
    }

    public record Si(
            List<RamaSi> ramas,
            PosicionFuente posicion
    ) implements Sentencia {

        public Si {
            ramas = List.copyOf(ramas);
            Objects.requireNonNull(posicion);
        }
    }

    public record Mientras(
            Expresion condicion,
            List<Sentencia> cuerpo,
            PosicionFuente posicion
    ) implements Sentencia {

        public Mientras {
            Objects.requireNonNull(condicion);
            cuerpo = List.copyOf(cuerpo);
            Objects.requireNonNull(posicion);
        }
    }

    public record HacerMientras(
            List<Sentencia> cuerpo,
            Expresion condicion,
            PosicionFuente posicion
    ) implements Sentencia {

        public HacerMientras {
            cuerpo = List.copyOf(cuerpo);
            Objects.requireNonNull(condicion);
            Objects.requireNonNull(posicion);
        }
    }

    public record Para(
            Optional<NodoAst> inicializacion,
            Optional<Expresion> condicion,
            Optional<Expresion> actualizacion,
            List<Sentencia> cuerpo,
            PosicionFuente posicion
    ) implements Sentencia {

        public Para {
            Objects.requireNonNull(inicializacion);
            Objects.requireNonNull(condicion);
            Objects.requireNonNull(actualizacion);
            cuerpo = List.copyOf(cuerpo);
            Objects.requireNonNull(posicion);
        }
    }

    public record Continuar(
            PosicionFuente posicion
    ) implements Sentencia {

        public Continuar {
            Objects.requireNonNull(posicion);
        }
    }

    public record Romper(
            PosicionFuente posicion
    ) implements Sentencia {

        public Romper {
            Objects.requireNonNull(posicion);
        }
    }

    public record InicializadorLista(
            List<Inicializador> valores,
            PosicionFuente posicion
    ) implements Inicializador {

        public InicializadorLista {
            valores = List.copyOf(valores);
            Objects.requireNonNull(posicion);
        }
    }

    public enum TipoLiteral {
        ENTERO,
        DECIMAL,
        CADENA,
        CARACTER,
        BOOLEANO,
        NULO
    }

    public record Literal(
            TipoLiteral tipo,
            String lexema,
            PosicionFuente posicion
    ) implements Expresion {

        public Literal {
            Objects.requireNonNull(tipo);
            Objects.requireNonNull(lexema);
            Objects.requireNonNull(posicion);
        }
    }

    public record Identificador(
            String nombre,
            PosicionFuente posicion
    ) implements Expresion {

        public Identificador {
            Objects.requireNonNull(nombre);
            Objects.requireNonNull(posicion);
        }
    }

    public enum OperadorBinario {
        SUMA,
        RESTA,
        MULTIPLICACION,
        DIVISION,
        IGUALDAD,
        DIFERENTE,
        MENOR,
        MAYOR,
        MENOR_IGUAL,
        MAYOR_IGUAL,
        AND,
        OR
    }

    public record Binaria(
            OperadorBinario operador,
            Expresion izquierda,
            Expresion derecha,
            PosicionFuente posicion
    ) implements Expresion {

        public Binaria {
            Objects.requireNonNull(operador);
            Objects.requireNonNull(izquierda);
            Objects.requireNonNull(derecha);
            Objects.requireNonNull(posicion);
        }
    }

    public enum OperadorUnario {
        NEGACION,
        POSITIVO,
        NEGATIVO,
        INCREMENTO_PRE,
        DECREMENTO_PRE
    }

    public record Unaria(
            OperadorUnario operador,
            Expresion expresion,
            PosicionFuente posicion
    ) implements Expresion {

        public Unaria {
            Objects.requireNonNull(operador);
            Objects.requireNonNull(expresion);
            Objects.requireNonNull(posicion);
        }
    }

    public record Asignacion(
            Expresion destino,
            Expresion valor,
            PosicionFuente posicion
    ) implements Expresion {

        public Asignacion {
            Objects.requireNonNull(destino);
            Objects.requireNonNull(valor);
            Objects.requireNonNull(posicion);
        }
    }

    public record Llamada(
            Expresion objetivo,
            List<Expresion> argumentos,
            PosicionFuente posicion
    ) implements Expresion {

        public Llamada {
            Objects.requireNonNull(objetivo);
            argumentos = List.copyOf(argumentos);
            Objects.requireNonNull(posicion);
        }
    }

    public record AccesoArreglo(
            Expresion objetivo,
            Expresion indice,
            PosicionFuente posicion
    ) implements Expresion {

        public AccesoArreglo {
            Objects.requireNonNull(objetivo);
            Objects.requireNonNull(indice);
            Objects.requireNonNull(posicion);
        }
    }

    public record AccesoMiembro(
            Expresion objetivo,
            String miembro,
            PosicionFuente posicion
    ) implements Expresion {

        public AccesoMiembro {
            Objects.requireNonNull(objetivo);
            Objects.requireNonNull(miembro);
            Objects.requireNonNull(posicion);
        }
    }

    public enum OperacionPostfija {
        INCREMENTO,
        DECREMENTO
    }

    public record CambioPostfijo(
            Expresion objetivo,
            OperacionPostfija operacion,
            PosicionFuente posicion
    ) implements Expresion {

        public CambioPostfijo {
            Objects.requireNonNull(objetivo);
            Objects.requireNonNull(operacion);
            Objects.requireNonNull(posicion);
        }
    }

    public record NuevoObjeto(
            String tipo,
            List<Expresion> argumentos,
            PosicionFuente posicion
    ) implements Expresion {

        public NuevoObjeto {
            Objects.requireNonNull(tipo);
            argumentos = List.copyOf(argumentos);
            Objects.requireNonNull(posicion);
        }
    }
}