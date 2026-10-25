lexer grammar ZetarianoLexer;

KW_PUBLIC    : 'public';
KW_PRIVATE   : 'private';
KW_PROTECTED : 'protected';
KW_OVERRIDE  : '@Override';
KW_EXTENDS   : 'extends';
KW_THIS      : 'this';
KW_SUPER     : 'super';
KW_CLASS     : 'class';
KW_VOID      : 'void';
KW_NEW       : 'new';
KW_RETURN    : 'return';
KW_IF        : 'if';
KW_ELSE      : 'else';
KW_SWITCH    : 'switch';
KW_CASE      : 'case';
KW_DEFAULT   : 'default';
KW_BREAK     : 'break';
KW_CONTINUE  : 'continue';
KW_FOR       : 'for';
KW_WHILE     : 'while';
KW_DO        : 'do';
KW_PRINT     : 'print';
KW_PRINTLN   : 'println';
KW_READLN    : 'readln';

TYPE_INT     : 'int';
TYPE_DOUBLE  : 'double';
TYPE_CHAR    : 'char';
TYPE_BOOLEAN : 'boolean';
TYPE_STRING  : 'String';

BOOL_TRUE  : 'true';
BOOL_FALSE : 'false';
NULL_VALUE : 'null';

INCREMENTO     : '++';
DECREMENTO     : '--';
SUMA_ASIG      : '+=';
RESTA_ASIG     : '-=';
MULT_ASIG      : '*=';
DIV_ASIG       : '/=';
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
MODULO         : '%';
NEGACION       : '!';
MENOR          : '<';
MAYOR          : '>';
TERNARIO       : '?';

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
ESPACIO          : [ \t\r\n]+ -> channel(HIDDEN);

fragment ESCAPE: '\\' .;

