package com.compi2.contacto.ast.zetariano;

import com.compi2.contacto.ast.NodoAst;
import com.compi2.contacto.ast.PosicionFuente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ZAst {

    private ZAst() {
    }

    public interface Miembro extends NodoAst {
    }

    public interface Sentencia extends NodoAst {
    }

    public interface Inicializador extends NodoAst {
    }

    public interface Expresion extends Inicializador {
    }

    public record Clase(
            String nombre,
            List<Miembro> miembros,
            PosicionFuente posicion,
            Optional<String> padre
    ) implements NodoAst {

        public Clase {
            Objects.requireNonNull(nombre);
            miembros = List.copyOf(miembros);
            Objects.requireNonNull(posicion);
            Objects.requireNonNull(padre);
        }

        public Clase(
                String nombre,
                List<Miembro> miembros,
                PosicionFuente posicion
        ) {
            this(nombre, miembros, posicion, Optional.empty());
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

    public record Parametro(
            Tipo tipo,
            String nombre,
            PosicionFuente posicion
    ) implements NodoAst {

        public Parametro {
            Objects.requireNonNull(tipo);
            Objects.requireNonNull(nombre);
            Objects.requireNonNull(posicion);
        }
    }


    public record Atributo(
            Tipo tipo,
            String nombre,
            Optional<Inicializador> inicializador,
            PosicionFuente posicion,
            Visibilidad visibilidad
    ) implements Miembro {

        public Atributo {
            Objects.requireNonNull(tipo);
            Objects.requireNonNull(nombre);
            Objects.requireNonNull(inicializador);
            Objects.requireNonNull(posicion);
            Objects.requireNonNull(visibilidad);
        }

        public Atributo(
                Tipo tipo,
                String nombre,
                Optional<Inicializador> inicializador,
                PosicionFuente posicion
        ) {
            this(tipo, nombre, inicializador, posicion, Visibilidad.DEFAULT);
        }
    }

    public record Constructor(
            String nombre,
            List<Parametro> parametros,
            Bloque cuerpo,
            PosicionFuente posicion,
            Visibilidad visibilidad
    ) implements Miembro {

        public Constructor {
            Objects.requireNonNull(nombre);
            parametros = List.copyOf(parametros);
            Objects.requireNonNull(cuerpo);
            Objects.requireNonNull(posicion);
            Objects.requireNonNull(visibilidad);
        }

        public Constructor(
                String nombre,
                List<Parametro> parametros,
                Bloque cuerpo,
                PosicionFuente posicion
        ) {
            this(nombre, parametros, cuerpo, posicion, Visibilidad.PUBLICA);
        }
    }

    public record Metodo(
            Optional<Tipo> retorno,
            String nombre,
            List<Parametro> parametros,
            Bloque cuerpo,
            PosicionFuente posicion,
            Visibilidad visibilidad,
            boolean sobrescrito
    ) implements Miembro {

        public Metodo {
            Objects.requireNonNull(retorno);
            Objects.requireNonNull(nombre);
            parametros = List.copyOf(parametros);
            Objects.requireNonNull(cuerpo);
            Objects.requireNonNull(posicion);
            Objects.requireNonNull(visibilidad);
        }

        public Metodo(
                Optional<Tipo> retorno,
                String nombre,
                List<Parametro> parametros,
                Bloque cuerpo,
                PosicionFuente posicion
        ) {
            this(
                    retorno,
                    nombre,
                    parametros,
                    cuerpo,
                    posicion,
                    Visibilidad.PUBLICA,
                    false
            );
        }

        public boolean esVoid() {
            return retorno.isEmpty();
        }
    }


    public record Bloque(
            List<Sentencia> sentencias,
            PosicionFuente posicion
    ) implements Sentencia {

        public Bloque {
            sentencias = List.copyOf(sentencias);
            Objects.requireNonNull(posicion);
        }
    }

    public record Declaracion(
            Tipo tipo,
            String nombre,
            Optional<Inicializador> inicializador,
            PosicionFuente posicion
    ) implements Sentencia {

        public Declaracion {
            Objects.requireNonNull(tipo);
            Objects.requireNonNull(nombre);
            Objects.requireNonNull(inicializador);
            Objects.requireNonNull(posicion);
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

    public record Si(
            Expresion condicion,
            Sentencia entonces,
            Optional<Sentencia> sino,
            PosicionFuente posicion
    ) implements Sentencia {

        public Si {
            Objects.requireNonNull(condicion);
            Objects.requireNonNull(entonces);
            Objects.requireNonNull(sino);
            Objects.requireNonNull(posicion);
        }
    }

    public record CasoSeleccion(
            Optional<Expresion> valor,
            List<Sentencia> sentencias,
            PosicionFuente posicion
    ) implements NodoAst {

        public CasoSeleccion {
            Objects.requireNonNull(valor);
            sentencias = List.copyOf(sentencias);
            Objects.requireNonNull(posicion);
        }

        public boolean esDefault() {
            return valor.isEmpty();
        }
    }

    public record Seleccion(
            Expresion expresion,
            List<CasoSeleccion> casos,
            PosicionFuente posicion
    ) implements Sentencia {

        public Seleccion {
            Objects.requireNonNull(expresion);
            casos = List.copyOf(casos);
            Objects.requireNonNull(posicion);
        }
    }

    public record ListaExpresiones(
            List<Expresion> expresiones,
            PosicionFuente posicion
    ) implements NodoAst {

        public ListaExpresiones {
            expresiones = List.copyOf(expresiones);
            Objects.requireNonNull(posicion);
        }
    }

    public record Para(
            Optional<NodoAst> inicializacion,
            Optional<Expresion> condicion,
            List<Expresion> actualizaciones,
            Sentencia cuerpo,
            PosicionFuente posicion
    ) implements Sentencia {

        public Para {
            Objects.requireNonNull(inicializacion);
            Objects.requireNonNull(condicion);
            actualizaciones = List.copyOf(actualizaciones);
            Objects.requireNonNull(cuerpo);
            Objects.requireNonNull(posicion);
        }
    }

    public record Mientras(
            Expresion condicion,
            Sentencia cuerpo,
            PosicionFuente posicion
    ) implements Sentencia {

        public Mientras {
            Objects.requireNonNull(condicion);
            Objects.requireNonNull(cuerpo);
            Objects.requireNonNull(posicion);
        }
    }

    public record HacerMientras(
            Sentencia cuerpo,
            Expresion condicion,
            PosicionFuente posicion
    ) implements Sentencia {

        public HacerMientras {
            Objects.requireNonNull(cuerpo);
            Objects.requireNonNull(condicion);
            Objects.requireNonNull(posicion);
        }
    }

    public record Retorno(
            Optional<Expresion> expresion,
            PosicionFuente posicion
    ) implements Sentencia {

        public Retorno {
            Objects.requireNonNull(expresion);
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

    public record Continuar(
            PosicionFuente posicion
    ) implements Sentencia {

        public Continuar {
            Objects.requireNonNull(posicion);
        }
    }

    public record Vacia(
            PosicionFuente posicion
    ) implements Sentencia {

        public Vacia {
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
        MODULO,

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

    public enum OperadorAsignacion {
        ASIGNAR,
        SUMAR_ASIGNAR,
        RESTAR_ASIGNAR,
        MULTIPLICAR_ASIGNAR,
        DIVIDIR_ASIGNAR
    }

    public record AsignacionExpresion(
            OperadorAsignacion operador,
            Expresion destino,
            Expresion valor,
            PosicionFuente posicion
    ) implements Expresion {

        public AsignacionExpresion {
            Objects.requireNonNull(operador);
            Objects.requireNonNull(destino);
            Objects.requireNonNull(valor);
            Objects.requireNonNull(posicion);
        }
    }

    public record Ternaria(
            Expresion condicion,
            Expresion verdadero,
            Expresion falso,
            PosicionFuente posicion
    ) implements Expresion {

        public Ternaria {
            Objects.requireNonNull(condicion);
            Objects.requireNonNull(verdadero);
            Objects.requireNonNull(falso);
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

    public record NuevoArreglo(
            String tipoBase,
            List<Expresion> dimensiones,
            PosicionFuente posicion
    ) implements Expresion {

        public NuevoArreglo {
            Objects.requireNonNull(tipoBase);
            dimensiones = List.copyOf(dimensiones);
            Objects.requireNonNull(posicion);
        }
    }
}