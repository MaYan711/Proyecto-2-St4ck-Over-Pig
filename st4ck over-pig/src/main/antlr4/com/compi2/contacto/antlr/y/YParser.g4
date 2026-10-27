parser grammar YParser;

options {
    tokenVocab = YLexer;
}

archivo
    : NUEVA_LINEA*
      seccionEstructuras?
      seccionFunciones
      NUEVA_LINEA*
      EOF
    ;

seccionEstructuras
    : SECCION_ESTRUCTURAS finLinea definicionEstructura*
    ;

seccionFunciones
    : SECCION_FUNCIONES finLinea definicionFuncion*
    ;

definicionEstructura
    : KW_ESTRUCTURA IDENTIFICADOR DOS_PUNTOS finLinea
      INDENT atributoEstructura+ DEDENT
    ;

atributoEstructura
    : tipoDato IDENTIFICADOR dimensionArreglo* finSentencia
    ;

dimensionArreglo
    : CORCHETE_ABRE expresion CORCHETE_CIERRA
    ;


definicionFuncion
    : KW_DEFINIR IDENTIFICADOR
      PARENTESIS_ABRE listaParametros? PARENTESIS_CIERRA
      retornoFuncion?
      DOS_PUNTOS finLinea
      bloque
    ;

retornoFuncion
    : FLECHA tipoDato
    ;

listaParametros
    : parametro (COMA parametro)*
    ;

parametro
    : parametroArreglo
    | parametroEstructura
    | parametroValor
    ;

parametroValor
    : tipoDato IDENTIFICADOR
    ;

parametroArreglo
    : CORCHETE_ABRE CORCHETE_CIERRA tipoDato IDENTIFICADOR
    ;

parametroEstructura
    : LLAVE_ABRE LLAVE_CIERRA tipoDato IDENTIFICADOR
    ;

bloque
    : INDENT sentencia+ DEDENT
    ;

sentencia
    : definicionEstructura
    | sentenciaSi
    | sentenciaElegir
    | sentenciaPara
    | sentenciaMientras
    | sentenciaHacerMientras
    | sentenciaSimple
    ;

sentenciaSimple
    : (
        declaracionVariable
      | asignacion
      | incrementoDecremento
      | llamadaFuncion
      | llamadaLeer
      | sentenciaImprimir
      | sentenciaRetornar
      | KW_ROMPER
      | KW_CONTINUAR
      ) finSentencia
    ;

declaracionVariable
    : tipoDato IDENTIFICADOR dimensionArreglo* (ASIGNACION inicializador)?
    ;

asignacion
    : accesoAsignable ASIGNACION inicializador
    ;

incrementoDecremento
    : accesoAsignable (INCREMENTO | DECREMENTO)
    ;

accesoAsignable
    : IDENTIFICADOR sufijoAsignable*
    ;

sufijoAsignable
    : CORCHETE_ABRE expresion CORCHETE_CIERRA
    | PUNTO IDENTIFICADOR
    ;

inicializador
    : expresion
    | inicializadorCompuesto
    ;

inicializadorCompuesto
    : LLAVE_ABRE
      (inicializador (COMA inicializador)*)?
      LLAVE_CIERRA
    ;

sentenciaSi
    : KW_SI PARENTESIS_ABRE expresion PARENTESIS_CIERRA KW_ENTONCES
      finLinea bloque
      ramaSino*
      ramaContrario?
    ;

ramaSino
    : KW_SINO PARENTESIS_ABRE expresion PARENTESIS_CIERRA KW_ENTONCES
      finLinea bloque
    ;

ramaContrario
    : KW_CONTRARIO finLinea bloque
    ;

sentenciaElegir
    : KW_ELEGIR PARENTESIS_ABRE expresion PARENTESIS_CIERRA DOS_PUNTOS
      finLinea
      INDENT
      (ramaCaso+ ramaSiempre? | ramaSiempre)
      DEDENT
    ;

ramaCaso
    : KW_CASO expresion DOS_PUNTOS finLinea bloque
    ;

ramaSiempre
    : KW_SIEMPRE DOS_PUNTOS finLinea bloque
    ;

sentenciaPara
    : KW_PARA PARENTESIS_ABRE
      inicializacionPara PUNTO_COMA
      expresion PUNTO_COMA
      actualizacionPara
      PARENTESIS_CIERRA DOS_PUNTOS
      finLinea bloque
    ;

inicializacionPara
    : declaracionVariable
    | asignacion
    ;

actualizacionPara
    : incrementoDecremento
    | asignacion
    | llamadaFuncion
    ;

sentenciaMientras
    : KW_MIENTRAS PARENTESIS_ABRE expresion PARENTESIS_CIERRA KW_HACER
      finLinea bloque
    ;

sentenciaHacerMientras
    : KW_HACER DOS_PUNTOS finLinea bloque
      KW_MIENTRAS PARENTESIS_ABRE expresion PARENTESIS_CIERRA
      finSentencia
    ;

sentenciaImprimir
    : KW_IMPRIMIR PARENTESIS_ABRE expresion PARENTESIS_CIERRA
    ;

llamadaLeer
    : KW_LEER PARENTESIS_ABRE PARENTESIS_CIERRA
    ;

sentenciaRetornar
    : KW_RETORNAR expresion
    ;

llamadaFuncion
    : IDENTIFICADOR PARENTESIS_ABRE listaArgumentos? PARENTESIS_CIERRA
    ;

listaArgumentos
    : expresion (COMA expresion)*
    ;

expresion
    : expresionOr
    ;

expresionOr
    : expresionAnd (OR expresionAnd)*
    ;

expresionAnd
    : expresionIgualdad (AND expresionIgualdad)*
    ;

expresionIgualdad
    : expresionRelacional ((IGUALDAD | DIFERENTE) expresionRelacional)*
    ;

expresionRelacional
    : expresionAditiva
      ((MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL) expresionAditiva)*
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
    : (NEGACION | SUMA | RESTA) expresionUnaria
    | expresionPostfija
    ;

expresionPostfija
    : primaria sufijoPostfijo* (INCREMENTO | DECREMENTO)?
    ;

sufijoPostfijo
    : PARENTESIS_ABRE listaArgumentos? PARENTESIS_CIERRA
    | CORCHETE_ABRE expresion CORCHETE_CIERRA
    | PUNTO IDENTIFICADOR
    ;

primaria
    : ENTERO
    | DECIMAL
    | CADENA
    | CARACTER
    | BOOL_VERDADERO
    | BOOL_FALSO
    | KW_LEER PARENTESIS_ABRE PARENTESIS_CIERRA
    | IDENTIFICADOR
    | PARENTESIS_ABRE expresion PARENTESIS_CIERRA
    ;

tipoDato
    : TYPE_ENTERO
    | TYPE_FLOTANTE
    | TYPE_CADENA
    | TYPE_CARACTER
    | TYPE_BOOL
    | IDENTIFICADOR
    ;

finLinea
    : NUEVA_LINEA
    ;

finSentencia
    : PUNTO_COMA? NUEVA_LINEA
    ;