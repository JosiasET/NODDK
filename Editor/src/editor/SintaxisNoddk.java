package editor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SintaxisNoddk {
    // Patrones para identificadores y literales
    private static final Pattern IDENTIFICADOR_VALIDO = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern NUMERO = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");
    private static final Pattern CADENA = Pattern.compile("^\"(.*?)\"$");
    private static final Pattern CHAR = Pattern.compile("^'(.)'$");
    private static final Pattern BOOLEANO = Pattern.compile("^(true|false)$");
    
    // Patrones para declaraciones y asignaciones
    private static final Pattern DECLARACION = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+?)\\s*;?$");
    
    // Patrones para operadores
    private static final Pattern OPERADOR_ARITMETICO = Pattern.compile("[+\\-*/%]");
    private static final Pattern OPERADOR_COMPARACION = Pattern.compile("==|!=|<=|>=|<|>");
    private static final Pattern OPERADOR_LOGICO = Pattern.compile("&&|\\|\\|");
    private static final Pattern OPERADOR_UNARIO = Pattern.compile("[+\\-!]");
    
    // Patrones para estructuras de control
    private static final Pattern PRINT = Pattern.compile("^print(ln)?\\((.+?)\\);?$", Pattern.DOTALL);
    private static final Pattern INPUT = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*input\\((?:\"(.+?)\")?\\);?$");
    private static final Pattern IF = Pattern.compile("^if\\s*\\((.+?)\\)\\s*\\{(.*?)\\}(?:\\s*else\\s*if\\s*\\((.+?)\\)\\s*\\{(.*?)\\})*(?:\\s*else\\s*\\{(.*?)\\})?\\s*$", Pattern.DOTALL);
    private static final Pattern WHILE = Pattern.compile("^while\\s*\\((.+?)\\)\\s*\\{(.*?)\\}\\s*$", Pattern.DOTALL);
    private static final Pattern DO_WHILE = Pattern.compile("^do\\s*\\{(.*?)\\}\\s*while\\s*\\((.+?)\\)\\s*;?\\s*$", Pattern.DOTALL);
    private static final Pattern FOR = Pattern.compile("^for\\s*\\(\\s*(.*?)\\s*;\\s*(.*?)\\s*;\\s*(.*?)\\s*\\)\\s*\\{(.*?)\\}\\s*$", Pattern.DOTALL);
    private static final Pattern BREAK = Pattern.compile("^break\\s*;?$");
    private static final Pattern RETURN = Pattern.compile("^return\\s*(.+?)\\s*;?$");
    
    // Patrones para funciones
    private static final Pattern FUNCION = Pattern.compile("^function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(\\s*(.*?)\\s*\\)\\s*\\{(.*?)\\}\\s*$", Pattern.DOTALL);
    private static final Pattern LLAMADA_FUNCION = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(\\s*(.*?)\\s*\\)\\s*;?$");
    
    // Patrones para cadenas formateadas e interpolación
    private static final Pattern CADENA_FORMATEADA = Pattern.compile("^f\"(.*)\"$");
    private static final Pattern EXPRESION_INTERPOLADA = Pattern.compile("\\$\\{(.+?)\\}");
    
    // Patrones para incrementos/decrementos
    private static final Pattern INCREMENTO = Pattern.compile("^(\\+\\+|--)?\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\+\\+|--)?\\s*;?$");
    
    private static final Map<String, Object> variables = new HashMap<>();
    private static final Map<String, Funcion> funciones = new HashMap<>();

    public static String procesarLinea(String linea) {
        return procesarLinea(linea, 1);
    }

    public static String procesarLinea(String linea, int numeroLinea) {
        StringBuilder resultado = new StringBuilder();
        linea = linea.trim();

        // Eliminar comentarios
        linea = removerComentarios(linea).trim();
        if (linea.isEmpty()) {
            return "";
        }

        try {
            // 1) Break
            if (BREAK.matcher(linea).matches()) {
                // En una implementación completa, esto debería lanzar una excepción controlada
                return "[break]";
            }

            // 2) Return
            Matcher matcherReturn = RETURN.matcher(linea);
            if (matcherReturn.matches()) {
                Object valor = evaluarExpresion(matcherReturn.group(1).trim());
                return "[return: " + valor + "]";
            }

            // 3) Declaración/Asignación de variable
            Matcher matcherDeclaracion = DECLARACION.matcher(linea);
            if (matcherDeclaracion.matches()) {
                String identificador = matcherDeclaracion.group(1);
                String valorExpr = matcherDeclaracion.group(2);
                Object valor = evaluarExpresion(valorExpr);
                variables.put(identificador, valor);
                return "";
            }

            // 4) Input
            Matcher matcherInput = INPUT.matcher(linea);
            if (matcherInput.matches()) {
                String identificador = matcherInput.group(1);
                String mensaje = matcherInput.group(2) != null ? matcherInput.group(2) : "Ingrese un valor: ";
                // En una implementación real, aquí se leería de la consola
                variables.put(identificador, "[input: " + mensaje + "]");
                return "";
            }

            // 5) Print/Println
            Matcher matcherPrint = PRINT.matcher(linea);
            if (matcherPrint.matches()) {
                boolean esPrintln = matcherPrint.group(1) != null;
                String contenido = matcherPrint.group(2).trim();
                String output = procesarPrint(contenido);
                return esPrintln ? output + "\n" : output;
            }

            // 6) If/else if/else
            Matcher matcherIf = IF.matcher(linea);
            if (matcherIf.matches()) {
                return procesarIf(matcherIf, numeroLinea);
            }

            // 7) While
            Matcher matcherWhile = WHILE.matcher(linea);
            if (matcherWhile.matches()) {
                return procesarWhile(matcherWhile, numeroLinea);
            }

            // 8) Do-While
            Matcher matcherDoWhile = DO_WHILE.matcher(linea);
            if (matcherDoWhile.matches()) {
                return procesarDoWhile(matcherDoWhile, numeroLinea);
            }

            // 9) For
            Matcher matcherFor = FOR.matcher(linea);
            if (matcherFor.matches()) {
                return procesarFor(matcherFor, numeroLinea);
            }

            // 10) Definición de función
            Matcher matcherFuncion = FUNCION.matcher(linea);
            if (matcherFuncion.matches()) {
                String nombre = matcherFuncion.group(1);
                String parametros = matcherFuncion.group(2).trim();
                String cuerpo = matcherFuncion.group(3).trim();
                funciones.put(nombre, new Funcion(nombre, parametros, cuerpo));
                return "";
            }

            // 11) Llamada a función
            Matcher matcherLlamada = LLAMADA_FUNCION.matcher(linea);
            if (matcherLlamada.matches()) {
                String nombreFuncion = matcherLlamada.group(1);
                String argumentos = matcherLlamada.group(2).trim();
                return ejecutarFuncion(nombreFuncion, argumentos, numeroLinea);
            }

            // 12) Incremento/Decremento
            Matcher matcherIncremento = INCREMENTO.matcher(linea);
            if (matcherIncremento.matches() && (matcherIncremento.group(1) != null || matcherIncremento.group(3) != null)) {
                return procesarIncremento(matcherIncremento);
            }

            // 13) Expresión simple (sin asignación)
            Object resultadoExpresion = evaluarExpresion(linea);
            if (resultadoExpresion != null) {
                return resultadoExpresion.toString();
            }

        } catch (Exception e) {
            return "Error en línea " + numeroLinea + ": " + e.getMessage() + "\n";
        }

        return "Error en línea " + numeroLinea + ": Sintaxis no reconocida: " + linea + "\n";
    }

    // Métodos de procesamiento para cada estructura

    private static String procesarIf(Matcher matcher, int numeroLinea) throws Exception {
        StringBuilder resultado = new StringBuilder();
        
        // Evaluar condición principal del if
        String condicionPrincipal = matcher.group(1).trim();
        String bloqueIf = matcher.group(2).trim();
        
        if (evaluarCondicion(condicionPrincipal)) {
            resultado.append(procesarBloque(bloqueIf, numeroLinea));
            return resultado.toString();
        }
        
        // Procesar else ifs
        int groupIndex = 3;
        while (groupIndex < matcher.groupCount() - 1) {
            String condicionElseIf = matcher.group(groupIndex);
            String bloqueElseIf = matcher.group(groupIndex + 1);
            
            if (condicionElseIf != null && bloqueElseIf != null && 
                evaluarCondicion(condicionElseIf.trim())) {
                resultado.append(procesarBloque(bloqueElseIf.trim(), numeroLinea));
                return resultado.toString();
            }
            groupIndex += 2;
        }
        
        // Procesar else final
        String bloqueElse = matcher.group(matcher.groupCount() - 1);
        if (bloqueElse != null && !bloqueElse.trim().isEmpty()) {
            resultado.append(procesarBloque(bloqueElse.trim(), numeroLinea));
        }
        
        return resultado.toString();
    }

    private static String procesarWhile(Matcher matcher, int numeroLinea) throws Exception {
        StringBuilder resultado = new StringBuilder();
        String condicion = matcher.group(1).trim();
        String bloque = matcher.group(2).trim();
        
        while (evaluarCondicion(condicion)) {
            resultado.append(procesarBloque(bloque, numeroLinea));
        }
        
        return resultado.toString();
    }

    private static String procesarDoWhile(Matcher matcher, int numeroLinea) throws Exception {
        StringBuilder resultado = new StringBuilder();
        String bloque = matcher.group(1).trim();
        String condicion = matcher.group(2).trim();
        
        do {
            resultado.append(procesarBloque(bloque, numeroLinea));
        } while (evaluarCondicion(condicion));
        
        return resultado.toString();
    }

    private static String procesarFor(Matcher matcher, int numeroLinea) throws Exception {
        StringBuilder resultado = new StringBuilder();
        String inicializacion = matcher.group(1).trim();
        String condicion = matcher.group(2).trim();
        String incremento = matcher.group(3).trim();
        String bloque = matcher.group(4).trim();
        
        // Ejecutar inicialización
        if (!inicializacion.isEmpty()) {
            procesarLinea(inicializacion, numeroLinea);
        }
        
        // Ejecutar bucle
        while (condicion.isEmpty() || evaluarCondicion(condicion)) {
            resultado.append(procesarBloque(bloque, numeroLinea));
            
            // Ejecutar incremento
            if (!incremento.isEmpty()) {
                procesarLinea(incremento, numeroLinea);
            }
        }
        
        return resultado.toString();
    }

    private static String procesarIncremento(Matcher matcher) throws Exception {
        String operadorPrefijo = matcher.group(1);
        String variable = matcher.group(2);
        String operadorSufijo = matcher.group(3);
        
        if (!variables.containsKey(variable)) {
            throw new Exception("Variable no definida: " + variable);
        }
        
        Object valorActual = variables.get(variable);
        if (!(valorActual instanceof Number)) {
            throw new Exception("Incremento/decremento solo aplicable a números");
        }
        
        double valor = ((Number) valorActual).doubleValue();
        double nuevoValor = valor;
        
        if ("++".equals(operadorPrefijo) || "++".equals(operadorSufijo)) {
            nuevoValor = valor + 1;
        } else if ("--".equals(operadorPrefijo) || "--".equals(operadorSufijo)) {
            nuevoValor = valor - 1;
        }
        
        // Convertir a entero si es posible
        Object valorFinal = (nuevoValor == (int) nuevoValor) ? (int) nuevoValor : nuevoValor;
        variables.put(variable, valorFinal);
        
        // En prefijo, devolver el nuevo valor; en sufijo, devolver el valor original
        if (operadorPrefijo != null) {
            return valorFinal.toString();
        } else {
            return valorActual.toString();
        }
    }

    private static String procesarBloque(String bloque, int numeroLinea) throws Exception {
        StringBuilder resultado = new StringBuilder();
        String[] instrucciones = dividirInstrucciones(bloque);
        
        for (String instruccion : instrucciones) {
            if (!instruccion.trim().isEmpty()) {
                String res = procesarLinea(instruccion.trim(), numeroLinea);
                if (res != null && !res.isEmpty()) {
                    resultado.append(res);
                }
            }
        }
        
        return resultado.toString();
    }

    private static String ejecutarFuncion(String nombre, String argumentos, int numeroLinea) throws Exception {
        if (!funciones.containsKey(nombre)) {
            throw new Exception("Función no definida: " + nombre);
        }
        
        Funcion funcion = funciones.get(nombre);
        // En una implementación completa, aquí se procesarían los parámetros y argumentos
        return procesarBloque(funcion.cuerpo, numeroLinea);
    }

    // Métodos de evaluación de expresiones (mejorados)

    private static boolean evaluarCondicion(String condicion) throws Exception {
        condicion = condicion.trim();
        
        // Buscar operadores lógicos
        if (condicion.contains("&&") || condicion.contains("||")) {
            return evaluarExpresionLogica(condicion);
        }
        
        // Buscar operadores de comparación
        for (String op : new String[]{"==", "!=", ">=", "<=", ">", "<"}) {
            if (condicion.contains(op)) {
                return evaluarComparacion(condicion, op);
            }
        }
        
        // Si no hay operador, evaluar como expresión booleana
        Object resultado = evaluarExpresion(condicion);
        if (resultado instanceof Boolean) {
            return (Boolean) resultado;
        }
        
        throw new Exception("Condición inválida: " + condicion);
    }

    private static boolean evaluarExpresionLogica(String expresion) throws Exception {
        // Simplificación: evaluar expresiones lógicas básicas
        if (expresion.contains("&&")) {
            String[] partes = expresion.split("&&", 2);
            return evaluarCondicion(partes[0].trim()) && evaluarCondicion(partes[1].trim());
        } else if (expresion.contains("||")) {
            String[] partes = expresion.split("\\|\\|", 2);
            return evaluarCondicion(partes[0].trim()) || evaluarCondicion(partes[1].trim());
        }
        
        throw new Exception("Expresión lógica inválida: " + expresion);
    }

    private static boolean evaluarComparacion(String expresion, String operador) throws Exception {
        String[] partes = expresion.split("\\s*" + Pattern.quote(operador) + "\\s*", 2);
        if (partes.length != 2) {
            throw new Exception("Comparación inválida: " + expresion);
        }
        
        Object izquierda = evaluarExpresion(partes[0]);
        Object derecha = evaluarExpresion(partes[1]);
        
        // Comparación numérica
        if (izquierda instanceof Number && derecha instanceof Number) {
            double num1 = ((Number) izquierda).doubleValue();
            double num2 = ((Number) derecha).doubleValue();
            
            switch (operador) {
                case "==": return num1 == num2;
                case "!=": return num1 != num2;
                case ">": return num1 > num2;
                case ">=": return num1 >= num2;
                case "<": return num1 < num2;
                case "<=": return num1 <= num2;
            }
        }
        
        // Comparación de objetos
        switch (operador) {
            case "==": return izquierda.equals(derecha);
            case "!=": return !izquierda.equals(derecha);
            default: throw new Exception("Operador " + operador + " no soportado para tipos no numéricos");
        }
    }

    private static Object evaluarExpresion(String expresion) throws Exception {
        expresion = expresion.trim();
        
        // 1) Literales básicos
        if (BOOLEANO.matcher(expresion).matches()) {
            return Boolean.parseBoolean(expresion);
        }
        if (NUMERO.matcher(expresion).matches()) {
            return expresion.contains(".") ? Double.parseDouble(expresion) : Integer.parseInt(expresion);
        }
        if (CADENA.matcher(expresion).matches()) {
            return expresion.substring(1, expresion.length() - 1);
        }
        if (CHAR.matcher(expresion).matches()) {
            return expresion.charAt(1);
        }
        
        // 2) Variables
        if (IDENTIFICADOR_VALIDO.matcher(expresion).matches()) {
            if (!variables.containsKey(expresion)) {
                throw new Exception("Variable no definida: " + expresion);
            }
            return variables.get(expresion);
        }
        
        // 3) Operaciones aritméticas
        if (expresion.contains("+") || expresion.contains("-") || 
            expresion.contains("*") || expresion.contains("/") || expresion.contains("%")) {
            return evaluarOperacionAritmetica(expresion);
        }
        
        // 4) Expresiones entre paréntesis
        if (expresion.startsWith("(") && expresion.endsWith(")")) {
            return evaluarExpresion(expresion.substring(1, expresion.length() - 1));
        }
        
        throw new Exception("Expresión no válida: " + expresion);
    }

    private static Object evaluarOperacionAritmetica(String expresion) throws Exception {
        // Simplificación: evaluar operaciones básicas
        for (String op : new String[]{"+", "-", "*", "/", "%"}) {
            if (expresion.contains(op)) {
                String[] partes = expresion.split("\\s*" + Pattern.quote(op) + "\\s*", 2);
                if (partes.length == 2) {
                    Object izquierda = evaluarExpresion(partes[0]);
                    Object derecha = evaluarExpresion(partes[1]);
                    
                    if (!(izquierda instanceof Number) || !(derecha instanceof Number)) {
                        throw new Exception("Operación aritmética inválida para tipos no numéricos");
                    }
                    
                    double num1 = ((Number) izquierda).doubleValue();
                    double num2 = ((Number) derecha).doubleValue();
                    
                    switch (op) {
                        case "+": return num1 + num2;
                        case "-": return num1 - num2;
                        case "*": return num1 * num2;
                        case "/": 
                            if (num2 == 0) throw new Exception("División por cero");
                            return num1 / num2;
                        case "%": 
                            if (num2 == 0) throw new Exception("Módulo por cero");
                            return num1 % num2;
                    }
                }
            }
        }
        
        throw new Exception("Operación aritmética inválida: " + expresion);
    }

    private static String procesarPrint(String contenido) throws Exception {
        // Verificar si es cadena formateada
        Matcher matcherFormato = CADENA_FORMATEADA.matcher(contenido);
        if (matcherFormato.matches()) {
            String contenidoFormateado = matcherFormato.group(1);
            return procesarCadenaFormateada(contenidoFormateado);
        }
        
        // Evaluar como expresión normal
        Object resultado = evaluarExpresion(contenido);
        return resultado != null ? resultado.toString() : "null";
    }

    private static String procesarCadenaFormateada(String contenido) throws Exception {
        Matcher matcher = EXPRESION_INTERPOLADA.matcher(contenido);
        StringBuffer resultado = new StringBuffer();
        
        while (matcher.find()) {
            String expresion = matcher.group(1);
            Object valor = evaluarExpresion(expresion.trim());
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valor.toString()));
        }
        matcher.appendTail(resultado);
        
        return resultado.toString();
    }

    // Métodos auxiliares

    private static String removerComentarios(String texto) {
        // Eliminar comentarios de una línea (// y #)
        texto = texto.replaceAll("//.*|#.*", "");
        // Eliminar comentarios multilínea (/* ... */)
        texto = texto.replaceAll("/\\*.*?\\*/", "");
        return texto;
    }

    private static String[] dividirInstrucciones(String bloque) {
        bloque = removerComentarios(bloque);
        return bloque.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    }

    public static void limpiarVariables() {
        variables.clear();
        funciones.clear();
    }

    // Clase interna para representar funciones
    private static class Funcion {
        String nombre;
        String parametros;
        String cuerpo;
        
        Funcion(String nombre, String parametros, String cuerpo) {
            this.nombre = nombre;
            this.parametros = parametros;
            this.cuerpo = cuerpo;
        }
    }
}