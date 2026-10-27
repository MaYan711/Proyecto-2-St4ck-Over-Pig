package com.compi2.contacto.analisis;

import com.compi2.contacto.antlr.y.YLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class TokenizadorIndentacionY implements TokenSource {

    private static final int ANCHO_TABULACION = 4;

    private final TokenSource origen;
    private final List<Token> tokens;
    private int indiceActual;

    public TokenizadorIndentacionY(TokenSource origen) {
        this.origen = Objects.requireNonNull(
                origen,
                "El lexer de Y? es obligatorio"
        );
        this.tokens = transformar(leerTokens());
    }

    public List<Token> erroresIndentacion() {
        return tokens.stream()
                .filter(token -> token.getType() == YLexer.ERROR_INDENTACION)
                .toList();
    }

    @Override
    public Token nextToken() {
        if (indiceActual >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }

        return tokens.get(indiceActual++);
    }

    @Override
    public int getLine() {
        return origen.getLine();
    }

    @Override
    public int getCharPositionInLine() {
        return origen.getCharPositionInLine();
    }

    @Override
    public CharStream getInputStream() {
        return origen.getInputStream();
    }

    @Override
    public String getSourceName() {
        return origen.getSourceName();
    }

    @Override
    public void setTokenFactory(TokenFactory<?> fabrica) {
        origen.setTokenFactory(fabrica);
    }

    @Override
    public TokenFactory<?> getTokenFactory() {
        return origen.getTokenFactory();
    }

    private List<Token> leerTokens() {
        List<Token> resultado = new ArrayList<>();
        Token token;

        do {
            token = origen.nextToken();
            resultado.add(token);
        } while (token.getType() != Token.EOF);

        return resultado;
    }

    private List<Token> transformar(List<Token> originales) {
        List<Token> resultado = new ArrayList<>();
        Deque<Integer> indentaciones = new ArrayDeque<>();

        indentaciones.push(0);

        int delimitadoresAbiertos = 0;
        Token eof = originales.get(originales.size() - 1);

        for (int indice = 0; indice < originales.size() - 1; indice++) {
            Token token = originales.get(indice);
            int tipo = token.getType();

            if (tipo == YLexer.NUEVA_LINEA) {
                if (delimitadoresAbiertos > 0
                        || lineaVaciaOComentario(originales, indice + 1)) {
                    continue;
                }

                resultado.add(crearToken(
                        token,
                        YLexer.NUEVA_LINEA,
                        "\n"
                ));

                int nuevaIndentacion = calcularIndentacion(
                        token.getText()
                );

                agregarCambiosIndentacion(
                        resultado,
                        indentaciones,
                        nuevaIndentacion,
                        token
                );

                continue;
            }

            resultado.add(token);

            delimitadoresAbiertos = actualizarDelimitadores(
                    delimitadoresAbiertos,
                    tipo
            );
        }

        cerrarArchivo(
                resultado,
                indentaciones,
                eof
        );

        return List.copyOf(resultado);
    }

    private boolean lineaVaciaOComentario(
            List<Token> originales,
            int inicio
    ) {
        for (int indice = inicio; indice < originales.size(); indice++) {
            Token token = originales.get(indice);

            if (token.getType() == Token.EOF
                    || token.getType() == YLexer.NUEVA_LINEA) {
                return true;
            }

            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                return false;
            }
        }

        return true;
    }

    private int calcularIndentacion(String textoNuevaLinea) {
        int ultimoSalto = Math.max(
                textoNuevaLinea.lastIndexOf('\n'),
                textoNuevaLinea.lastIndexOf('\r')
        );

        String espacios = textoNuevaLinea.substring(
                ultimoSalto + 1
        );

        int columnas = 0;

        for (int indice = 0; indice < espacios.length(); indice++) {
            char caracter = espacios.charAt(indice);

            if (caracter == '\t') {
                columnas += ANCHO_TABULACION
                        - columnas % ANCHO_TABULACION;
            } else {
                columnas++;
            }
        }

        return columnas;
    }

    private void agregarCambiosIndentacion(
            List<Token> resultado,
            Deque<Integer> indentaciones,
            int nuevaIndentacion,
            Token referencia
    ) {
        int indentacionActual = indentaciones.peek();

        if (nuevaIndentacion > indentacionActual) {
            indentaciones.push(nuevaIndentacion);

            resultado.add(crearTokenIndentacion(
                    referencia,
                    YLexer.INDENT,
                    "<INDENT>"
            ));

            return;
        }

        while (nuevaIndentacion < indentaciones.peek()
                && indentaciones.size() > 1) {

            indentaciones.pop();

            resultado.add(crearTokenIndentacion(
                    referencia,
                    YLexer.DEDENT,
                    "<DEDENT>"
            ));
        }

        if (nuevaIndentacion != indentaciones.peek()) {
            resultado.add(crearTokenIndentacion(
                    referencia,
                    YLexer.ERROR_INDENTACION,
                    "Indentacion no coincide con ningun bloque anterior"
            ));
        }
    }

    private int actualizarDelimitadores(
            int abiertos,
            int tipo
    ) {
        if (tipo == YLexer.PARENTESIS_ABRE
                || tipo == YLexer.CORCHETE_ABRE
                || tipo == YLexer.LLAVE_ABRE) {

            return abiertos + 1;
        }

        if (tipo == YLexer.PARENTESIS_CIERRA
                || tipo == YLexer.CORCHETE_CIERRA
                || tipo == YLexer.LLAVE_CIERRA) {

            return Math.max(
                    0,
                    abiertos - 1
            );
        }

        return abiertos;
    }

    private void cerrarArchivo(
            List<Token> resultado,
            Deque<Integer> indentaciones,
            Token eof
    ) {
        Token ultimo = ultimoTokenPrincipal(resultado);

        if (ultimo != null
                && ultimo.getType() != YLexer.NUEVA_LINEA
                && ultimo.getType() != YLexer.DEDENT) {

            resultado.add(crearToken(
                    eof,
                    YLexer.NUEVA_LINEA,
                    "\n"
            ));
        }

        while (indentaciones.size() > 1) {
            indentaciones.pop();

            resultado.add(crearTokenIndentacion(
                    eof,
                    YLexer.DEDENT,
                    "<DEDENT>"
            ));
        }

        resultado.add(eof);
    }

    private Token ultimoTokenPrincipal(
            List<Token> resultado
    ) {
        for (int indice = resultado.size() - 1;
             indice >= 0;
             indice--) {

            Token token = resultado.get(indice);

            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                return token;
            }
        }

        return null;
    }

    private CommonToken crearToken(
            Token referencia,
            int tipo,
            String texto
    ) {
        CommonToken token = new CommonToken(referencia);

        token.setType(tipo);
        token.setText(texto);
        token.setChannel(Token.DEFAULT_CHANNEL);

        return token;
    }

    private Token crearTokenIndentacion(
            Token referencia,
            int tipo,
            String texto
    ) {
        CommonToken token = crearToken(
                referencia,
                tipo,
                texto
        );

        if (referencia.getType() == YLexer.NUEVA_LINEA) {
            token.setLine(
                    referencia.getLine() + 1
            );

            token.setCharPositionInLine(0);
        }

        if (tipo == YLexer.ERROR_INDENTACION) {
            token.setChannel(Token.HIDDEN_CHANNEL);
        }

        return token;
    }
}