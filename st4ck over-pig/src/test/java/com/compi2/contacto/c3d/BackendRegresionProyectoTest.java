package com.compi2.contacto.c3d;

import com.compi2.contacto.compilacion.CompiladorProyecto;
import com.compi2.contacto.compilacion.ResultadoCompilacion;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendRegresionProyectoTest {

    private static final Set<OperadorCuarteta> OPERADORES_SIN_BAJAR =
            EnumSet.of(
                    OperadorCuarteta.INICIALIZAR_COMPUESTO,
                    OperadorCuarteta.PARAMETRO,
                    OperadorCuarteta.NUEVO_OBJETO,
                    OperadorCuarteta.NUEVO_ARREGLO
            );

    @TempDir
    Path temporal;

    @Test
    void estructurasAnidadasYArregloEnEstructuraLleganAC() throws Exception {
        Path raiz = temporal.resolve("estructuras");

        String y = """
                %estructuras
                estructura Direccion:
                    entero numero
                    entero zona
                estructura Persona:
                    entero edad
                    Direccion domicilio
                estructura Estudiante:
                    entero codigo
                    entero calificaciones[3]
                %funciones
                definir obtenerEdad({} Persona persona) -> entero:
                    retornar persona.edad
                definir obtenerNumero({} Persona persona) -> entero:
                    Direccion direccion = persona.domicilio
                    retornar direccion.numero
                definir obtenerZona({} Persona persona) -> entero:
                    Direccion direccion = persona.domicilio
                    retornar direccion.zona
                definir obtenerCodigo({} Estudiante estudiante) -> entero:
                    retornar estudiante.codigo
                definir sumarNotas({} Estudiante estudiante) -> entero:
                    retornar estudiante.calificaciones[0] + estudiante.calificaciones[1] + estudiante.calificaciones[2]
                """;

        String pig = """
                import estructuras.y

                VARIABILES>
                esto direccion_base : Direccion {700, 12};
                esto ciudadano_inline : Persona {25, {500, 8}};
                esto ciudadano_referencia : Persona {30, direccion_base};
                series notas[3] : numerus {10, 20, 30};
                esto alumno : Estudiante {202031554, notas};

                MAIOR>
                >> obtenerEdad(ciudadano_inline);
                >> "\\n";
                >> obtenerNumero(ciudadano_inline);
                >> "\\n";
                >> obtenerZona(ciudadano_inline);
                >> "\\n";
                >> obtenerNumero(ciudadano_referencia);
                >> "\\n";
                >> obtenerZona(ciudadano_referencia);
                >> "\\n";
                >> obtenerCodigo(alumno);
                >> "\\n";
                >> sumarNotas(alumno);
                >> "\\n";
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "estructuras.y", LenguajeFuente.Y, y),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        ejecutarSiHayGcc(
                codigo,
                "25\n500\n8\n700\n12\n202031554\n60\n"
        );
    }

    @Test
    void arreglosSinInicializadorReservanMemoria() throws Exception {
        Path raiz = temporal.resolve("arreglos");

        String y = """
                %funciones
                definir probarArregloY() -> entero:
                    entero numeros[5]
                    numeros[0] = 10
                    numeros[1] = 20
                    retornar numeros[0] + numeros[1] + numeros[4]

                definir probarMatrizY() -> entero:
                    entero matriz[2][3]
                    matriz[1][2] = 7
                    retornar matriz[0][0] + matriz[1][2]
                """;

        String pig = """
                import arreglos.y

                VARIABILES>
                series valores[3] : numerus;
                series tabla[2][2] : numerus;

                MAIOR>
                valores[0] = 7;
                valores[1] = 25;
                tabla[0][1] = 11;

                >> probarArregloY();
                >> "\\n";
                >> probarMatrizY();
                >> "\\n";
                >> valores[0];
                >> "\\n";
                >> valores[1];
                >> "\\n";
                >> valores[2];
                >> "\\n";
                >> tabla[0][1];
                >> "\\n";
                >> tabla[1][1];
                >> "\\n";
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "arreglos.y", LenguajeFuente.Y, y),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        ejecutarSiHayGcc(
                codigo,
                "30\n7\n7\n25\n0\n11\n0\n"
        );
    }

    @Test
    void referenciasSinInicializarUsanNull() throws Exception {
        Path raiz = temporal.resolve("referencias");

        String z = """
                public class Referencias {
                    String texto;
                    int numero;
                    Referencias otra;

                    public Referencias(int valor) {
                        numero = valor;
                    }

                    public int validarCampos() {
                        int resultado = 0;

                        if (texto == null) {
                            resultado += 1;
                        }

                        if (otra == null) {
                            resultado += 10;
                        }

                        if (numero == 7) {
                            resultado += 100;
                        }

                        return resultado;
                    }

                    public int validarLocales() {
                        int numeroLocal;
                        String textoLocal;
                        Referencias objetoLocal;
                        int resultado = 0;

                        if (numeroLocal == 0) {
                            resultado += 1;
                        }

                        if (textoLocal == null) {
                            resultado += 10;
                        }

                        if (objetoLocal == null) {
                            resultado += 100;
                        }

                        return resultado;
                    }

                    public int validarArreglos() {
                        Referencias[] objetos = new Referencias[2];
                        String[] textos = new String[2];
                        int[] numeros = new int[2];
                        int resultado = 0;

                        if (objetos[0] == null) {
                            resultado += 1;
                        }

                        if (objetos[1] == null) {
                            resultado += 10;
                        }

                        if (textos[0] == null) {
                            resultado += 100;
                        }

                        if (textos[1] == null) {
                            resultado += 1000;
                        }

                        if (numeros[0] == 0) {
                            resultado += 10000;
                        }

                        if (numeros[1] == 0) {
                            resultado += 100000;
                        }

                        return resultado;
                    }
                }
                """;

        String pig = """
                import Referencias.z

                VARIABILES>
                esto prueba : novus Referencias(7);

                MAIOR>
                >> prueba.validarCampos();
                >> "\\n";
                >> prueba.validarLocales();
                >> "\\n";
                >> prueba.validarArreglos();
                >> "\\n";
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "Referencias.z", LenguajeFuente.ZETARIANO, z),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        ejecutarSiHayGcc(
                codigo,
                "111\n111\n111111\n"
        );
    }

    @Test
    void lecturaTipadaGeneraRuntimeCompleto() {
        Path raiz = temporal.resolve("lectura");

        String y = """
                %funciones
                definir leerNombre():
                    cadena nombre = leer()
                    imprimir(nombre)
                """;

        String z = """
                public class Entrada {
                    public void leerCiudad() {
                        String ciudad = readln();
                        println(ciudad);
                    }
                }
                """;

        String pig = """
                import entrada.y
                import Entrada.z

                VARIABILES>
                esto lector : novus Entrada();
                esto texto : textum "";
                esto numero : numerus 0;
                esto decimal : decimalis 0.0;
                esto letra : littera 'x';
                esto bandera : bool falsus;

                MAIOR>
                leerNombre();
                lector.leerCiudad();
                texto <<
                numero <<
                decimal <<
                letra <<
                bandera <<
                <<;
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "entrada.y", LenguajeFuente.Y, y),
                        archivo(raiz, "Entrada.z", LenguajeFuente.ZETARIANO, z),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        assertTrue(codigo.contains("runtime_read_string"));
        assertTrue(codigo.contains("runtime_read_number"));
        assertTrue(codigo.contains("runtime_read_char"));
        assertTrue(codigo.contains("runtime_read_boolean"));
        assertTrue(codigo.contains("runtime_discard_input"));
    }

    @Test
    void sobrecargaSeConservaHastaCodigoC() throws Exception {
        Path raiz = temporal.resolve("sobrecarga");

        String sobrecarga = """
                public class Sobrecarga {
                    int marca;

                    public Sobrecarga(int valor) {
                        marca = 100;
                    }

                    public Sobrecarga(double valor) {
                        marca = 200;
                    }

                    public int getMarca() {
                        return marca;
                    }

                    public int seleccionar(int valor) {
                        return 10;
                    }

                    public int seleccionar(double valor) {
                        return 20;
                    }

                    public int probarInternaInt() {
                        return seleccionar(1);
                    }

                    public int probarInternaDouble() {
                        return seleccionar(1.5);
                    }

                    public int soloInt(int valor) {
                        return valor;
                    }

                    public double soloDouble(double valor) {
                        return valor + 0.5;
                    }

                    public int probarCharAInt() {
                        return soloInt('A');
                    }

                    public double probarIntADouble() {
                        return soloDouble(7);
                    }
                }
                """;

        String fabrica = """
                public class Fabrica {
                    public Fabrica() {
                    }

                    public int probarConstructores() {
                        Sobrecarga entero = new Sobrecarga(10);
                        Sobrecarga decimal = new Sobrecarga(10.5);
                        return entero.getMarca() + decimal.getMarca();
                    }

                    public int probarMetodosObjeto() {
                        Sobrecarga prueba = new Sobrecarga(1);
                        return prueba.seleccionar(2) + prueba.seleccionar(2.5);
                    }

                    public int probarLlamadasInternas() {
                        Sobrecarga prueba = new Sobrecarga(1);
                        return prueba.probarInternaInt() + prueba.probarInternaDouble();
                    }

                    public int probarCharAInt() {
                        Sobrecarga prueba = new Sobrecarga(1);
                        return prueba.probarCharAInt();
                    }

                    public double probarIntADouble() {
                        Sobrecarga prueba = new Sobrecarga(1);
                        return prueba.probarIntADouble();
                    }
                }
                """;

        String pig = """
                import Sobrecarga.z
                import Fabrica.z

                VARIABILES>
                esto fabrica : novus Fabrica();

                MAIOR>
                >> fabrica.probarConstructores();
                >> "\\n";
                >> fabrica.probarMetodosObjeto();
                >> "\\n";
                >> fabrica.probarLlamadasInternas();
                >> "\\n";
                >> fabrica.probarCharAInt();
                >> "\\n";
                >> fabrica.probarIntADouble();
                >> "\\n";
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "Sobrecarga.z", LenguajeFuente.ZETARIANO, sobrecarga),
                        archivo(raiz, "Fabrica.z", LenguajeFuente.ZETARIANO, fabrica),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        ejecutarSiHayGcc(
                codigo,
                "300\n30\n30\n65\n7.5\n"
        );
    }

    @Test
    void cadenasSePropaganPorStackHeapYArreglos() throws Exception {
        Path raiz = temporal.resolve("cadenas");

        String y = """
                %funciones
                definir decorar(cadena nombre) -> cadena:
                    retornar "Hola " + nombre

                definir duplicar(cadena valor) -> cadena:
                    cadena copia = valor + valor
                    retornar copia
                """;

        String z = """
                public class Cadenas {
                    String prefijo;

                    public Cadenas(String valor) {
                        prefijo = valor;
                    }

                    public String unir(String nombre) {
                        return prefijo + nombre;
                    }

                    public String obtenerPrefijo() {
                        return prefijo;
                    }

                    public String eco(String valor) {
                        return valor + "!";
                    }

                    public String probarArreglo() {
                        String[] nombres = new String[2];
                        nombres[0] = "Ana";
                        nombres[1] = nombres[0] + " clon";
                        return nombres[1];
                    }
                }
                """;

        String pig = """
                import cadenas.y
                import Cadenas.z

                VARIABILES>
                esto objeto : novus Cadenas("Z-");
                series textos[2] : textum {"Capitan", "Base"};

                MAIOR>
                >> decorar("Kenny");
                >> "\\n";
                >> duplicar("AB");
                >> "\\n";
                >> objeto.unir("Mario");
                >> "\\n";
                >> objeto.obtenerPrefijo();
                >> "\\n";
                >> objeto.eco("Hola");
                >> "\\n";
                >> objeto.probarArreglo();
                >> "\\n";
                textos[1] = textos[0] + " clon";
                >> textos[1];
                >> "\\n";
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "cadenas.y", LenguajeFuente.Y, y),
                        archivo(raiz, "Cadenas.z", LenguajeFuente.ZETARIANO, z),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        ejecutarSiHayGcc(
                codigo,
                "Hola Kenny\nABAB\nZ-Mario\nZ-\nHola!\nAna clon\nCapitan clon\n"
        );
    }

    @Test
    void divisionEnteraYSalidaTipadaMantienenSemanticaZ() throws Exception {
        Path raiz = temporal.resolve("division");

        String z = """
                public class DivisionSalida {
                    public DivisionSalida() {
                    }

                    public int dividirPositivo() {
                        return 7 / 2;
                    }

                    public int dividirDiez() {
                        return 10 / 3;
                    }

                    public int dividirNegativo() {
                        return -7 / 2;
                    }

                    public double dividirDecimal() {
                        return 7.0 / 2;
                    }

                    public int dividirAsignacion() {
                        int x = 9;
                        x /= 2;
                        return x;
                    }

                    public void probarSalida() {
                        println('A');
                        print('B');
                        println('C');
                        println(true);
                        println(false);
                    }
                }
                """;

        String pig = """
                import DivisionSalida.z

                VARIABILES>
                esto prueba : novus DivisionSalida();

                MAIOR>
                >> prueba.dividirPositivo();
                >> "\\n";
                >> prueba.dividirDiez();
                >> "\\n";
                >> prueba.dividirNegativo();
                >> "\\n";
                >> prueba.dividirDecimal();
                >> "\\n";
                >> prueba.dividirAsignacion();
                >> "\\n";
                prueba.probarSalida();
                FINIS;
                """;

        ResultadoCompilacion resultado =
                compilar(
                        raiz,
                        archivo(raiz, "DivisionSalida.z", LenguajeFuente.ZETARIANO, z),
                        archivo(raiz, "main.pig", LenguajeFuente.PIG_LATIN, pig)
                );

        String codigo = generarCodigo(resultado);

        ejecutarSiHayGcc(
                codigo,
                "3\n3\n-3\n3.5\n4\nA\nBC\ntrue\nfalse\n"
        );
    }

    private ResultadoCompilacion compilar(
            Path raiz,
            ArchivoFuente... archivos
    ) {
        ProyectoCompilacion proyecto =
                new ProyectoCompilacion(
                        raiz,
                        List.of(archivos)
                );

        ResultadoCompilacion resultado =
                new CompiladorProyecto()
                        .compilar(
                                proyecto
                        );

        assertTrue(
                resultado.esValido(),
                () -> resultado.diagnosticos().toString()
        );

        return resultado;
    }

    private String generarCodigo(
            ResultadoCompilacion resultado
    ) {
        boolean quedanOperadoresAltos =
                resultado.programaMemoria()
                        .cuartetas()
                        .stream()
                        .anyMatch(
                                cuarteta ->
                                        OPERADORES_SIN_BAJAR.contains(
                                                cuarteta.operador()
                                        )
                        );

        assertFalse(
                quedanOperadoresAltos,
                () -> resultado.programaMemoria()
                        .cuartetas()
                        .toString()
        );

        String codigo =
                new GeneradorCodigoC()
                        .generar(
                                resultado.programaMemoria()
                        );

        assertFalse(codigo.isBlank());
        assertTrue(codigo.contains("int main(void)"));

        return codigo;
    }

    private void ejecutarSiHayGcc(
            String codigo,
            String salidaEsperada
    ) throws Exception {
        Assumptions.assumeTrue(
                gccDisponible(),
                "GCC no esta disponible en PATH"
        );

        Path fuente =
                temporal.resolve(
                        "regresion_"
                                + System.nanoTime()
                                + ".c"
                );

        boolean windows =
                System.getProperty("os.name")
                        .toLowerCase()
                        .contains("win");

        Path ejecutable =
                temporal.resolve(
                        "regresion_"
                                + System.nanoTime()
                                + (windows ? ".exe" : "")
                );

        Files.writeString(
                fuente,
                codigo,
                StandardCharsets.UTF_8
        );

        Process compilacion =
                new ProcessBuilder(
                        "gcc",
                        fuente.toString(),
                        "-o",
                        ejecutable.toString(),
                        "-lm"
                )
                        .redirectErrorStream(true)
                        .start();

        String salidaCompilacion =
                new String(
                        compilacion.getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        assertTrue(
                compilacion.waitFor(
                        20,
                        TimeUnit.SECONDS
                ),
                "GCC excedio el tiempo limite"
        );

        assertEquals(
                0,
                compilacion.exitValue(),
                salidaCompilacion
        );

        Process ejecucion =
                new ProcessBuilder(
                        ejecutable.toString()
                )
                        .redirectErrorStream(true)
                        .start();

        String salida =
                new String(
                        ejecucion.getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        assertTrue(
                ejecucion.waitFor(
                        10,
                        TimeUnit.SECONDS
                ),
                "El ejecutable C excedio el tiempo limite"
        );

        assertEquals(
                0,
                ejecucion.exitValue(),
                salida
        );

        assertEquals(
                normalizar(salidaEsperada),
                normalizar(salida)
        );
    }

    private boolean gccDisponible() {
        try {
            Process proceso =
                    new ProcessBuilder(
                            "gcc",
                            "--version"
                    )
                            .redirectErrorStream(true)
                            .start();

            proceso.getInputStream()
                    .readAllBytes();

            return proceso.waitFor(
                    5,
                    TimeUnit.SECONDS
            )
                    && proceso.exitValue() == 0;

        } catch (IOException excepcion) {
            return false;

        } catch (InterruptedException excepcion) {
            Thread.currentThread()
                    .interrupt();

            return false;
        }
    }

    private String normalizar(
            String texto
    ) {
        return texto
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .strip();
    }

    private ArchivoFuente archivo(
            Path raiz,
            String nombre,
            LenguajeFuente lenguaje,
            String contenido
    ) {
        return new ArchivoFuente(
                raiz.resolve(nombre),
                lenguaje,
                contenido
        );
    }
}
