parser grammar ZetarianoParser;

options {
    tokenVocab = ZetarianoLexer;
}

archivo
    : definicionClase EOF
    ;

definicionClase
    : KW_PUBLIC? KW_CLASS IDENTIFICADOR
      herencia?
      LLAVE_ABRE
      miembroClase*
      LLAVE_CIERRA
    ;

herencia
    : KW_EXTENDS IDENTIFICADOR
    ;

miembroClase
    : constructor
    | metodo
    | atributo
    ;

atributo
    : modificadorAcceso?
      tipo
      IDENTIFICADOR
      (ASIGNACION inicializador)?
      PUNTO_COMA
    ;

constructor
    : modificadorAcceso?
      IDENTIFICADOR
      PARENTESIS_ABRE
      listaParametros?
      PARENTESIS_CIERRA
      bloque
    ;

metodo
    : KW_OVERRIDE?
      modificadorAcceso?
      tipoRetorno
      IDENTIFICADOR
      PARENTESIS_ABRE
      listaParametros?
      PARENTESIS_CIERRA
      bloque
    ;

modificadorAcceso
    : KW_PUBLIC
    | KW_PRIVATE
    | KW_PROTECTED
    ;

tipoRetorno
    : KW_VOID
    | tipo
    ;

tipo
    : tipoBase corchetesVacios*
    ;

tipoBase
    : TYPE_INT
    | TYPE_DOUBLE
    | TYPE_CHAR
    | TYPE_BOOLEAN
    | TYPE_STRING
    | IDENTIFICADOR
    ;

corchetesVacios
    : CORCHETE_ABRE CORCHETE_CIERRA
    ;

listaParametros
    : parametro (COMA parametro)*
    ;

parametro
    : tipo IDENTIFICADOR
    ;

bloque
    : LLAVE_ABRE sentencia* LLAVE_CIERRA
    ;

sentencia
    : bloque
    | declaracionLocal PUNTO_COMA
    | expresion PUNTO_COMA
    | sentenciaIf
    | sentenciaSwitch
    | sentenciaFor
    | sentenciaWhile
    | sentenciaDoWhile
    | sentenciaReturn
    | KW_BREAK PUNTO_COMA
    | KW_CONTINUE PUNTO_COMA
    | PUNTO_COMA
    ;

declaracionLocal
    : tipo IDENTIFICADOR
      (ASIGNACION inicializador)?
    ;

inicializador
    : expresion
    | inicializadorLista
    ;

inicializadorLista
    : LLAVE_ABRE
      (inicializador (COMA inicializador)*)?
      LLAVE_CIERRA
    ;

sentenciaIf
    : KW_IF
      PARENTESIS_ABRE
      expresion
      PARENTESIS_CIERRA
      sentencia
      (KW_ELSE sentencia)?
    ;

sentenciaSwitch
    : KW_SWITCH
      PARENTESIS_ABRE
      expresion
      PARENTESIS_CIERRA
      LLAVE_ABRE
      bloqueSwitch*
      LLAVE_CIERRA
    ;

bloqueSwitch
    : KW_CASE expresion DOS_PUNTOS sentencia*
    | KW_DEFAULT DOS_PUNTOS sentencia*
    ;

sentenciaFor
    : KW_FOR
      PARENTESIS_ABRE
      inicializacionFor?
      PUNTO_COMA
      expresion?
      PUNTO_COMA
      listaExpresiones?
      PARENTESIS_CIERRA
      sentencia
    ;

inicializacionFor
    : declaracionLocal
    | listaExpresiones
    ;

listaExpresiones
    : expresion (COMA expresion)*
    ;

sentenciaWhile
    : KW_WHILE
      PARENTESIS_ABRE
      expresion
      PARENTESIS_CIERRA
      sentencia
    ;

sentenciaDoWhile
    : KW_DO
      sentencia
      KW_WHILE
      PARENTESIS_ABRE
      expresion
      PARENTESIS_CIERRA
      PUNTO_COMA
    ;

sentenciaReturn
    : KW_RETURN expresion? PUNTO_COMA
    ;

expresion
    : expresionAsignacion
    ;

expresionAsignacion
    : expresionTernaria
      (operadorAsignacion expresionAsignacion)?
    ;

operadorAsignacion
    : ASIGNACION
    | SUMA_ASIG
    | RESTA_ASIG
    | MULT_ASIG
    | DIV_ASIG
    ;

expresionTernaria
    : expresionOr
      (
        TERNARIO
        expresion
        DOS_PUNTOS
        expresionTernaria
      )?
    ;

expresionOr
    : expresionAnd
      (OR expresionAnd)*
    ;

expresionAnd
    : expresionIgualdad
      (AND expresionIgualdad)*
    ;

expresionIgualdad
    : expresionRelacional
      (
        (IGUALDAD | DIFERENTE)
        expresionRelacional
      )*
    ;

expresionRelacional
    : expresionAditiva
      (
        (
            MENOR
          | MAYOR
          | MENOR_IGUAL
          | MAYOR_IGUAL
        )
        expresionAditiva
      )*
    ;

expresionAditiva
    : expresionMultiplicativa
      (
        (SUMA | RESTA)
        expresionMultiplicativa
      )*
    ;

expresionMultiplicativa
    : expresionUnaria
      (
        (
            MULTIPLICACION
          | DIVISION
          | MODULO
        )
        expresionUnaria
      )*
    ;

expresionUnaria
    : (
          NEGACION
        | SUMA
        | RESTA
        | INCREMENTO
        | DECREMENTO
      )
      expresionUnaria

    | expresionPostfija
    ;

expresionPostfija
    : primaria
      sufijoPostfijo*
      (INCREMENTO | DECREMENTO)?
    ;

sufijoPostfijo
    : PARENTESIS_ABRE
      listaArgumentos?
      PARENTESIS_CIERRA

    | CORCHETE_ABRE
      expresion
      CORCHETE_CIERRA

    | PUNTO
      miembroAcceso
    ;

miembroAcceso
    : IDENTIFICADOR
    | KW_PRINT
    | KW_PRINTLN
    | KW_READLN
    ;

primaria
    : ENTERO
    | DECIMAL
    | CADENA
    | CARACTER
    | BOOL_TRUE
    | BOOL_FALSE
    | NULL_VALUE
    | KW_THIS
    | KW_SUPER
    | IDENTIFICADOR
    | KW_PRINT
    | KW_PRINTLN
    | KW_READLN
    | creacion
    | PARENTESIS_ABRE expresion PARENTESIS_CIERRA
    ;

creacion
    : KW_NEW tipoBase
      (
          argumentosConstructor
        | dimensionCreacion+
      )
    ;

argumentosConstructor
    : PARENTESIS_ABRE
      listaArgumentos?
      PARENTESIS_CIERRA
    ;

dimensionCreacion
    : CORCHETE_ABRE
      expresion
      CORCHETE_CIERRA
    ;

listaArgumentos
    : expresion (COMA expresion)*
    ;

