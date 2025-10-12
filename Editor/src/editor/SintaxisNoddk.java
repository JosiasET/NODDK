package editor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// el bueno xdN
public class SintaxisNoddk {
    private static final Pattern IDENTIFICADOR_VALIDO = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern NUMERO = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");
    private static final Pattern CADENA = Pattern.compile("^\"(.*?)\"$");
    private static final Pattern CARACTER = Pattern.compile("^'.'$");
    private static final Pattern BOOLEANO = Pattern.compile("^(true|false)$");
    private static final Pattern DECLARACION = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+?);?$");
    private static final Pattern OPERACION_ARITMETICA = Pattern
            .compile("([a-zA-Z_][a-zA-Z0-9_]*|\\d+\\.?\\d*)\\s*([+\\-*/%])\\s*([a-zA-Z_][a-zA-Z0-9_]*|\\d+\\.?\\d*)");
    // Permite:
    //  - print("texto")
    //  - print(f"texto {var}")
    //  - print(expresión)
    private static final Pattern PRINT = Pattern.compile(
            "^print\\((f?)\"(.*?)\"\\);?$|^print\\(([^)]+)\\);?$");
    private static final Pattern IF = Pattern
            .compile("^if\\s*\\((.+?)\\)\\s*\\{(.*?)\\}(?:\\s*else\\s*\\{(.*?)\\})?\\s*$", Pattern.DOTALL);
    private static final Pattern ELSE_IF = Pattern.compile("^else\\s+if\\s*\\((.+?)\\)\\s*\\{(.*?)\\}\\s*$",
            Pattern.DOTALL);
    private static final Pattern WHILE = Pattern.compile("^while\\s*\\((.+?)\\)\\s*\\{(.*?)\\}\\s*$", Pattern.DOTALL);
    private static final Pattern DO_WHILE = Pattern.compile("^do\\s*\\{(.*?)\\}\\s*while\\s*\\((.+?)\\)\\s*;?\\s*$",
            Pattern.DOTALL);
    private static final Pattern FOR = Pattern
            .compile("^for\\s*\\(\\s*(.*?)\\s*;\\s*(.*?)\\s*;\\s*(.*?)\\s*\\)\\s*\\{(.*?)\\}\\s*$", Pattern.DOTALL);
    private static final Pattern VARIABLE_INTERPOLACION = Pattern.compile("\\{(\\w+)\\}");

    private static final Map<String, Object> variables = new HashMap<>();

    public static String procesarLinea(String linea) {
        return procesarLinea(linea, 1);
    }

    public static String procesarLinea(String linea, int numeroLinea) {
        StringBuilder resultado = new StringBuilder();
        linea = linea.trim();

        // Eliminar comentarios multilínea
        linea = removerComentariosMultilinea(linea).trim();
        // Eliminar comentarios en línea
        int indexComentario = linea.indexOf("//");
        if (indexComentario != -1) {
            linea = linea.substring(0, indexComentario).trim();
        }
        if (linea.isEmpty()) {
            return "";
        }

        // 1) Declaración de variable: identificador = expresión;
        Matcher matcherDeclaracion = DECLARACION.matcher(linea);
        if (matcherDeclaracion.matches()) {
            String identificador = matcherDeclaracion.group(1);
            String valor = matcherDeclaracion.group(2);
            try {
                Object valorObjeto = evaluarExpresion(valor);
                if (valorObjeto == null) {
                    throw new Exception("Valor no válido para la variable.");
                }
                variables.put(identificador, valorObjeto);
                return "";
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        // 2) Estructura if / else
        Matcher matcherIf = IF.matcher(linea);
        if (matcherIf.matches()) {
            String condicion = matcherIf.group(1).trim();
            String bloqueIf = matcherIf.group(2).trim();
            String bloqueElse = matcherIf.group(3) != null ? matcherIf.group(3).trim() : null;
            try {
                boolean condicionEvaluada = evaluarCondicion(condicion);
                String bloqueEjecutar = condicionEvaluada ? bloqueIf : (bloqueElse != null ? bloqueElse : "");
                if (!bloqueEjecutar.isEmpty()) {
                    String[] instrucciones = dividirInstrucciones(bloqueEjecutar);
                    for (String instruccion : instrucciones) {
                        String res = procesarLinea(instruccion.trim(), numeroLinea);
                        if (!res.isEmpty()) {
                            resultado.append(res).append("\n");
                        }
                    }
                }
                return resultado.toString().trim();
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        // 3) Estructura else if (solo aislada)
        Matcher matcherElseIf = ELSE_IF.matcher(linea);
        if (matcherElseIf.matches()) {
            String condicion = matcherElseIf.group(1).trim();
            String bloque = matcherElseIf.group(2).trim();
            try {
                if (evaluarCondicion(condicion)) {
                    String[] instrucciones = dividirInstrucciones(bloque);
                    for (String instruccion : instrucciones) {
                        String res = procesarLinea(instruccion.trim(), numeroLinea);
                        if (!res.isEmpty()) {
                            resultado.append(res).append("\n");
                        }
                    }
                }
                return resultado.toString().trim();
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        // 4) Bucle while
        Matcher matcherWhile = WHILE.matcher(linea);
        if (matcherWhile.matches()) {
            String condicion = matcherWhile.group(1).trim();
            String bloque = matcherWhile.group(2).trim();
            try {
                while (evaluarCondicion(condicion)) {
                    String[] instrucciones = dividirInstrucciones(bloque);
                    for (String instruccion : instrucciones) {
                        String res = procesarLinea(instruccion.trim(), numeroLinea);
                        if (!res.isEmpty()) {
                            resultado.append(res).append("\n");
                        }
                    }
                }
                return resultado.toString().trim();
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        // 5) Bucle do-while
        Matcher matcherDoWhile = DO_WHILE.matcher(linea);
        if (matcherDoWhile.matches()) {
            String bloque = matcherDoWhile.group(1).trim();
            String condicion = matcherDoWhile.group(2).trim();
            try {
                do {
                    String[] instrucciones = dividirInstrucciones(bloque);
                    for (String instruccion : instrucciones) {
                        String res = procesarLinea(instruccion.trim(), numeroLinea);
                        if (!res.isEmpty()) {
                            resultado.append(res).append("\n");
                        }
                    }
                } while (evaluarCondicion(condicion));
                return resultado.toString().trim();
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        // 6) Bucle for
        Matcher matcherFor = FOR.matcher(linea);
        if (matcherFor.matches()) {
            String inicializacion = matcherFor.group(1).trim();
            String condicion = matcherFor.group(2).trim();
            String incremento = matcherFor.group(3).trim();
            String bloque = matcherFor.group(4).trim();
            try {
                procesarLinea(inicializacion, numeroLinea); // e.g., i = 0
                while (evaluarCondicion(condicion)) {
                    String[] instrucciones = dividirInstrucciones(bloque);
                    for (String instruccion : instrucciones) {
                        String res = procesarLinea(instruccion.trim(), numeroLinea);
                        if (!res.isEmpty()) {
                            resultado.append(res).append("\n");
                        }
                    }
                    procesarLinea(incremento, numeroLinea); // e.g., i = i + 1
                }
                return resultado.toString().trim();
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        // 7) Print: cadenas con f-string o variable / expresión simple
        Matcher matcherPrint = PRINT.matcher(linea);
        if (matcherPrint.matches()) {
            try {
                // Caso 1: print(f"texto con {var}")
                if (matcherPrint.group(2) != null) {
                    boolean esFormato = matcherPrint.group(1) != null && !matcherPrint.group(1).isEmpty();
                    String contenido = matcherPrint.group(2);
                    if (esFormato) {
                        // Verificar que hay al menos una variable entre llaves
                        Matcher matcherVar = VARIABLE_INTERPOLACION.matcher(contenido);
                        if (!matcherVar.find()) {
                            throw new Exception("f-string requiere al menos una variable entre llaves");
                        }
                        // Verificar que todas las variables existen
                        matcherVar.reset();
                        while (matcherVar.find()) {
                            String nombreVariable = matcherVar.group(1);
                            if (!variables.containsKey(nombreVariable)) {
                                throw new Exception("Variable no definida: " + nombreVariable);
                            }
                        }
                    }
                    return procesarPrint(contenido, esFormato);
                }
                // Caso 2: print(expresión o variable)
                else if (matcherPrint.group(3) != null) {
                    Object val = evaluarExpresion(matcherPrint.group(3).trim());
                    if (val == null) {
                        throw new Exception("Valor para imprimir es null o inválido");
                    }
                    return val.toString();
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage().trim();
                if (!errorMessage.isEmpty()) {
                    return "Error en línea " + numeroLinea + ": " + errorMessage + "\n";
                }
                return "";
            }
        }

        // Si no coincide con ninguna sintaxis conocida:
        return "Error en línea " + numeroLinea + ": Sintaxis no reconocida: " + linea.trim() + "\n";
    }

    // Elimina comentarios multilínea /* ... */
    private static String removerComentariosMultilinea(String texto) {
        return texto.replaceAll("/\\*.*?\\*/", "");
    }

    // Divide un bloque (entre llaves) en instrucciones separadas por ';' fuera de cadenas
    private static String[] dividirInstrucciones(String bloque) {
        bloque = removerComentariosMultilinea(bloque).replaceAll("//.*", "");
        return bloque.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    }

    // Evalúa una condición booleana (comparaciones o variable booleana)
    private static boolean evaluarCondicion(String condicion) throws Exception {
        condicion = condicion.trim();
        String[] operadores = { "==", "!=", ">=", "<=", ">", "<" };

        for (String op : operadores) {
            if (condicion.contains(op)) {
                String[] partes = condicion.split("\\s*" + Pattern.quote(op) + "\\s*");
                if (partes.length == 2) {
                    Object valor1 = evaluarExpresion(partes[0]);
                    Object valor2 = evaluarExpresion(partes[1]);

                    // Si ambos son números, comparar numéricamente
                    if (valor1 instanceof Number && valor2 instanceof Number) {
                        double num1 = ((Number) valor1).doubleValue();
                        double num2 = ((Number) valor2).doubleValue();
                        return switch (op) {
                            case "==" -> num1 == num2;
                            case "!=" -> num1 != num2;
                            case ">" -> num1 > num2;
                            case ">=" -> num1 >= num2;
                            case "<" -> num1 < num2;
                            case "<=" -> num1 <= num2;
                            default -> false;
                        };
                    }
                    // Si no son números, comparar como objetos (String o Boolean)
                    else if (valor1 != null && valor2 != null) {
                        switch (op) {
                            case "==" -> {
                                return valor1.equals(valor2);
                            }
                            case "!=" -> {
                                return !valor1.equals(valor2);
                            }
                            default -> throw new Exception("Operador " + op + " no soportado para tipos no numéricos");
                        }
                    }
                }
            }
        }

        // Si no hay comparador, la condición puede ser una expresión booleana directa
        Object valor = evaluarExpresion(condicion);
        if (valor instanceof Boolean) {
            return (Boolean) valor;
        }

        throw new Exception("Condición inválida: " + condicion);
    }

    // Evalúa una expresión numérica, de cadena, booleano o variable
    private static Object evaluarExpresion(String expresion) throws Exception {
        expresion = expresion.trim();
        Matcher matcher = OPERACION_ARITMETICA.matcher(expresion);

        // Caso operación aritmética simple (a + b, x * 2, etc.)
        if (matcher.matches()) {
            String operador = matcher.group(2);
            Object valor1 = convertirTipo(matcher.group(1));
            Object valor2 = convertirTipo(matcher.group(3));

            if (!(valor1 instanceof Number) || !(valor2 instanceof Number)) {
                throw new Exception("Operación aritmética inválida");
            }

            double num1 = ((Number) valor1).doubleValue();
            double num2 = ((Number) valor2).doubleValue();

            return switch (operador) {
                case "+" -> num1 + num2;
                case "-" -> num1 - num2;
                case "*" -> num1 * num2;
                case "/" -> {
                    if (num2 == 0) throw new Exception("División por cero");
                    yield num1 / num2;
                }
                case "%" -> {
                    if (num2 == 0) throw new Exception("Módulo por cero");
                    yield num1 % num2;
                }
                default -> throw new Exception("Operador no soportado");
            };
        }

        // Si no es operación aritmética, convertir tipo directo
        return convertirTipo(expresion);
    }

    // Convierte un literal o variable a su tipo adecuado (Integer, Double, String, Boolean, Character)
    private static Object convertirTipo(String valor) throws Exception {
        if (valor == null) return null;
        valor = valor.trim();

        // 1) Literal booleano
        if (BOOLEANO.matcher(valor).matches()) {
            return Boolean.parseBoolean(valor);
        }

        // 2) Literal numérico
        if (NUMERO.matcher(valor).matches()) {
            return valor.contains(".") ? Double.parseDouble(valor) : Integer.parseInt(valor);
        }

        // 3) Literal cadena
        Matcher matcherCadena = CADENA.matcher(valor);
        if (matcherCadena.matches()) {
            return matcherCadena.group(1);
        }

        // 4) Literal carácter
        Matcher matcherCaracter = CARACTER.matcher(valor);
        if (matcherCaracter.matches()) {
            return valor.charAt(1);
        }

        // 5) Variable definida
        if (IDENTIFICADOR_VALIDO.matcher(valor).matches()) {
            if (!variables.containsKey(valor)) {
                throw new Exception("Variable no definida: " + valor);
            }
            return variables.get(valor);
        }

        throw new Exception("Expresión no válida: " + valor);
    }

    // Procesa print con o sin formato. Si esFormato==true, reemplaza {var} por su valor.
    private static String procesarPrint(String contenido, boolean esFormato) throws Exception {
        if (!esFormato) {
            // Sin interpolación: se devuelve tal cual el contenido
            return contenido;
        }

        Matcher matcher = VARIABLE_INTERPOLACION.matcher(contenido);
        StringBuffer resultado = new StringBuffer();
        while (matcher.find()) {
            String nombreVariable = matcher.group(1);
            if (!variables.containsKey(nombreVariable)) {
                throw new Exception("Variable no definida: " + nombreVariable);
            }
            Object valorVariable = variables.get(nombreVariable);
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valorVariable.toString()));
        }
        matcher.appendTail(resultado);
        return resultado.toString();
    }

    // Limpia todas las variables definidas
    public static void limpiarVariables() {
        variables.clear();
    }
}
