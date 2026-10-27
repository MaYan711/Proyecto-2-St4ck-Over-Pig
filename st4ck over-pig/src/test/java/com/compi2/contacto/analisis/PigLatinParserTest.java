package com.compi2.contacto.analisis;

import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.LenguajeFuente;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PigLatinParserTest {

    private final AnalizadorArchivo analizador =
            new AnalizadorArchivo();

    @Test
    void aceptaProgramaPrincipalMinimo() {
        String codigo = """
                MAIOR>
                >> "Hola";
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaImportaciones() {
        String codigo = """
                import carpeta.Objeto1.z
                import carpeta.Funciones.y

                MAIOR>
                >> "Hola";
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaVariablesGlobales() {
        String codigo = """
                VARIABILES>
                esto edad : numerus 20;
                esto promedio : decimalis 85.5;
                esto nombre : textum "Mario";
                esto letra : littera 'A';
                esto activo : verum;

                MAIOR>
                >> nombre;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaArreglos() {
        String codigo = """
                VARIABILES>
                series numeros[3] : numerus {1, 2, 3};
                series nombres[2] : textum {"Mario", "Ana"};
                series matriz[2][2] : numerus {{1, 2}, {3, 4}};

                MAIOR>
                numeros[0] = 10;
                >> numeros[0];
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaEstructurasImportadas() {
        String codigo = """
                import datos.Estructuras.y

                VARIABILES>
                esto direccion : Direccion {"Calle Real", 42};
                esto persona : Persona {"Mario", 20, direccion};
                series personas[3] : Persona;

                MAIOR>
                persona.nombre = "Carlos";
                >> persona.nombre;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaObjetosZetarianos() {
        String codigo = """
                import modelos.Persona.z

                VARIABILES>
                esto persona : novus Persona(20, "Mario");
                esto otra : novus Persona();

                MAIOR>
                persona.nombre = "Carlos";
                persona.saludar();
                >> persona.getNombre();
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaAccesosEncadenados() {
        String codigo = """
                import modelos.Persona.z

                MAIOR>
                misObjetos[9].hablar(miObjeto.getNombre());
                miCadena = miObjeto.apellidos[0].getNombre();
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaEntradaYSalida() {
        String codigo = """
                VARIABILES>
                esto nombre : textum "Inicial";

                MAIOR>
                >> "Ingresa tu nombre";
                nombre <<;
                >> "Bienvenido" >> nombre;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaCondicionalSimple() {
        String codigo = """
                VARIABILES>
                esto x : numerus 20;

                MAIOR>
                si (x > 10) {
                    >> "Mayor";
                } finis;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaIfElseIfElse() {
        String codigo = """
                VARIABILES>
                esto x : numerus 20;

                MAIOR>
                si (x > 20) {
                    >> "Mayor";
                } aliter (x == 20) {
                    >> "Igual";
                } aliter {
                    >> "Menor";
                } finis;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaWhile() {
        String codigo = """
                VARIABILES>
                esto x : numerus 0;

                MAIOR>
                dum (x < 100) {
                    x = x + 1;
                    si (x == 50) {
                        perge;
                    } finis;
                } finis;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaDoWhile() {
        String codigo = """
                VARIABILES>
                esto x : numerus 0;

                MAIOR>
                facere {
                    x++;
                } dum (x < 10);
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaFor() {
        String codigo = """
                MAIOR>
                per (esto i : numerus 0; i < 10; i++) {
                    >> i;
                    si (i == 8) {
                        interrumpe;
                    } finis;
                }
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void aceptaExpresiones() {
        String codigo = """
                VARIABILES>
                esto a : numerus 10;
                esto b : numerus 20;
                esto resultado : numerus 0;
                esto bandera : verum;

                MAIOR>
                resultado = a + b * 2;
                bandera = a < b && b != 0;
                resultado++;
                --resultado;
                FINIS;
                """;

        assertTrue(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaProgramaSinMaior() {
        String codigo = """
                VARIABILES>
                esto edad : numerus 20;
                FINIS;
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaProgramaSinFinisFinal() {
        String codigo = """
                MAIOR>
                >> "Hola";
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaDeclaracionSinPuntoYComa() {
        String codigo = """
                VARIABILES>
                esto edad : numerus 20

                MAIOR>
                >> edad;
                FINIS;
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    @Test
    void rechazaEstructuraDefinidaDentroDePig() {
        String codigo = """
                MAIOR>
                estructura Persona {
                }
                FINIS;
                """;

        assertFalse(
                analizar(codigo).esValido()
        );
    }

    private ResultadoAnalisisArchivo analizar(
            String codigo
    ) {
        ArchivoFuente archivo =
                new ArchivoFuente(
                        Path.of("Main.pig"),
                        LenguajeFuente.PIG_LATIN,
                        codigo
                );

        return analizador.analizar(
                archivo
        );
    }
}
