package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.piglatin.PigLatinLexer;
import com.compi2.contacto.antlr.piglatin.PigLatinParser;
import com.compi2.contacto.antlr.y.YLexer;
import com.compi2.contacto.antlr.y.YParser;
import com.compi2.contacto.antlr.zetariano.ZetarianoLexer;
import com.compi2.contacto.antlr.zetariano.ZetarianoParser;
import com.compi2.contacto.ast.ProgramaAst;
import com.compi2.contacto.ast.zetariano.ZAst;
import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import com.compi2.contacto.proyecto.ArchivoFuente;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AnalizadorArchivo {


    public ResultadoAnalisisArchivo analizar(
            ArchivoFuente archivo
    ) {
        Objects.requireNonNull(
                archivo,
                "El archivo es obligatorio"
        );

        List<Diagnostico> diagnosticos =
                new ArrayList<>();

        Optional<ProgramaAst> ast =
                switch (archivo.lenguaje()) {
                    case Y -> analizarY(
                            archivo,
                            diagnosticos
                    );

                    case ZETARIANO -> analizarZetariano(
                            archivo,
                            diagnosticos
                    );


                    case PIG_LATIN -> analizarPigLatin(
                            archivo,
                            diagnosticos
                    );

                };

        if (archivo.contenido().isBlank()) {
            diagnosticos.add(
                    new Diagnostico(
                            TipoDiagnostico.PROYECTO,
                            Severidad.ADVERTENCIA,
                            archivo.ruta(),
                            1,
                            0,
                            "El archivo esta vacio"
                    )
            );
        }

        return new ResultadoAnalisisArchivo(
                archivo,
                diagnosticos,
                ast
        );
    }

    private Optional<ProgramaAst> analizarY(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        YLexer lexer = new YLexer(
                CharStreams.fromString(
                        archivo.contenido()
                )
        );

        configurarLexer(
                archivo,
                lexer,
                diagnosticos
        );

        TokenizadorIndentacionY indentacion =
                new TokenizadorIndentacionY(
                        lexer
                );

        registrarErroresIndentacion(
                archivo,
                indentacion,
                diagnosticos
        );

        YParser parser = new YParser(
                new CommonTokenStream(
                        indentacion
                )
        );

        configurarParser(
                archivo,
                parser,
                diagnosticos
        );

        YParser.ArchivoContext arbol =
                parser.archivo();

        if (contieneErrores(diagnosticos)) {
            return Optional.empty();
        }

        ProgramaAst ast =
                new YAstBuilder(
                        archivo.ruta()
                ).construir(arbol);

        return Optional.of(ast);
    }

    private void registrarErroresIndentacion(
            ArchivoFuente archivo,
            TokenizadorIndentacionY indentacion,
            List<Diagnostico> diagnosticos
    ) {
        indentacion.erroresIndentacion()
                .forEach(token ->
                        diagnosticos.add(
                                new Diagnostico(
                                        TipoDiagnostico.SINTACTICO,
                                        Severidad.ERROR,
                                        archivo.ruta(),
                                        Math.max(
                                                token.getLine(),
                                                1
                                        ),
                                        Math.max(
                                                token.getCharPositionInLine(),
                                                0
                                        ),
                                        token.getText()
                                )
                        )
                );
    }

    private Optional<ProgramaAst> analizarZetariano(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        ZetarianoLexer lexer =
                new ZetarianoLexer(
                        CharStreams.fromString(
                                archivo.contenido()
                        )
                );

        ZetarianoParser parser =
                new ZetarianoParser(
                        new CommonTokenStream(
                                lexer
                        )
                );

        configurar(
                archivo,
                lexer,
                parser,
                diagnosticos
        );

        ZetarianoParser.ArchivoContext arbol =
                parser.archivo();

        if (contieneErrores(diagnosticos)) {
            return Optional.empty();
        }

        ProgramaAst ast =
                new ZAstBuilder(
                        archivo.ruta()
                ).construir(arbol);


        validarNombreClase(
                archivo,
                ast,
                diagnosticos
        );

        return Optional.of(ast);
    }

    private Optional<ProgramaAst> analizarPigLatin(
            ArchivoFuente archivo,
            List<Diagnostico> diagnosticos
    ) {
        PigLatinLexer lexer =
                new PigLatinLexer(
                        CharStreams.fromString(
                                archivo.contenido()
                        )
                );

        CommonTokenStream tokens =
                new CommonTokenStream(
                        lexer
                );

        PigLatinParser parser =
                new PigLatinParser(
                        tokens
                );

        configurar(
                archivo,
                lexer,
                parser,
                diagnosticos
        );

        PigLatinParser.ArchivoContext arbol =
                parser.archivo();

        validarEntradaPigLatin(
                archivo,
                tokens,
                diagnosticos
        );

        if (contieneErrores(diagnosticos)) {
            return Optional.empty();
        }

        ProgramaAst ast =
                new PAstBuilder(
                        archivo.ruta()
                ).construir(arbol);

        return Optional.of(ast);
    }

    private void validarEntradaPigLatin(
            ArchivoFuente archivo,
            CommonTokenStream tokens,
            List<Diagnostico> diagnosticos
    ) {
        List<Token> lista =
                tokens.getTokens();

        for (int indice = 0;
             indice < lista.size();
             indice++) {

            Token actual =
                    lista.get(
                            indice
                    );

            if (actual.getChannel()
                    != Token.DEFAULT_CHANNEL
                    || actual.getType()
                    != PigLatinLexer.LEER) {

                continue;
            }

            Token siguiente =
                    siguienteTokenVisible(
                            lista,
                            indice + 1
                    );

            if (siguiente == null
                    || siguiente.getType()
                    == Token.EOF
                    || siguiente.getLine()
                    > actual.getLine()
                    || siguiente.getType()
                    == PigLatinLexer.PUNTO_COMA) {

                continue;
            }

            diagnosticos.add(
                    new Diagnostico(
                            TipoDiagnostico.SINTACTICO,
                            Severidad.ERROR,
                            archivo.ruta(),
                            Math.max(
                                    actual.getLine(),
                                    1
                            ),
                            Math.max(
                                    actual.getCharPositionInLine(),
                                    0
                            ),
                            "Despues de << solo puede aparecer ; o un salto de linea. "
                                    + "Para guardar la entrada use variable <<"
                    )
            );
        }
    }

    private Token siguienteTokenVisible(
            List<Token> tokens,
            int inicio
    ) {
        for (int indice = inicio;
             indice < tokens.size();
             indice++) {

            Token token =
                    tokens.get(
                            indice
                    );

            if (token.getChannel()
                    == Token.DEFAULT_CHANNEL) {

                return token;
            }
        }

        return null;
    }

    private void configurar(
            ArchivoFuente archivo,
            Lexer lexer,
            Parser parser,
            List<Diagnostico> diagnosticos
    ) {
        configurarLexer(
                archivo,
                lexer,
                diagnosticos
        );

        configurarParser(
                archivo,
                parser,
                diagnosticos
        );
    }

    private void configurarLexer(
            ArchivoFuente archivo,
            Lexer lexer,
            List<Diagnostico> diagnosticos
    ) {
        lexer.removeErrorListeners();

        lexer.addErrorListener(
                new RecolectorErroresAntlr(
                        archivo.ruta(),
                        TipoDiagnostico.LEXICO,
                        diagnosticos
                )
        );
    }

    private void configurarParser(
            ArchivoFuente archivo,
            Parser parser,
            List<Diagnostico> diagnosticos
    ) {
        parser.removeErrorListeners();

        parser.addErrorListener(
                new RecolectorErroresAntlr(
                        archivo.ruta(),
                        TipoDiagnostico.SINTACTICO,
                        diagnosticos
                )
        );
    }

    private boolean contieneErrores(
            List<Diagnostico> diagnosticos
    ) {
        return diagnosticos.stream()
                .anyMatch(Diagnostico::esError);
    }

    private void validarNombreClase(
            ArchivoFuente archivo,
            ProgramaAst ast,
            List<Diagnostico> diagnosticos
    ) {
        ZAst.Clase clase =
                ast.elementos()
                        .stream()
                        .filter(
                                ZAst.Clase.class::isInstance
                        )
                        .map(
                                ZAst.Clase.class::cast
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (clase == null) {
            return;
        }

        String esperado =
                clase.nombre()
                        + ".z";

        if (!archivo.nombre()
                .equals(esperado)) {

            diagnosticos.add(
                    new Diagnostico(
                            TipoDiagnostico.SEMANTICO,
                            Severidad.ERROR,
                            archivo.ruta(),
                            clase.posicion()
                                    .linea(),
                            clase.posicion()
                                    .columna(),
                            "El archivo debe llamarse "
                                    + esperado
                    )
            );
        }
    }
}