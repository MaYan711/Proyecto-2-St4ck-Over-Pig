parser grammar PigLatinParser;

options {
    tokenVocab = PigLatinLexer;
}

archivo
    : importacion*
      seccionVariables?
      seccionPrincipal
      EOF
    ;

importacion
    : KW_IMPORT rutaImportacion PUNTO_COMA?
    ;

rutaImportacion
    : IDENTIFICADOR
      (PUNTO IDENTIFICADOR)*
      PUNTO extensionImportacion
    ;

extensionImportacion
    : IDENTIFICADOR
    ;

seccionVariables
    : SECCION_VARIABLES declaracionGlobal*
    ;

seccionPrincipal
    : SECCION_PRINCIPAL sentencia* FIN_PROGRAMA
    ;

declaracionGlobal
    : declaracionVariable
    | declaracionArreglo
    ;

declaracionVariable
    : KW_ESTO IDENTIFICADOR DOS_PUNTOS definicionVariable PUNTO_COMA
    ;

definicionVariable
    : tipoDato expresion?
    | BOOL_VERUM
    | BOOL_FALSUS
    | KW_NOVUS IDENTIFICADOR argumentos (DOS_PUNTOS IDENTIFICADOR)?
    | KW_NULL DOS_PUNTOS IDENTIFICADOR
    | IDENTIFICADOR inicializadorCompuesto?
    ;

declaracionArreglo
    : KW_SERIES IDENTIFICADOR dimensionDeclaracion+
      DOS_PUNTOS tipoDatoArreglo
      inicializadorCompuesto?
      PUNTO_COMA
    ;

dimensionDeclaracion
    : CORCHETE_ABRE expresion CORCHETE_CIERRA
    ;

tipoDatoArreglo
    : tipoDato
    | IDENTIFICADOR
    ;

sentencia
    : declaracionVariable
    | declaracionArreglo
    | sentenciaSi
    | sentenciaMientras
    | sentenciaHacerMientras
    | sentenciaPara
    | sentenciaEntrada
    | sentenciaSalida
    | sentenciaInterrupcion
    | sentenciaExpresion
    ;

sentenciaExpresion
    : expresion PUNTO_COMA
    ;

sentenciaEntrada
    : accesoAsignable LEER PUNTO_COMA?
    | LEER accesoAsignable? PUNTO_COMA?
    ;

sentenciaSalida
    : IMPRIMIR expresion
      (IMPRIMIR expresion)*
      PUNTO_COMA?
    ;

sentenciaInterrupcion
    : (KW_PERGE | KW_INTERRUMPE) PUNTO_COMA?
    ;

sentenciaSi
    : KW_SI
      PARENTESIS_ABRE expresion PARENTESIS_CIERRA
      bloque
      ramaAliter*
      KW_FINIS
      PUNTO_COMA?
    ;

ramaAliter
    : KW_ALITER
      (
          PARENTESIS_ABRE expresion PARENTESIS_CIERRA
      )?
      bloque
    ;

sentenciaMientras
    : KW_DUM
      PARENTESIS_ABRE expresion PARENTESIS_CIERRA
      bloque
      KW_FINIS
      PUNTO_COMA?
    ;

sentenciaHacerMientras
    : KW_FACERE
      bloque
      KW_DUM
      PARENTESIS_ABRE expresion PARENTESIS_CIERRA
      PUNTO_COMA
    ;

sentenciaPara
    : KW_PER
      PARENTESIS_ABRE
      inicializacionPara?
      PUNTO_COMA
      condicion=expresion?
      PUNTO_COMA
      actualizacion=expresion?
      PARENTESIS_CIERRA
      bloque
    ;

inicializacionPara
    : declaracionPara
    | expresion
    ;

declaracionPara
    : KW_ESTO IDENTIFICADOR DOS_PUNTOS definicionVariableSinFin
    ;

definicionVariableSinFin
    : tipoDato expresion?
    | BOOL_VERUM
    | BOOL_FALSUS
    | KW_NOVUS IDENTIFICADOR argumentos (DOS_PUNTOS IDENTIFICADOR)?
    | KW_NULL DOS_PUNTOS IDENTIFICADOR
    | IDENTIFICADOR inicializadorCompuesto?
    ;

bloque
    : LLAVE_ABRE sentencia* LLAVE_CIERRA
    ;

inicializadorCompuesto
    : LLAVE_ABRE
      (inicializador (COMA inicializador)*)?
      LLAVE_CIERRA
    ;

inicializador
    : expresion
    | inicializadorCompuesto
    ;

expresion
    : expresionAsignacion
    ;

expresionAsignacion
    : expresionOr
      (ASIGNACION expresionAsignacion)?
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
      ((IGUALDAD | DIFERENTE) expresionRelacional)*
    ;

expresionRelacional
    : expresionAditiva
      (
          (MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL)
          expresionAditiva
      )*
    ;

expresionAditiva
    : expresionMultiplicativa
      ((SUMA | RESTA) expresionMultiplicativa)*
    ;

expresionMultiplicativa
    : expresionUnaria
      ((MULTIPLICACION | DIVISION) expresionUnaria)*
    ;

expresionUnaria
    : (NEGACION | SUMA | RESTA | INCREMENTO | DECREMENTO)
      expresionUnaria
    | expresionPostfija
    ;

expresionPostfija
    : primaria
      sufijoPostfijo*
      (INCREMENTO | DECREMENTO)?
    ;

sufijoPostfijo
    : argumentos
    | CORCHETE_ABRE expresion CORCHETE_CIERRA
    | PUNTO IDENTIFICADOR
    ;

primaria
    : ENTERO
    | DECIMAL
    | CADENA
    | CARACTER
    | BOOL_VERUM
    | BOOL_FALSUS
    | KW_NULL
    | IDENTIFICADOR
    | creacionObjeto
    | PARENTESIS_ABRE expresion PARENTESIS_CIERRA
    ;

creacionObjeto
    : KW_NOVUS IDENTIFICADOR argumentos
    ;

argumentos
    : PARENTESIS_ABRE listaArgumentos? PARENTESIS_CIERRA
    ;

listaArgumentos
    : expresion (COMA expresion)*
    ;

accesoAsignable
    : IDENTIFICADOR sufijoAsignable*
    ;

sufijoAsignable
    : CORCHETE_ABRE expresion CORCHETE_CIERRA
    | PUNTO IDENTIFICADOR
    ;

tipoDato
    : TYPE_NUMERUS
    | TYPE_DECIMALIS
    | TYPE_TEXTUM
    | TYPE_LITTERA
    | TYPE_BOOL
    ;