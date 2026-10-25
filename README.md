# Proyecto 2 - St4ck Over-Pig

## Descripción

Este proyecto fue desarrollado para el curso de Organización de Lenguajes y Compiladores 2 de la Universidad de San Carlos de Guatemala, Centro Universitario de Occidente (CUNOC).

Consiste en un compilador que permite trabajar con tres lenguajes de programación: Y?, Zetariano y PigLatin.

El programa cuenta con una interfaz gráfica en la que se pueden abrir carpetas con archivos de los diferentes lenguajes, realizar los análisis correspondientes y generar código intermedio mediante cuartetas y código de tres direcciones (C3D).

También permite generar código en C y ensamblador RISC-V, el cual puede ejecutarse utilizando el simulador RARS.

## Tecnologías utilizadas

- Java 21
- ANTLR4
- Apache Maven
- Java Swing
- JUnit 5
- RARS (simulador RISC-V)
- IntelliJ IDEA

## Funcionalidades principales

El compilador cuenta con las siguientes funcionalidades:

- Manejo de archivos y carpetas desde la interfaz gráfica.
- Reconocimiento de los lenguajes Y?, Zetariano y PigLatin.
- Análisis léxico, sintáctico y semántico.
- Detección de errores con línea, columna y descripción.
- Coloreado de palabras reservadas y símbolos.
- Construcción de árboles de sintaxis abstracta (AST).
- Manejo de tablas de símbolos y tipos de datos.
- Soporte para funciones, arreglos y estructuras.
- Soporte para clases, herencia, encapsulamiento y polimorfismo.
- Generación de cuartetas.
- Manejo de memoria mediante Stack y Heap.
- Generación de código de tres direcciones.
- Generación de código C.
- Generación de ensamblador RISC-V.

## Requisitos

Para compilar y ejecutar el proyecto es necesario tener instalado:

- JDK 21
- Apache Maven
- IntelliJ IDEA u otro IDE compatible con Java

Para ejecutar el código RISC-V generado, se utiliza RARS.

## Instalación y ejecución

**1. Clonar el repositorio**

```bash
git clone https://github.com/MaYan711/Proyecto-2-St4ck-Over-Pig.git
```

**2. Ingresar a la carpeta del proyecto**

```bash
cd Proyecto-2-St4ck-Over-Pig
cd "st4ck over-pig"
```

**3. Compilar y ejecutar las pruebas**

```bash
mvn clean test
```

**4. Iniciar la aplicación**

```bash
mvn exec:java
```

Al ejecutar el último comando se abrirá la interfaz gráfica del compilador.

## Uso del compilador

Para utilizar el programa se debe abrir una carpeta que contenga los archivos fuente de los lenguajes soportados.

Después, desde el menú Compilación, se selecciona la opción para analizar el proyecto.

Si existen errores, estos se muestran en la pestaña Diagnósticos. Si el análisis se completa correctamente, se pueden consultar las cuartetas, la representación de memoria, el código C3D, el código C y el ensamblador RISC-V.

El código generado también puede guardarse para realizar pruebas externas.

## Ejecución de código RISC-V

Para ejecutar el ensamblador generado:

1. Analizar un proyecto válido desde la interfaz.
2. Seleccionar la pestaña Código RISC-V.
3. Guardar el código generado con extensión `.s`.
4. Abrir una terminal en la carpeta donde se guardó el archivo.
5. Ejecutarlo utilizando RARS.

Ejemplo:

```powershell
java -jar rars1_6.jar nc programa.s
```

El archivo `rars1_6.jar` debe estar en la carpeta actual o se debe indicar su ruta completa.

## Pruebas realizadas

Durante el desarrollo se realizaron pruebas para comprobar el funcionamiento de los tres lenguajes y de las diferentes etapas del compilador.

Se utilizaron pruebas automáticas con JUnit, pruebas de generación de código y pruebas de ejecución en RARS.

En la última validación se ejecutaron 257 pruebas automáticas, todas completadas correctamente.

También se utilizó un banco de pruebas proporcionado por la auxiliar, con el cual se comprobó el funcionamiento de clases, herencia, polimorfismo, arreglos, operaciones y generación de ensamblador RISC-V.

## Documentación

El proyecto cuenta con la siguiente documentación:

- Manual de usuario.
- Manual técnico.
- Diagrama de clases.

## Autor

**Mario Raúl Yancor Ocaña**

Carné: 201930761

Universidad de San Carlos de Guatemala

Centro Universitario de Occidente - CUNOC

Organización de Lenguajes y Compiladores 2

2026
