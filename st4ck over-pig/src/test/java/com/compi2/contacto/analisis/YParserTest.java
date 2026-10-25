package com.compi2.contacto.analisis;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YParserTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaProgramaCompletoDelLenguajeY() {
        String codigo = """
                %estructuras
                estructura Direccion:
                    cadena calle
                    entero numero
                estructura Persona:
                    cadena nombre
                    entero edad
                    Direccion domicilio
                    entero notas[3]
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    entero total = a + b
                    retornar total
                definir procesar([] entero datos, {} Persona persona):
                    entero i = 0
                    si(persona.edad > 18) entonces
                        imprimir(persona.nombre)
                    sino (persona.edad == 18) entonces
                        imprimir("18")
                    contrario
                        imprimir("menor")
                    elegir(datos[0]):
                        caso 1:
                            datos[0] = datos[0] + 1
                            romper
                        siempre:
                            datos[0] = 0
                            romper
                    para(entero j = 0; j < 10; j++):
                        si(j == 3) entonces
                            continuar
                    mientras(i < 5) hacer
                        i++
                    hacer:
                        i--
                    mientras(i > 0)
                    cadena entrada = leer()
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaEstructuraLocalArreglosYMatrices() {
        String codigo = """
                %funciones
                definir prueba():
                    estructura Punto:
                        entero x
                        entero y
                        flotante promedio
                    Punto p1 = {10, 20, 85.5}
                    Punto p2 = {5, 15, 90.0}
                    entero numeros[5] = {10, 20, 30, 40, 50}
                    entero matriz[3][3]
                    flotante suma = p1.promedio + p2.promedio
                    matriz[0][0] = numeros[0] * 3
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaParametrosPorValorArregloYEstructura() {
        String codigo = """
                %funciones
                definir prueba(
                    entero valor,
                    [] entero datos,
                    {} Persona persona
                ) -> entero:
                    retornar valor + datos[0]
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaPuntoYComaOpcionalEnSentenciasSimples() {
        String codigo = """
                %funciones
                definir prueba():
                    entero contador = 0;
                    contador++;
                    si(contador == 1) entonces
                        continuar;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaArchivoSinSeccionFunciones() {
        String codigo = """
                %estructuras
                estructura Persona:
                    entero edad
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaVariableGlobal() {
        String codigo = """
                entero global = 10
                %funciones
                definir prueba():
                    imprimir("hola")
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaFuncionSinBloqueIndentado() {
        String codigo = """
                %funciones
                definir prueba():
                imprimir("hola")
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaSinoSinCondicion() {
        String codigo = """
                %funciones
                definir prueba(entero edad):
                    si(edad > 18) entonces
                        imprimir("mayor")
                    sino
                        imprimir("otro")
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    private ResultadoAnalisisArchivo analizar(
            String codigo
    ) {
        ArchivoFuente archivo = new ArchivoFuente(
                Path.of("Funciones.y"),
                LenguajeFuente.Y,
                codigo
        );

        return analizador.analizar(archivo);
    }
}