package com.compi2.contacto.c3d;

import com.compi2.contacto.ir.Cuarteta;
import com.compi2.contacto.ir.OperadorCuarteta;
import com.compi2.contacto.ir.ProgramaIntermedio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeneradorRiscV {
    private static final Pattern REFERENCIA = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern DESPLAZAMIENTO = Pattern.compile("(.+?)([+-])([0-9]+)");
    private static final Pattern SALTO_CONDICIONAL = Pattern.compile(
            "^([a-z]+)\\s+(.+),\\s*([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Map<String, String> CONDICIONES_INVERSAS = Map.ofEntries(
            Map.entry("beq", "bne"),
            Map.entry("bne", "beq"),
            Map.entry("blt", "bge"),
            Map.entry("bge", "blt"),
            Map.entry("bltu", "bgeu"),
            Map.entry("bgeu", "bltu"),
            Map.entry("beqz", "bnez"),
            Map.entry("bnez", "beqz"),
            Map.entry("bltz", "bgez"),
            Map.entry("bgez", "bltz"),
            Map.entry("bgtz", "blez"),
            Map.entry("blez", "bgtz"),
            Map.entry("bgt", "ble"),
            Map.entry("ble", "bgt"),
            Map.entry("bgtu", "bleu"),
            Map.entry("bleu", "bgtu"));
    private final Map<String, String> funciones = new LinkedHashMap<>();
    private final Map<String, String> temporales = new LinkedHashMap<>();
    private final Map<String, String> etiquetas = new LinkedHashMap<>();
    private StringBuilder codigo;
    private String funcion;
    private int indiceFuncion;
    private int indiceInterno;
    private int tamanoMarco;
    private int siguienteSaltoCorto;

    public String generar(ProgramaIntermedio programa) {
        Objects.requireNonNull(programa, "El programa intermedio es obligatorio");
        Map<String, List<Cuarteta>> cuerpos = new LinkedHashMap<>();
        String actual = null;
        for (Cuarteta c : programa.cuartetas()) {
            if (c.operador() == OperadorCuarteta.INICIO_FUNCION) {
                actual = c.argumento1();
                if (cuerpos.put(actual, new ArrayList<>()) != null) {
                    throw new IllegalArgumentException("Funcion duplicada: " + actual);
                }
            } else if (c.operador() == OperadorCuarteta.FIN_FUNCION) {
                actual = null;
            } else {
                if (actual == null) {
                    throw new IllegalArgumentException("Cuarteta sin funcion: " + c.operador());
                }
                cuerpos.get(actual).add(c);
            }
        }
        if (!cuerpos.containsKey("MAIOR")) {
            throw new IllegalArgumentException("No existe la funcion principal MAIOR");
        }
        funciones.clear();
        siguienteSaltoCorto = 0;
        int contador = 0;
        for (String nombre : cuerpos.keySet()) {
            funciones.put(nombre, "rv_fn_" + contador++);
        }
        codigo = new StringBuilder();
        linea(".data");
        linea(".align 2");
        linea("rv_stack: .space 400000");
        linea("rv_heap: .space 400000");
        linea("rv_stack_decimal: .space 400000");
        linea("rv_heap_decimal: .space 400000");
        linea("rv_stack_decimal_alto: .space 400000");
        linea("rv_heap_decimal_alto: .space 400000");
        linea(".align 3");
        linea("rv_decimal_aux: .space 16");
        linea("rv_tipo: .space 400000");
        linea("rv_entrada: .space 4096");
        linea("rv_profundidad: .word 0");
        linea("rv_p: .word 0");
        linea("rv_h: .word 0");
        linea("rv_msg_null: .asciz \"Error: referencia nula o invalida en llamada virtual\\n\"");
        linea("rv_msg_memoria: .asciz \"Error: indice fuera del rango de memoria RISC-V\\n\"");
        linea("rv_msg_recursion: .asciz \"Error: profundidad maxima de llamadas excedida\\n\"");
        linea("rv_msg_division: .asciz \"Error: division por cero\\n\"");
        linea("rv_msg_tipo: .asciz \"Error: tipos incompatibles en operacion RISC-V\\n\"");
        linea("rv_bool_true: .asciz \"true\"");
        linea("rv_bool_false: .asciz \"false\"");
        linea(".text");
        linea(".globl main");
        linea("main:");
        instr("la t3, rv_p");
        instr("sw zero, 0(t3)");
        instr("la t3, rv_h");
        instr("sw zero, 0(t3)");
        instr("la t3, rv_profundidad");
        instr("sw zero, 0(t3)");
        instr("jal ra, " + funciones.get("MAIOR"));
        instr("li a7, 10");
        instr("ecall");
        for (var entrada : cuerpos.entrySet()) {
            generarFuncion(entrada.getKey(), entrada.getValue());
        }
        generarRutinasCadenas();
        generarRutinaEntrada();
        linea("rv_error_referencia:");
        instr("la a0, rv_msg_null");
        instr("li a7, 4");
        instr("ecall");
        instr("li a7, 10");
        instr("ecall");
        linea("rv_error_memoria:");
        instr("la a0, rv_msg_memoria");
        instr("li a7, 4");
        instr("ecall");
        instr("li a7, 10");
        instr("ecall");
        linea("rv_error_tipo:");
        instr("la a0, rv_msg_tipo");
        instr("li a7, 4");
        instr("ecall");
        instr("li a7, 10");
        instr("ecall");
        linea("rv_error_division:");
        instr("la a0, rv_msg_division");
        instr("li a7, 4");
        instr("ecall");
        instr("li a7, 10");
        instr("ecall");
        linea("rv_error_recursion:");
        instr("la a0, rv_msg_recursion");
        instr("li a7, 4");
        instr("ecall");
        instr("li a7, 10");
        instr("ecall");
        return codigo.toString();
    }

    private void generarFuncion(String nombre, List<Cuarteta> cuerpo) {
        funcion = nombre;
        indiceFuncion = new ArrayList<>(funciones.keySet()).indexOf(nombre);
        indiceInterno = 0;
        temporales.clear();
        etiquetas.clear();
        for (Cuarteta c : cuerpo) {
            recolectar(c);
            if (c.operador() == OperadorCuarteta.ETIQUETA) {
                etiquetas.putIfAbsent(c.resultado(), "rv_lbl_" + indiceFuncion + "_" + etiquetas.size());
            }
        }
        int siguiente = 24;
        for (String temporal : new ArrayList<>(temporales.keySet())) {
            temporales.put(temporal, String.valueOf(siguiente));
            siguiente += 12;
        }
        tamanoMarco = ((siguiente + 15) / 16) * 16;
        linea(funciones.get(nombre) + ":");
        ajustarPila(-tamanoMarco);
        instr("sw ra, 0(sp)");
        instr("sw s0, 4(sp)");
        instr("sw s9, 8(sp)");
        instr("sw s10, 12(sp)");
        instr("sw s11, 16(sp)");
        instr("mv s0, sp");
        instr("la t3, rv_profundidad");
        instr("lw t4, 0(t3)");
        instr("addi t4, t4, 1");
        instr("sw t4, 0(t3)");
        instr("li t5, 1024");
        instr("bgt t4, t5, rv_error_recursion");
        for (Cuarteta c : cuerpo) {
            emitir(c);
        }
        linea(epilogo() + ":");
        instr("la t3, rv_profundidad");
        instr("lw t4, 0(t3)");
        instr("addi t4, t4, -1");
        instr("sw t4, 0(t3)");
        instr("lw ra, 0(s0)");
        instr("lw s9, 8(s0)");
        instr("lw s10, 12(s0)");
        instr("lw s11, 16(s0)");
        instr("lw t3, 4(s0)");
        if (tamanoMarco <= 2047) {
            instr("addi sp, s0, " + tamanoMarco);
        } else {
            instr("li t6, " + tamanoMarco);
            instr("add sp, s0, t6");
        }
        instr("mv s0, t3");
        instr("ret");
    }

    private void recolectar(Cuarteta c) {
        switch (c.operador()) {
            case ASIGNAR, NEGATIVO, NEGAR, LEER_STACK, LEER_HEAP, LEER_TIPO_OBJETO -> {
                referencia(c.argumento1()); referencia(c.resultado());
            }
            case SUMAR, RESTAR, MULTIPLICAR, DIVIDIR, MODULO,
                 IGUALDAD, DIFERENTE, MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL,
                 AND, OR, COMPARAR -> {
                referencia(c.argumento1()); referencia(c.argumento2()); referencia(c.resultado());
            }
            case ESCRIBIR_STACK, ESCRIBIR_HEAP, ESCRIBIR_TIPO_OBJETO -> {
                referencia(c.argumento1()); referencia(c.argumento2());
            }
            case SALTAR_SI_FALSO, SALTAR_SI_VERDADERO, IMPRIMIR -> referencia(c.argumento1());
            case LEER -> referencia(c.resultado());
            default -> { }
        }
    }

    private void referencia(String expresion) {
        if (expresion == null || expresion.equals("-")) return;
        String dato = expresion.replace("@STR@", "").trim();
        Matcher desplazado = DESPLAZAMIENTO.matcher(dato);
        if (desplazado.matches()) dato = desplazado.group(1);
        if (dato.equals("P") || dato.equals("H") || dato.equals("null")
                || dato.equals("true") || dato.equals("false") || dato.equals("verum")
                || dato.equals("falsus") || dato.matches("[-+]?[0-9]+(?:\\.[0-9]+)?")) return;
        if (REFERENCIA.matcher(dato).matches()) temporales.putIfAbsent(dato, "");
    }

    private void emitir(Cuarteta c) {
        switch (c.operador()) {
            case DECLARAR, DECLARAR_PARAMETRO -> { }
            case ASIGNAR -> { cargarTipado(c.argumento1(), "t0", "a4"); guardarTipado(c.resultado(), "t0", "a4"); }
            case SUMAR -> {
                if (esMarcadaCadena(c.argumento1()) || esMarcadaCadena(c.argumento2())) {
                    concatenar(c);
                } else {
                    binaria(c);
                }
            }
            case RESTAR, MULTIPLICAR, MODULO, DIVIDIR, IGUALDAD, DIFERENTE,
                 MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, AND, OR, COMPARAR -> binaria(c);
            case NEGATIVO -> {
                cargarTipado(c.argumento1(), "t0", "a4");
                String fin = interno("negativo_fin");
                instr("beqz a4, " + fin);
                cargarDoble("t0", "s10", "ft0");
                instr("fneg.d ft0, ft0");
                guardarDobleEnRegistros("ft0", "t2", "s9");
                instr("j " + fin + "_guardar");
                linea(fin + ":");
                instr("neg t2, t0");
                instr("li s9, 0");
                linea(fin + "_guardar:");
                guardarTipado(c.resultado(), "t2", "a4");
            }
            case NEGAR -> {
                cargar(c.argumento1(), "t0"); instr("seqz t2, t0");
                instr("li a4, 0"); guardarTipado(c.resultado(), "t2", "a4");
            }
            case ETIQUETA -> linea(etiqueta(c.resultado()) + ":");
            case SALTAR -> instr("j " + etiqueta(c.resultado()));
            case SALTAR_SI_FALSO, SALTAR_SI_VERDADERO -> {
                cargar(c.argumento1(), "t0");
                instr((c.operador() == OperadorCuarteta.SALTAR_SI_FALSO ? "beqz" : "bnez")
                        + " t0, " + etiqueta(c.resultado()));
            }
            case LLAMAR -> {
                String destino = funciones.get(c.argumento1());
                if (destino == null) noSoportado(c, "funcion no definida");
                instr("jal ra, " + destino);
            }
            case RETORNAR -> instr("j " + epilogo());
            case LEER_STACK, LEER_HEAP, LEER_TIPO_OBJETO -> leerMemoria(c);
            case ESCRIBIR_STACK, ESCRIBIR_HEAP, ESCRIBIR_TIPO_OBJETO -> escribirMemoria(c);
            case IMPRIMIR -> imprimir(c);
            case LEER -> leer(c);
            case PARAMETRO, NUEVO_OBJETO, NUEVO_ARREGLO, INICIALIZAR_COMPUESTO ->
                    noSoportado(c, "operacion sin bajar a memoria");
            case INICIO_FUNCION, FIN_FUNCION -> noSoportado(c, "marcador de funcion inesperado");
        }
    }

    private boolean esMarcadaCadena(String valor) {
        return valor != null && valor.startsWith("@STR@");
    }

    private void concatenar(Cuarteta c) {
        cargarTipado(c.argumento1(), "a0", "a4");
        cargarTipado(c.argumento2(), "a1", "a5");
        instr("li a2, " + (esMarcadaCadena(c.argumento1()) ? "1" : "0"));
        instr("li a3, " + (esMarcadaCadena(c.argumento2()) ? "1" : "0"));
        instr("jal ra, rv_concat");
        instr("li a4, 0");
        guardarTipado(c.resultado(), "a0", "a4");
    }

    private String interno(String prefijo) {
        return "rv_" + prefijo + "_" + indiceFuncion + "_" + indiceInterno++;
    }

    private void binaria(Cuarteta c) {
        if (esMarcadaCadena(c.argumento1()) || esMarcadaCadena(c.argumento2())) {
            noSoportado(c, "operacion de cadenas");
        }
        cargarTipado(c.argumento1(), "t0", "a4");
        cargarTipado(c.argumento2(), "t1", "a5");
        boolean aritmetica = switch (c.operador()) {
            case SUMAR, RESTAR, MULTIPLICAR, DIVIDIR, MODULO -> true;
            default -> false;
        };
        String flotante = interno("flotante");
        String finalizado = interno("bin_fin");
        if (c.operador() == OperadorCuarteta.DIVIDIR) {
            instr("j " + flotante);
        } else {
            instr("or a6, a4, a5");
            instr("bnez a6, " + flotante);
        }
        if (c.operador() != OperadorCuarteta.DIVIDIR) {
            switch (c.operador()) {
                case SUMAR -> instr("add t2, t0, t1");
                case RESTAR -> instr("sub t2, t0, t1");
                case MULTIPLICAR -> instr("mul t2, t0, t1");
                case MODULO -> { instr("beqz t1, rv_error_division"); instr("rem t2, t0, t1"); }
                case IGUALDAD -> { instr("sub t2, t0, t1"); instr("seqz t2, t2"); }
                case DIFERENTE -> { instr("sub t2, t0, t1"); instr("snez t2, t2"); }
                case MENOR -> instr("slt t2, t0, t1");
                case MAYOR -> instr("slt t2, t1, t0");
                case MENOR_IGUAL -> { instr("slt t2, t1, t0"); instr("xori t2, t2, 1"); }
                case MAYOR_IGUAL -> { instr("slt t2, t0, t1"); instr("xori t2, t2, 1"); }
                case AND -> { instr("snez t0, t0"); instr("snez t1, t1"); instr("and t2, t0, t1"); }
                case OR -> { instr("or t2, t0, t1"); instr("snez t2, t2"); }
                case COMPARAR -> { instr("slt t2, t1, t0"); instr("slt t3, t0, t1"); instr("sub t2, t2, t3"); }
                default -> noSoportado(c, "operacion binaria");
            }
            instr("li a6, 0");
            instr("li s9, 0");
            instr("j " + finalizado);
        }
        linea(flotante + ":");
        if (c.operador() == OperadorCuarteta.AND || c.operador() == OperadorCuarteta.OR) {
            instr("j rv_error_tipo");
        } else {
            convertirFlotante("t0", "a4", "ft0");
            convertirFlotante("t1", "a5", "ft1");
            switch (c.operador()) {
                case SUMAR -> instr("fadd.d ft2, ft0, ft1");
                case RESTAR -> instr("fsub.d ft2, ft0, ft1");
                case MULTIPLICAR -> instr("fmul.d ft2, ft0, ft1");
                case DIVIDIR -> {
                    instr("fcvt.d.w ft3, zero");
                    instr("feq.d t3, ft1, ft3");
                    instr("bnez t3, rv_error_division");
                    instr("fdiv.d ft2, ft0, ft1");
                }
                case MODULO -> {
                    instr("fcvt.d.w ft3, zero");
                    instr("feq.d t3, ft1, ft3");
                    instr("bnez t3, rv_error_division");
                    instr("fdiv.d ft2, ft0, ft1");
                    instr("fcvt.w.d t3, ft2, rtz");
                    instr("fcvt.d.w ft2, t3");
                    instr("fmul.d ft2, ft2, ft1");
                    instr("fsub.d ft2, ft0, ft2");
                }
                case IGUALDAD -> instr("feq.d t2, ft0, ft1");
                case DIFERENTE -> { instr("feq.d t2, ft0, ft1"); instr("xori t2, t2, 1"); }
                case MENOR -> instr("flt.d t2, ft0, ft1");
                case MAYOR -> instr("flt.d t2, ft1, ft0");
                case MENOR_IGUAL -> instr("fle.d t2, ft0, ft1");
                case MAYOR_IGUAL -> instr("fle.d t2, ft1, ft0");
                case COMPARAR -> {
                    instr("flt.d t2, ft1, ft0");
                    instr("flt.d t3, ft0, ft1");
                    instr("sub t2, t2, t3");
                }
                default -> noSoportado(c, "operacion flotante");
            }
            if (aritmetica) {
                guardarDobleEnRegistros("ft2", "t2", "s9");
                instr("li a6, 1");
            } else {
                instr("li a6, 0");
                instr("li s9, 0");
            }
        }
        linea(finalizado + ":");
        guardarTipado(c.resultado(), "t2", "a6");
    }

    private void convertirFlotante(String valor, String etiquetaTipo, String destino) {
        String fin = interno("convertido");
        instr("beqz " + etiquetaTipo + ", " + fin + "_entero");
        cargarDoble(valor, etiquetaTipo.equals("a4") ? "s10" : "s11", destino);
        instr("j " + fin);
        linea(fin + "_entero:");
        instr("fcvt.d.w " + destino + ", " + valor);
        linea(fin + ":");
    }

    private void cargarDoble(String bajo, String alto, String flotante) {
        instr("la t6, rv_decimal_aux");
        instr("sw " + bajo + ", 0(t6)");
        instr("sw " + alto + ", 4(t6)");
        instr("fld " + flotante + ", 0(t6)");
    }

    private void guardarDobleEnRegistros(String flotante, String bajo, String alto) {
        instr("la t6, rv_decimal_aux");
        instr("fsd " + flotante + ", 0(t6)");
        instr("lw " + bajo + ", 0(t6)");
        instr("lw " + alto + ", 4(t6)");
    }

    private void leerMemoria(Cuarteta c) {
        String base = switch (c.operador()) {
            case LEER_STACK -> "rv_stack";
            case LEER_HEAP -> "rv_heap";
            case LEER_TIPO_OBJETO -> "rv_tipo";
            default -> throw new IllegalStateException();
        };
        cargar(c.argumento1(), "t0");
        if (c.operador() == OperadorCuarteta.LEER_TIPO_OBJETO) {
            instr("bltz t0, rv_error_referencia");
        }
        direccion(base);
        instr("lw t2, 0(t5)");
        if (c.operador() != OperadorCuarteta.LEER_TIPO_OBJETO) {
            instr("la t3, " + (c.operador() == OperadorCuarteta.LEER_STACK
                    ? "rv_stack_decimal" : "rv_heap_decimal"));
            instr("add t3, t3, t4");
            instr("lw a4, 0(t3)");
            instr("la t3, " + (c.operador() == OperadorCuarteta.LEER_STACK
                    ? "rv_stack_decimal_alto" : "rv_heap_decimal_alto"));
            instr("add t3, t3, t4");
            instr("lw s10, 0(t3)");
        } else {
            instr("li s10, 0");
            instr("li a4, 0");
        }
        if (c.operador() == OperadorCuarteta.LEER_TIPO_OBJETO) {
            instr("beqz t2, rv_error_referencia");
        }
        guardarTipado(c.resultado(), "t2", "a4");
    }

    private void escribirMemoria(Cuarteta c) {
        String base = switch (c.operador()) {
            case ESCRIBIR_STACK -> "rv_stack";
            case ESCRIBIR_HEAP -> "rv_heap";
            case ESCRIBIR_TIPO_OBJETO -> "rv_tipo";
            default -> throw new IllegalStateException();
        };
        cargar(c.argumento1(), "t0");
        cargarTipado(c.argumento2(), "t1", "a5");
        direccion(base);
        instr("sw t1, 0(t5)");
        if (c.operador() != OperadorCuarteta.ESCRIBIR_TIPO_OBJETO) {
            instr("la t3, " + (c.operador() == OperadorCuarteta.ESCRIBIR_STACK
                    ? "rv_stack_decimal" : "rv_heap_decimal"));
            instr("add t3, t3, t4");
            instr("sw a5, 0(t3)");
            instr("la t3, " + (c.operador() == OperadorCuarteta.ESCRIBIR_STACK
                    ? "rv_stack_decimal_alto" : "rv_heap_decimal_alto"));
            instr("add t3, t3, t4");
            instr("sw s11, 0(t3)");
        }
    }

    private void direccion(String base) {
        instr("bltz t0, rv_error_memoria");
        instr("li t3, 100000");
        instr("bge t0, t3, rv_error_memoria");
        instr("slli t4, t0, 2");
        instr("la t5, " + base);
        instr("add t5, t5, t4");
    }

    private void imprimir(Cuarteta c) {
        String modo = c.argumento2().toLowerCase();
        boolean salto = modo.startsWith("println");
        boolean cadena = c.argumento1().startsWith("@STR@") || c.argumento1().matches("str_t[0-9]+");
        String valor = c.argumento1().replace("@STR@", "");
        cargar(valor, "t0");
        if (cadena) {
            String ciclo = "rv_print_" + indiceFuncion + "_" + indiceInterno++;
            instr("mv t6, t0");
            linea(ciclo + ":");
            instr("bltz t6, " + ciclo + "_fin");
            instr("li t3, 100000");
            instr("bge t6, t3, rv_error_memoria");
            instr("slli t4, t6, 2");
            instr("la t5, rv_heap");
            instr("add t5, t5, t4");
            instr("lw a0, 0(t5)");
            instr("li t3, -1");
            instr("beq a0, t3, " + ciclo + "_fin");
            instr("li a7, 11");
            instr("ecall");
            instr("addi t6, t6, 1");
            instr("j " + ciclo);
            linea(ciclo + "_fin:");
        } else if (modo.endsWith(":char")) {
            instr("mv a0, t0"); instr("li a7, 11"); instr("ecall");
        } else if (modo.endsWith(":boolean")) {
            String falso = "rv_bool_" + indiceFuncion + "_" + indiceInterno++;
            instr("beqz t0, " + falso);
            instr("la a0, rv_bool_true");
            instr("j " + falso + "_fin");
            linea(falso + ":");
            instr("la a0, rv_bool_false");
            linea(falso + "_fin:");
            instr("li a7, 4"); instr("ecall");
        } else {
            String entero = interno("print_entero");
            String flotante = interno("print_flotante");
            String fin = interno("print_fin");
            cargarTipado(c.argumento1(), "t0", "a4");
            instr("beqz a4, " + entero);
            cargarDoble("t0", "s10", "ft0");
            instr("li t1, 2147483647");
            instr("fcvt.d.w ft1, t1");
            instr("flt.d t2, ft1, ft0");
            instr("bnez t2, " + flotante);
            instr("li t1, -2147483648");
            instr("fcvt.d.w ft1, t1");
            instr("flt.d t2, ft0, ft1");
            instr("bnez t2, " + flotante);
            instr("fcvt.w.d t1, ft0, rtz");
            instr("fcvt.d.w ft1, t1");
            instr("feq.d t2, ft0, ft1");
            instr("beqz t2, " + flotante);
            instr("mv a0, t1");
            instr("li a7, 1");
            instr("ecall");
            instr("j " + fin);
            linea(flotante + ":");
            instr("fsgnj.d fa0, ft0, ft0");
            instr("li a7, 3");
            instr("ecall");
            instr("j " + fin);
            linea(entero + ":");
            instr("mv a0, t0");
            instr("li a7, 1");
            instr("ecall");
            linea(fin + ":");
        }
        if (salto) { instr("li a0, 10"); instr("li a7, 11"); instr("ecall"); }
    }

    private void leer(Cuarteta c) {
        String tipo = c.argumento1().toLowerCase();
        if (tipo.equals("descartar") || tipo.contains("string")
                || tipo.contains("text") || tipo.contains("cadena")) {
            instr("jal ra, rv_read_string");
            if (!c.resultado().equals("-")) {
                instr("li a4, 0");
                guardarTipado(c.resultado(), "a0", "a4");
            }
            return;
        }
        if (c.resultado().equals("-")) return;
        if (tipo.contains("decimal") || tipo.contains("flotante") || tipo.contains("double")) {
            instr("li a7, 7"); instr("ecall");
            guardarDobleEnRegistros("fa0", "t0", "s10");
            instr("li a4, 1");
            guardarTipado(c.resultado(), "t0", "a4");
        } else {
            instr("li a7, " + (tipo.contains("char") || tipo.contains("littera") ? 12 : 5));
            instr("ecall");
            instr("li a4, 0"); guardarTipado(c.resultado(), "a0", "a4");
        }
    }

    private void generarRutinaEntrada() {
        linea("rv_read_string:");
        instr("addi sp, sp, -16");
        instr("sw ra, 0(sp)");
        instr("sw s1, 4(sp)");
        instr("sw s2, 8(sp)");
        instr("la a0, rv_entrada");
        instr("li a1, 4096");
        instr("li a7, 8");
        instr("ecall");
        instr("la s1, rv_entrada");
        instr("la t0, rv_h");
        instr("lw s2, 0(t0)");
        linea("rv_read_string_loop:");
        instr("lbu a0, 0(s1)");
        instr("beqz a0, rv_read_string_end");
        instr("li t0, 10");
        instr("beq a0, t0, rv_read_string_end");
        instr("li t0, 13");
        instr("beq a0, t0, rv_read_string_end");
        instr("jal ra, rv_append_char");
        instr("addi s1, s1, 1");
        instr("j rv_read_string_loop");
        linea("rv_read_string_end:");
        instr("li a0, -1");
        instr("jal ra, rv_append_char");
        instr("mv a0, s2");
        instr("lw ra, 0(sp)");
        instr("lw s1, 4(sp)");
        instr("lw s2, 8(sp)");
        instr("addi sp, sp, 16");
        instr("ret");
    }

    private void ajustarPila(int ajuste) {
        if (ajuste >= -2048 && ajuste <= 2047) {
            instr("addi sp, sp, " + ajuste);
        } else {
            instr("li t6, " + ajuste);
            instr("add sp, sp, t6");
        }
    }

    private void leerSlot(String registro, int offset) {
        if (offset <= 2047) {
            instr("lw " + registro + ", " + offset + "(s0)");
        } else {
            instr("li t6, " + offset);
            instr("add t6, s0, t6");
            instr("lw " + registro + ", 0(t6)");
        }
    }

    private void escribirSlot(String registro, int offset) {
        if (offset <= 2047) {
            instr("sw " + registro + ", " + offset + "(s0)");
        } else {
            instr("li t6, " + offset);
            instr("add t6, s0, t6");
            instr("sw " + registro + ", 0(t6)");
        }
    }

    private void generarRutinasCadenas() {
        linea("rv_append_char:");
        instr("la t0, rv_h");
        instr("lw t1, 0(t0)");
        instr("bltz t1, rv_error_memoria");
        instr("li t2, 100000");
        instr("bge t1, t2, rv_error_memoria");
        instr("slli t3, t1, 2");
        instr("la t4, rv_heap");
        instr("add t4, t4, t3");
        instr("sw a0, 0(t4)");
        instr("addi t1, t1, 1");
        instr("sw t1, 0(t0)");
        instr("ret");

        linea("rv_append_string:");
        instr("addi sp, sp, -16");
        instr("sw ra, 0(sp)");
        instr("sw s1, 4(sp)");
        instr("mv s1, a0");
        instr("bltz s1, rv_append_string_null");
        linea("rv_append_string_loop:");
        instr("li t0, 100000");
        instr("bge s1, t0, rv_error_memoria");
        instr("slli t0, s1, 2");
        instr("la t1, rv_heap");
        instr("add t1, t1, t0");
        instr("lw a0, 0(t1)");
        instr("li t2, -1");
        instr("beq a0, t2, rv_append_string_fin");
        instr("jal ra, rv_append_char");
        instr("addi s1, s1, 1");
        instr("j rv_append_string_loop");
        linea("rv_append_string_null:");
        for (int codigo : new int[]{110, 117, 108, 108}) {
            instr("li a0, " + codigo);
            instr("jal ra, rv_append_char");
        }
        linea("rv_append_string_fin:");
        instr("lw ra, 0(sp)");
        instr("lw s1, 4(sp)");
        instr("addi sp, sp, 16");
        instr("ret");

        linea("rv_append_number:");
        instr("addi sp, sp, -32");
        instr("sw ra, 0(sp)");
        instr("sw s1, 4(sp)");
        instr("sw s2, 8(sp)");
        instr("mv s1, a0");
        instr("bgez s1, rv_append_number_digits");
        instr("li a0, 45");
        instr("jal ra, rv_append_char");
        instr("neg s1, s1");
        linea("rv_append_number_digits:");
        instr("li s2, 0");
        linea("rv_append_number_divide:");
        instr("li t5, 10");
        instr("remu t3, s1, t5");
        instr("addi t3, t3, 48");
        instr("add t4, sp, s2");
        instr("sb t3, 16(t4)");
        instr("addi s2, s2, 1");
        instr("divu s1, s1, t5");
        instr("bnez s1, rv_append_number_divide");
        linea("rv_append_number_print:");
        instr("addi s2, s2, -1");
        instr("bltz s2, rv_append_number_fin");
        instr("add t4, sp, s2");
        instr("lbu a0, 16(t4)");
        instr("jal ra, rv_append_char");
        instr("j rv_append_number_print");
        linea("rv_append_number_fin:");
        instr("lw ra, 0(sp)");
        instr("lw s1, 4(sp)");
        instr("lw s2, 8(sp)");
        instr("addi sp, sp, 32");
        instr("ret");

        linea("rv_append_float:");
        instr("addi sp, sp, -32");
        instr("sw ra, 0(sp)");
        instr("sw s1, 4(sp)");
        instr("sw s2, 8(sp)");
        instr("sw s3, 12(sp)");
        cargarDoble("a0", "a1", "ft0");
        instr("fcvt.d.w ft1, zero");
        instr("flt.d s1, ft0, ft1");
        instr("beqz s1, rv_float_unsigned");
        instr("li a0, 45");
        instr("jal ra, rv_append_char");
        instr("fsgnjx.d ft0, ft0, ft0");
        linea("rv_float_unsigned:");
        instr("fcvt.w.d s2, ft0, rtz");
        instr("fcvt.d.w ft1, s2");
        instr("fsub.d ft0, ft0, ft1");
        instr("li t0, 1000");
        instr("fcvt.d.w ft1, t0");
        instr("fmul.d ft0, ft0, ft1");
        instr("fcvt.w.d s3, ft0, rne");
        instr("li t0, 1000");
        instr("blt s3, t0, rv_float_no_carry");
        instr("addi s2, s2, 1");
        instr("li s3, 0");
        linea("rv_float_no_carry:");
        instr("mv a0, s2");
        instr("jal ra, rv_append_number");
        instr("li a0, 46");
        instr("jal ra, rv_append_char");
        instr("li t0, 100");
        instr("div s1, s3, t0");
        instr("rem s3, s3, t0");
        instr("addi a0, s1, 48");
        instr("jal ra, rv_append_char");
        instr("li t0, 10");
        instr("div s1, s3, t0");
        instr("rem s3, s3, t0");
        instr("addi a0, s1, 48");
        instr("jal ra, rv_append_char");
        instr("addi a0, s3, 48");
        instr("jal ra, rv_append_char");
        instr("lw ra, 0(sp)");
        instr("lw s1, 4(sp)");
        instr("lw s2, 8(sp)");
        instr("lw s3, 12(sp)");
        instr("addi sp, sp, 32");
        instr("ret");

        linea("rv_concat:");
        instr("addi sp, sp, -32");
        instr("sw ra, 0(sp)");
        instr("sw s1, 4(sp)");
        instr("sw s2, 8(sp)");
        instr("sw s3, 12(sp)");
        instr("sw s4, 16(sp)");
        instr("sw s5, 20(sp)");
        instr("sw s6, 24(sp)");
        instr("sw s7, 28(sp)");
        instr("mv s6, a4");
        instr("mv s7, a5");
        instr("mv s1, a0");
        instr("mv s2, a1");
        instr("mv s3, a2");
        instr("mv s4, a3");
        instr("la t0, rv_h");
        instr("lw s5, 0(t0)");
        instr("mv a0, s1");
        instr("bnez s3, rv_concat_left_string");
        instr("bnez s6, rv_concat_left_float");
        instr("jal ra, rv_append_number");
        instr("j rv_concat_right");
        linea("rv_concat_left_float:");
        instr("mv a1, s10");
        instr("jal ra, rv_append_float");
        instr("j rv_concat_right");
        linea("rv_concat_left_string:");
        instr("jal ra, rv_append_string");
        linea("rv_concat_right:");
        instr("mv a0, s2");
        instr("bnez s4, rv_concat_right_string");
        instr("bnez s7, rv_concat_right_float");
        instr("jal ra, rv_append_number");
        instr("j rv_concat_end");
        linea("rv_concat_right_float:");
        instr("mv a1, s11");
        instr("jal ra, rv_append_float");
        instr("j rv_concat_end");
        linea("rv_concat_right_string:");
        instr("jal ra, rv_append_string");
        linea("rv_concat_end:");
        instr("li a0, -1");
        instr("jal ra, rv_append_char");
        instr("mv a0, s5");
        instr("lw ra, 0(sp)");
        instr("lw s1, 4(sp)");
        instr("lw s2, 8(sp)");
        instr("lw s3, 12(sp)");
        instr("lw s4, 16(sp)");
        instr("lw s5, 20(sp)");
        instr("lw s6, 24(sp)");
        instr("lw s7, 28(sp)");
        instr("addi sp, sp, 32");
        instr("ret");
    }

    private void cargar(String valor, String registro) {
        String dato = valor.trim().replace("@STR@", "");
        Matcher m = DESPLAZAMIENTO.matcher(dato);
        if (m.matches()) {
            cargar(m.group(1), registro);
            long incremento = Long.parseLong(m.group(3));
            if (m.group(2).equals("-")) incremento = -incremento;
            if (incremento < -2048 || incremento > 2047) {
                instr("li t3, " + incremento);
                instr("add " + registro + ", " + registro + ", t3");
            } else {
                instr("addi " + registro + ", " + registro + ", " + incremento);
            }
            return;
        }
        if (dato.matches("[-+]?[0-9]+")) {
            instr("li " + registro + ", " + dato);
        } else if (dato.matches("[-+]?[0-9]+\\.[0-9]+")) {
            instr("li " + registro + ", " + (int) Double.doubleToRawLongBits(Double.parseDouble(dato)));
        } else if (esLiteralCaracter(dato)) {
            instr("li " + registro + ", " + codigoCaracter(dato));
        } else if (dato.equals("null")) {
            instr("li " + registro + ", -1");
        } else if (dato.equals("true") || dato.equals("verum")) {
            instr("li " + registro + ", 1");
        } else if (dato.equals("false") || dato.equals("falsus")) {
            instr("li " + registro + ", 0");
        } else if (dato.equals("P") || dato.equals("H")) {
            instr("la t3, " + (dato.equals("P") ? "rv_p" : "rv_h"));
            instr("lw " + registro + ", 0(t3)");
        } else if (temporales.containsKey(dato)) {
            leerSlot(registro, Integer.parseInt(temporales.get(dato)));
        } else {
            throw new IllegalArgumentException("RISC-V: expresion no soportada '" + valor + "' en " + funcion);
        }
    }

    private boolean esLiteralCaracter(String valor) {
        return valor.length() >= 2 && valor.charAt(0) == '\''
                && valor.charAt(valor.length() - 1) == '\'';
    }

    private int codigoCaracter(String valor) {
        String contenido = valor.substring(1, valor.length() - 1);
        if (contenido.length() == 1 && contenido.charAt(0) != '\\') {
            return contenido.charAt(0);
        }
        if (contenido.length() == 2 && contenido.charAt(0) == '\\') {
            return switch (contenido.charAt(1)) {
                case 'n' -> 10;
                case 'r' -> 13;
                case 't' -> 9;
                case 'b' -> 8;
                case 'f' -> 12;
                case '0' -> 0;
                case '\\' -> 92;
                case '\'' -> 39;
                case '"' -> 34;
                default -> throw new IllegalArgumentException(
                        "RISC-V: escape de caracter no soportado " + valor + " en " + funcion);
            };
        }
        throw new IllegalArgumentException(
                "RISC-V: literal de caracter invalido " + valor + " en " + funcion);
    }

    private void cargarTipado(String valor, String registro, String tipoRegistro) {
        cargar(valor, registro);
        String dato = valor.trim().replace("@STR@", "");
        String registroAlto = tipoRegistro.equals("a4") ? "s10" : "s11";
        if (dato.matches("[-+]?[0-9]+\\.[0-9]+")) {
            long bits = Double.doubleToRawLongBits(Double.parseDouble(dato));
            instr("li " + registroAlto + ", " + (int) (bits >>> 32));
            instr("li " + tipoRegistro + ", 1");
        } else if (temporales.containsKey(dato)) {
            int offset = Integer.parseInt(temporales.get(dato));
            leerSlot(registroAlto, offset + 4);
            leerSlot(tipoRegistro, offset + 8);
        } else {
            instr("li " + registroAlto + ", 0");
            instr("li " + tipoRegistro + ", 0");
        }
    }

    private void guardarTipado(String destino, String registro, String tipoRegistro) {
        guardar(destino, registro);
        if (temporales.containsKey(destino)) {
            int offset = Integer.parseInt(temporales.get(destino));
            String registroAlto = tipoRegistro.equals("a6") ? "s9" :
                    tipoRegistro.equals("a5") ? "s11" : "s10";
            escribirSlot(registroAlto, offset + 4);
            escribirSlot(tipoRegistro, offset + 8);
        }
    }

    private void guardar(String destino, String registro) {
        if (destino.equals("-")) return;
        if (destino.equals("P") || destino.equals("H")) {
            instr("la t3, " + (destino.equals("P") ? "rv_p" : "rv_h"));
            instr("sw " + registro + ", 0(t3)");
        } else if (temporales.containsKey(destino)) {
            escribirSlot(registro, Integer.parseInt(temporales.get(destino)));
        } else {
            throw new IllegalArgumentException("RISC-V: destino no soportado '" + destino + "' en " + funcion);
        }
    }

    private String etiqueta(String nombre) {
        String resultado = etiquetas.get(nombre);
        if (resultado == null) throw new IllegalArgumentException("RISC-V: etiqueta inexistente '" + nombre + "'");
        return resultado;
    }

    private String epilogo() { return "rv_fin_" + indiceFuncion; }
    private void noSoportado(Cuarteta c, String razon) {
        throw new IllegalArgumentException("RISC-V pendiente (" + razon + ") en " + funcion + ": " + c);
    }
    private void linea(String texto) { codigo.append(texto).append('\n'); }
    private void instr(String texto) {
        Matcher salto = SALTO_CONDICIONAL.matcher(texto);
        if (salto.matches()) {
            String inversa = CONDICIONES_INVERSAS.get(salto.group(1));
            if (inversa != null) {
                String siguiente = "rv_salto_corto_" + siguienteSaltoCorto++;
                linea("    " + inversa + " " + salto.group(2) + ", " + siguiente);
                linea("    j " + salto.group(3));
                linea(siguiente + ":");
                return;
            }
        }
        linea("    " + texto);
    }
}
