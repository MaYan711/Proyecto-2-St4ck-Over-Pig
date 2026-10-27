lexer grammar YLexer;

tokens {
    INDENT,
    DEDENT,
    ERROR_INDENTACION
}

SECCION_ESTRUCTURAS : '%estructuras';
SECCION_FUNCIONES   : '%funciones';

KW_ESTRUCTURA : 'estructura';
KW_DEFINIR    : 'definir';
KW_RETORNAR   : 'retornar';
KW_SI         : 'si';
KW_ENTONCES   : 'entonces';
KW_SINO       : 'sino';
KW_CONTRARIO  : 'contrario';
KW_ELEGIR     : 'elegir';
KW_CASO       : 'caso';
KW_SIEMPRE    : 'siempre';
KW_ROMPER     : 'romper';
KW_CONTINUAR  : 'continuar';
KW_PARA       : 'para';
KW_MIENTRAS   : 'mientras';
KW_HACER      : 'hacer';
KW_IMPRIMIR   : 'imprimir';
KW_LEER       : 'leer';

TYPE_ENTERO   : 'entero';
TYPE_FLOTANTE : 'flotante';
TYPE_CADENA   : 'cadena';
TYPE_CARACTER : 'caracter';
TYPE_BOOL     : 'bool';

BOOL_VERDADERO : 'verdadero';
BOOL_FALSO     : 'falso';

FLECHA       : '->';
INCREMENTO   : '++';
DECREMENTO   : '--';
IGUALDAD     : '==';
DIFERENTE    : '!=';
MENOR_IGUAL  : '<=';
MAYOR_IGUAL  : '>=';
AND          : '&&';
OR           : '||';
ASIGNACION   : '=';
SUMA         : '+';
RESTA        : '-';
MULTIPLICACION : '*';
DIVISION     : '/';
NEGACION     : '!';
MENOR        : '<';
MAYOR        : '>';

DOS_PUNTOS       : ':';
PUNTO_COMA       : ';';
COMA             : ',';
PUNTO            : '.';
PARENTESIS_ABRE  : '(';
PARENTESIS_CIERRA: ')';
CORCHETE_ABRE    : '[';
CORCHETE_CIERRA  : ']';
LLAVE_ABRE       : '{';
LLAVE_CIERRA     : '}';

DECIMAL : [0-9]+ '.' [0-9]+;
ENTERO  : [0-9]+;
CADENA  : '"' (ESCAPE | ~["\\\r\n])* '"';
CARACTER: '\'' (ESCAPE | ~['\\\r\n]) '\'';
IDENTIFICADOR: [a-zA-Z_] [a-zA-Z0-9_]*;

COMENTARIO_LINEA : '//' ~[\r\n]* -> channel(HIDDEN);
COMENTARIO_BLOQUE: '/*' .*? '*/' -> channel(HIDDEN);
NUEVA_LINEA      : '\r'? '\n' [ \t]*;
ESPACIO          : [ \t]+ -> channel(HIDDEN);

fragment ESCAPE: '\\' .;
