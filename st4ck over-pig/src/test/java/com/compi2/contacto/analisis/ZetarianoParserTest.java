package com.compi2.contacto.analisis;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZetarianoParserTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaClaseConAtributosConstructoresYMetodos() {
        String codigo = """
                public class Persona {

                    String nombre;
                    int edad;

                    public Persona(String nombreParametro, int edadParametro) {
                        nombre = nombreParametro;
                        edad = edadParametro;
                    }

                    public Persona() {
                        nombre = "Sin nombre";
                        edad = 0;
                    }

                    public void saludar() {
                        println("Hola");
                    }

                    public int calcularAnioNacimiento(int anioActual) {
                        return anioActual - edad;
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Persona.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaExpresionesDelEnunciado() {
        String codigo = """
                public class Operaciones {

                    public void prueba() {
                        int a = 10;
                        int b = 3;

                        int suma = a + b;
                        int resta = a - b;
                        int multiplicacion = a * b;
                        int division = a / b;
                        int modulo = a % b;

                        a++;
                        b--;

                        boolean esMayor = a > b;
                        boolean esIgual = a == 10;
                        boolean condicionAnd = a > 5 && b < 10;
                        boolean condicionOr = a > 20 || b > 0;
                        boolean condicionNot = !(a == 10);

                        a += 3;
                        a -= 2;
                        a *= 2;
                        a /= 2;
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Operaciones.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaTernario() {
        String codigo = """
                public class Ternarios {

                    public void prueba() {
                        int edad = 20;

                        String mensaje =
                            edad >= 18
                                ? "Es mayor de edad"
                                : "Es menor de edad";

                        int numero = 7;

                        int par =
                            numero % 2 == 0
                                ? 0
                                : 1;
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Ternarios.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaArreglosYMatrices() {
        String codigo = """
                public class Arreglos {

                    public void prueba() {
                        int[] numeros = {10, 20, 30, 40, 50};

                        int resultado;

                        resultado = numeros[0] + numeros[1];

                        numeros[2] =
                            numeros[0] * 3;

                        int[] calificaciones =
                            new int[5];

                        String[] nombres =
                            {"Carlos", "Ana", "Pedro"};

                        int[][] matriz =
                            new int[3][3];

                        matriz[0][0] = 10;
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Arreglos.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaObjetosNewAccesosYLlamadas() {
        String codigo = """
                public class PruebaObjetos {

                    public void prueba() {

                        Persona p1 =
                            new Persona("Carlos", 25);

                        Persona p2 =
                            new Persona("Ana", 30);

                        int sumaEdades;

                        sumaEdades =
                            p1.edad + p2.edad;

                        int anio =
                            2026 - p1.obtenerEdad();

                        p1.saludar();

                        if (p1 == null) {
                            print("Es nulo");
                        }
                    }
                }
                """;

        assertTrue(
                analizar(
                        "PruebaObjetos.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaIfConYLlavesSinLlaves() {
        String codigo = """
                public class Condicionales {

                    public void prueba() {

                        int edad = 18;

                        if (edad > 18) {
                            println("Mayor");
                        } else if (edad == 18) {
                            println("Igual");
                        } else {
                            println("Menor");
                        }

                        if (edad >= 18)
                            println("Adulto");
                        else
                            println("Menor");

                        if (edad == 18)
                            if (edad > 10)
                                print("hola");
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Condicionales.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaSwitch() {
        String codigo = """
                public class Selector {

                    public void prueba() {

                        int opcion = 2;

                        switch (opcion) {

                            case 1:
                                println("Uno");
                                break;

                            case 2:
                                println("Dos");

                            default:
                                println("Default");
                                break;
                        }
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Selector.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaForWhileYDoWhile() {
        String codigo = """
                public class Ciclos {

                    public void prueba() {

                        for (int i = 0; i < 5; i++) {

                            println(i);

                            if (i == 2) {
                                continue;
                            }

                            if (i == 4) {
                                break;
                            }
                        }

                        for (;;) {
                            println("infinito");
                            break;
                        }

                        int contador = 0;

                        while (contador < 3) {
                            contador++;

                            if (contador == 1) {
                                continue;
                            }
                        }

                        int intentos = 0;

                        do {
                            intentos++;

                            if (intentos == 2) {
                                break;
                            }

                        } while (intentos < 5);
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Ciclos.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaSystemOutPrintln() {
        String codigo = """
                public class Salida {

                    public void prueba() {

                        System.out.println(
                            "Hola"
                        );

                        System.out.print(
                            "Sin salto"
                        );

                        println("Directo");

                        print("Directo sin salto");

                        String texto =
                            readln();
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Salida.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void aceptaSobrecargaDeMetodos() {
        String codigo = """
                public class Calculadora {

                    public int sumar(
                        int a,
                        int b
                    ) {
                        return a + b;
                    }

                    public double sumar(
                        double a,
                        double b
                    ) {
                        return a + b;
                    }
                }
                """;

        assertTrue(
                analizar(
                        "Calculadora.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void rechazaMetodoSinPuntoYComaEnSentencia() {
        String codigo = """
                public class ErrorSintactico {

                    public void prueba() {
                        int edad = 20
                        edad++;
                    }
                }
                """;

        assertFalse(
                analizar(
                        "ErrorSintactico.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void rechazaClaseSinLlaveDeCierre() {
        String codigo = """
                public class Incompleta {

                    int edad;

                    public void prueba() {
                        edad = 10;
                    }
                """;

        assertFalse(
                analizar(
                        "Incompleta.z",
                        codigo
                ).esValido()
        );
    }

    @Test
    void rechazaContenidoFueraDeClase() {
        String codigo = """
                int variable = 10;

                public class Persona {
                    int edad;
                }
                """;

        assertFalse(
                analizar(
                        "Persona.z",
                        codigo
                ).esValido()
        );
    }

    private ResultadoAnalisisArchivo analizar(
            String nombre,
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of(nombre),
                        LenguajeFuente.ZETARIANO,
                        codigo
                );

        return analizador.analizar(
                archivo
        );
    }
}