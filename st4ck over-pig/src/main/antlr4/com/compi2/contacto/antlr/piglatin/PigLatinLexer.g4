lexer grammar PigLatinLexer;

SECCION_VARIABLES : 'VARIABILES>';
SECCION_PRINCIPAL : 'MAIOR>';
FIN_PROGRAMA      : 'FINIS;';

KW_IMPORT      : 'import';
KW_ESTO        : 'esto';
KW_SERIES      : 'series';
KW_NOVUS       : 'novus';
KW_NULL        : 'null';
KW_SI          : 'si';
KW_ALITER      : 'aliter';
KW_FINIS       : 'finis';
KW_DUM         : 'dum';
KW_FACERE      : 'facere';
KW_PER         : 'per';
KW_PERGE       : 'perge';
KW_INTERRUMPE  : 'interrumpe';

TYPE_NUMERUS   : 'numerus';
TYPE_DECIMALIS : 'decimalis';
TYPE_TEXTUM    : 'textum';
TYPE_LITTERA   : 'littera';
TYPE_BOOL      : 'bool';

BOOL_VERUM  : 'verum';
BOOL_FALSUS : 'falsus';

LEER           : '<<';
IMPRIMIR       : '>>';
INCREMENTO     : '++';
DECREMENTO     : '--';
IGUALDAD       : '==';
DIFERENTE      : '!=';
MENOR_IGUAL    : '<=';
MAYOR_IGUAL    : '>=';
AND            : '&&';
OR             : '||';
ASIGNACION     : '=';
SUMA           : '+';
RESTA          : '-';
MULTIPLICACION : '*';
DIVISION       : '/';
NEGACION       : '!';
MENOR          : '<';
MAYOR          : '>';

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
COMENTARIO_HASH  : '##' .*? '##' -> channel(HIDDEN);
ESPACIO          : [ \t\r\n]+ -> channel(HIDDEN);

fragment ESCAPE: '\\' .;