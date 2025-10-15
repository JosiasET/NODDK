import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analizador Semántico para el lenguaje Noddk
 * Realiza verificaciones de tipos, límites y reglas semánticas
 */
public class SemanticAnalyzer {
    
    // Tabla de símbolos para almacenar variables y sus información
    private Map<String, VariableInfo> symbolTable;
    private Set<String> errors;
    
    // Límites de tipos de datos según especificación
    private static final int INT_MIN = -2147483648;
    private static final int INT_MAX = 2147483647;
    private static final float FLOAT_MIN = 1.4e-45f;
    private static final float FLOAT_MAX = 3.4e38f;
    private static final char CHAR_MIN = '\u0000';
    private static final char CHAR_MAX = '\uffff';
    private static final int STRING_MAX_LENGTH = 2147483647; // Límite teórico de Java
    
    /**
     * Información de una variable en la tabla de símbolos
     */
    public static class VariableInfo {
        public String type;      // Tipo de dato: int, float, char, boolean, String
        public Object value;     // Valor actual de la variable
        public int line;         // Línea donde fue declarada
        public boolean isConstant; // Si es una constante
        
        public VariableInfo(String type, Object value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.isConstant = false;
        }
        
        public VariableInfo(String type, Object value, int line, boolean isConstant) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.isConstant = isConstant;
        }
        
        @Override
        public String toString() {
            return "VariableInfo{type='" + type + "', value=" + value + ", line=" + line + ", constant=" + isConstant + "}";
        }
    }
    
    // Constructor
    public SemanticAnalyzer() {
        this.symbolTable = new HashMap<>();
        this.errors = new HashSet<>();
    }
    
    // ==================== REGLA 1: DETECCIÓN DE TIPOS Y LÍMITES ====================
    
    /**
     * Infiere el tipo de dato basado en el valor literal
     * REGLA: Detección automática de tipos con verificación de límites
     */
    public String inferTypeFromValue(Object value, int line) {
        if (value instanceof Integer) {
            return inferIntegerType((Integer) value, line);
        } else if (value instanceof Float) {
            return inferFloatType((Float) value, line);
        } else if (value instanceof Character) {
            checkCharLimits((Character) value, line);
            return "char";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else if (value instanceof String) {
            checkStringLimits((String) value, line);
            return "String";
        }
        return "unknown";
    }
    
    /**
     * Infiere tipo para enteros verificando límites de int
     * REGLA: Los enteros deben estar entre -2,147,483,648 y 2,147,483,647
     */
    private String inferIntegerType(int value, int line) {
        if (value >= INT_MIN && value <= INT_MAX) {
            return "int";
        } else {
            addError("Línea " + line + ": Valor entero fuera de rango: " + value + 
                    ". Límites: [" + INT_MIN + ", " + INT_MAX + "]");
            return "error";
        }
    }
    
    /**
     * Infiere tipo para flotantes verificando límites de float
     * REGLA: Los floats deben estar entre ~1.4e-45 y ~3.4e38
     */
    private String inferFloatType(float value, int line) {
        float absValue = Math.abs(value);
        if (absValue == 0 || (absValue >= FLOAT_MIN && absValue <= FLOAT_MAX)) {
            return "float";
        } else {
            addError("Línea " + line + ": Valor float fuera de rango: " + value + 
                    ". Límites: [" + FLOAT_MIN + ", " + FLOAT_MAX + "]");
            return "error";
        }
    }
    
    /**
     * Verifica límites para caracteres
     * REGLA: Chars entre '\u0000' y '\uffff'
     */
    private void checkCharLimits(char value, int line) {
        if (value < CHAR_MIN || value > CHAR_MAX) {
            addError("Línea " + line + ": Valor char fuera de rango: '" + value + "'");
        }
    }
    
    /**
     * Verifica límites para strings
     * REGLA: Strings no pueden exceder longitud máxima
     */
    private void checkStringLimits(String value, int line) {
        if (value.length() > STRING_MAX_LENGTH) {
            addError("Línea " + line + ": String excede longitud máxima: " + value.length());
        }
    }
    
    /**
     * Verifica que un valor esté dentro de los límites de su tipo
     */
    private void checkValueLimits(String type, Object value, int line) {
        switch (type) {
            case "int":
                if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue < INT_MIN || intValue > INT_MAX) {
                        addError("Línea " + line + ": Valor int fuera de rango: " + intValue);
                    }
                }
                break;
            case "float":
                if (value instanceof Float) {
                    float floatValue = (Float) value;
                    float absValue = Math.abs(floatValue);
                    if (absValue != 0 && (absValue < FLOAT_MIN || absValue > FLOAT_MAX)) {
                        addError("Línea " + line + ": Valor float fuera de rango: " + floatValue);
                    }
                }
                break;
            case "char":
                if (value instanceof Character) {
                    checkCharLimits((Character) value, line);
                }
                break;
            case "String":
                if (value instanceof String) {
                    checkStringLimits((String) value, line);
                }
                break;
        }
    }
    
    // ==================== REGLA 2: DECLARACIÓN DE VARIABLES ====================
    
    /**
     * Verifica una declaración de variable
     * REGLA: No se pueden duplicar variables (nombres únicos)
     * REGLA: Nombres de variables válidos
     */
    public boolean checkDeclaration(String identifier, Object value, int line) {
        // REGLA: Verificar nombre de variable válido
        if (!isValidIdentifier(identifier)) {
            addError("Línea " + line + ": Nombre de variable inválido: '" + identifier + "'");
            return false;
        }
        
        // REGLA: No se pueden usar palabras reservadas
        if (isReservedWord(identifier)) {
            addError("Línea " + line + ": No se puede usar palabra reservada: '" + identifier + "'");
            return false;
        }
        
        // Verificar si la variable ya existe
        if (symbolTable.containsKey(identifier)) {
            addError("Línea " + line + ": Variable '" + identifier + "' ya está declarada");
            return false;
        }
        
        // Inferir tipo y verificar límites
        String inferredType = inferTypeFromValue(value, line);
        if (inferredType.equals("error")) {
            return false;
        }
        
        checkValueLimits(inferredType, value, line);
        
        // Agregar a tabla de símbolos
        symbolTable.put(identifier, new VariableInfo(inferredType, value, line));
        System.out.println("✓ Variable declarada: " + identifier + " = " + value + " (" + inferredType + ")");
        return true;
    }
    
    /**
     * REGLA: Declaración de constantes (no pueden reasignarse)
     */
    public boolean checkConstantDeclaration(String identifier, Object value, int line) {
        if (!checkDeclaration(identifier, value, line)) {
            return false;
        }
        
        // Marcar como constante
        symbolTable.get(identifier).isConstant = true;
        System.out.println("✓ Constante declarada: " + identifier + " = " + value);
        return true;
    }
    
    // ==================== REGLA 3: ASIGNACIÓN DE VARIABLES ====================
    
    /**
     * Verifica una asignación de variable
     * REGLA: La variable debe existir previamente
     * REGLA: Compatibilidad de tipos en asignación
     * REGLA: Constantes no pueden reasignarse
     */
    public boolean checkAssignment(String identifier, Object value, int line) {
        // Verificar existencia de la variable
        if (!symbolTable.containsKey(identifier)) {
            addError("Línea " + line + ": Variable '" + identifier + "' no declarada");
            return false;
        }
        
        VariableInfo varInfo = symbolTable.get(identifier);
        
        // REGLA: Constantes no pueden reasignarse
        if (varInfo.isConstant) {
            addError("Línea " + line + ": No se puede reasignar constante '" + identifier + "'");
            return false;
        }
        
        String inferredType = inferTypeFromValue(value, line);
        
        if (inferredType.equals("error")) {
            return false;
        }
        
        // REGLA: Compatibilidad de tipos con promoción numérica
        if (!checkTypeCompatibility(varInfo.type, inferredType, line)) {
            addError("Línea " + line + ": Incompatibilidad de tipos. Esperado: " + 
                    varInfo.type + ", Obtenido: " + inferredType);
            return false;
        }
        
        // Verificar límites del nuevo valor
        checkValueLimits(varInfo.type, value, line);
        
        // Actualizar valor en tabla de símbolos
        varInfo.value = value;
        System.out.println("✓ Variable asignada: " + identifier + " = " + value);
        return true;
    }
    
    /**
     * Verifica compatibilidad entre tipos para asignación
     * REGLA: Permite promoción numérica (int → float)
     */
    private boolean checkTypeCompatibility(String expected, String actual, int line) {
        // Mismo tipo
        if (expected.equals(actual)) {
            return true;
        }
        
        // REGLA: Promoción numérica permitida
        if (expected.equals("float") && actual.equals("int")) {
            return true; // int puede promocionar a float
        }
        
        // REGLA: Char puede asignarse a int (valor ASCII)
        if (expected.equals("int") && actual.equals("char")) {
            return true;
        }
        
        return false;
    }
    
    // ==================== REGLA 4: OPERACIONES ARITMÉTICAS ====================
    
    /**
     * Verifica una operación binaria entre dos valores
     * REGLA: No se pueden operar tipos incompatibles
     * REGLA: No se puede operar números con strings (excepto concatenación)
     * REGLA: No división por cero
     * REGLA: Prioridades aritméticas
     */
    public Object checkBinaryOperation(Object left, Object right, String operator, int line) {
        String leftType = inferTypeFromValue(left, line);
        String rightType = inferTypeFromValue(right, line);
        
        // REGLA ESPECIAL: Concatenación de strings
        if (operator.equals("+") && (leftType.equals("String") || rightType.equals("String"))) {
            return concatenateValues(left, right, line);
        }
        
        // REGLA: No se pueden operar booleanos en operaciones aritméticas
        if (leftType.equals("boolean") || rightType.equals("boolean")) {
            addError("Línea " + line + ": No se pueden operar booleanos en operaciones aritméticas");
            return null;
        }
        
        // REGLA: Solo operaciones entre tipos compatibles
        if (!areOperableTypes(leftType, rightType)) {
            addError("Línea " + line + ": No se puede operar " + leftType + " con " + rightType);
            return null;
        }
        
        // REGLA: División por cero
        if ((operator.equals("/") || operator.equals("%")) && isZero(right)) {
            addError("Línea " + line + ": División por cero no permitida");
            return null;
        }
        
        // Realizar operación
        return performOperation(left, right, operator, leftType, rightType, line);
    }
    
    /**
     * REGLA: Concatenación de strings
     */
    private Object concatenateValues(Object left, Object right, int line) {
        String leftStr = left.toString();
        String rightStr = right.toString();
        String result = leftStr + rightStr;
        
        checkStringLimits(result, line);
        return result;
    }
    
    /**
     * Verifica si tipos son operables (números o chars)
     */
    private boolean areOperableTypes(String type1, String type2) {
        Set<String> numericTypes = Set.of("int", "float", "char");
        return numericTypes.contains(type1) && numericTypes.contains(type2);
    }
    
    /**
     * Verifica si un tipo es numérico
     */
    private boolean isNumericType(String type) {
        return type.equals("int") || type.equals("float") || type.equals("char");
    }
    
    /**
     * Verifica si un valor es cero
     */
    private boolean isZero(Object value) {
        if (value instanceof Integer) return (Integer) value == 0;
        if (value instanceof Float) return (Float) value == 0.0f;
        if (value instanceof Character) return (Character) value == 0;
        return false;
    }
    
    /**
     * Realiza la operación aritmética verificando resultados
     */
    private Object performOperation(Object left, Object right, String operator, 
                                   String leftType, String rightType, int line) {
        try {
            // Determinar tipo resultante (promoción numérica)
            String resultType = getResultType(leftType, rightType);
            
            switch (operator) {
                case "+":
                    return addValues(left, right, resultType, line);
                case "-":
                    return subtractValues(left, right, resultType, line);
                case "*":
                    return multiplyValues(left, right, resultType, line);
                case "/":
                    return divideValues(left, right, resultType, line);
                case "%":
                    return moduloValues(left, right, resultType, line);
                case "==": case "!=": case "<": case ">": case "<=": case ">=":
                    return compareValues(left, right, operator, resultType, line);
                case "&&": case "||":
                    return logicalOperation(left, right, operator, line);
                default:
                    addError("Línea " + line + ": Operador no soportado: " + operator);
                    return null;
            }
        } catch (ArithmeticException e) {
            addError("Línea " + line + ": Error aritmético: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * REGLA: Determina tipo resultante de operación (promoción numérica)
     */
    private String getResultType(String type1, String type2) {
        // Si alguno es float, resultado es float
        if (type1.equals("float") || type2.equals("float")) {
            return "float";
        }
        // Si alguno es int, resultado es int
        if (type1.equals("int") || type2.equals("int")) {
            return "int";
        }
        // Ambos char, resultado es int
        if (type1.equals("char") && type2.equals("char")) {
            return "int";
        }
        return type1;
    }
    
    // ==================== OPERACIONES ARITMÉTICAS ESPECÍFICAS ====================
    
    private Object addValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float result = getFloatValue(left) + getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else {
            int result = getIntValue(left) + getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
    }
    
    private Object subtractValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float result = getFloatValue(left) - getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else {
            int result = getIntValue(left) - getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
    }
    
    private Object multiplyValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float result = getFloatValue(left) * getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else {
            int result = getIntValue(left) * getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
    }
    
    private Object divideValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float divisor = getFloatValue(right);
            if (divisor == 0.0f) {
                addError("Línea " + line + ": División por cero");
                return null;
            }
            float result = getFloatValue(left) / divisor;
            checkValueLimits("float", result, line);
            return result;
        } else {
            int divisor = getIntValue(right);
            if (divisor == 0) {
                addError("Línea " + line + ": División por cero");
                return null;
            }
            int result = getIntValue(left) / divisor;
            checkValueLimits("int", result, line);
            return result;
        }
    }
    
    private Object moduloValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float divisor = getFloatValue(right);
            if (divisor == 0.0f) {
                addError("Línea " + line + ": Módulo por cero");
                return null;
            }
            float result = getFloatValue(left) % getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else {
            int divisor = getIntValue(right);
            if (divisor == 0) {
                addError("Línea " + line + ": Módulo por cero");
                return null;
            }
            int result = getIntValue(left) % getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
    }
    
    private Object compareValues(Object left, Object right, String operator, String resultType, int line) {
        if (resultType.equals("float")) {
            float leftVal = getFloatValue(left);
            float rightVal = getFloatValue(right);
            
            switch (operator) {
                case "==": return leftVal == rightVal;
                case "!=": return leftVal != rightVal;
                case "<": return leftVal < rightVal;
                case ">": return leftVal > rightVal;
                case "<=": return leftVal <= rightVal;
                case ">=": return leftVal >= rightVal;
                default: return false;
            }
        } else {
            int leftVal = getIntValue(left);
            int rightVal = getIntValue(right);
            
            switch (operator) {
                case "==": return leftVal == rightVal;
                case "!=": return leftVal != rightVal;
                case "<": return leftVal < rightVal;
                case ">": return leftVal > rightVal;
                case "<=": return leftVal <= rightVal;
                case ">=": return leftVal >= rightVal;
                default: return false;
            }
        }
    }
    
    private Object logicalOperation(Object left, Object right, String operator, int line) {
        if (!(left instanceof Boolean) || !(right instanceof Boolean)) {
            addError("Línea " + line + ": Operadores lógicos solo aplican a booleanos");
            return null;
        }
        
        boolean leftBool = (Boolean) left;
        boolean rightBool = (Boolean) right;
        
        switch (operator) {
            case "&&": return leftBool && rightBool;
            case "||": return leftBool || rightBool;
            default: return false;
        }
    }
    
    // ==================== REGLAS DE VALIDACIÓN ====================
    
    /**
     * REGLA: Identificadores válidos (letras, números, _, no empezar con número)
     */
    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        
        // No puede empezar con número
        if (Character.isDigit(identifier.charAt(0))) {
            return false;
        }
        
        // Solo letras, números y _
        for (char c : identifier.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * REGLA: Palabras reservadas no pueden usarse como identificadores
     */
    private boolean isReservedWord(String identifier) {
        Set<String> reservedWords = Set.of(
            "if", "else", "while", "for", "do", "break", "return", 
            "function", "true", "false", "input", "print", "println"
        );
        return reservedWords.contains(identifier);
    }
    
    /**
     * REGLA: Verificar uso de variable no declarada
     */
    public boolean checkVariableUsage(String identifier, int line) {
        if (!symbolTable.containsKey(identifier)) {
            addError("Línea " + line + ": Variable no declarada '" + identifier + "'");
            return false;
        }
        return true;
    }
    
    // ==================== FUNCIONES AUXILIARES ====================
    
    private float getFloatValue(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Character) return (float) (Character) value;
        return 0.0f;
    }
    
    private int getIntValue(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Float) return ((Float) value).intValue();
        if (value instanceof Character) return (int) (Character) value;
        return 0;
    }
    
    private void addError(String error) {
        errors.add(error);
        System.err.println("❌ " + error);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("✓ No hay errores semánticos");
        } else {
            System.out.println("\n=== ERRORES SEMÁNTICOS ENCONTRADOS (" + errors.size() + ") ===");
            for (String error : errors) {
                System.out.println(error);
            }
        }
    }
    
    public void printSymbolTable() {
        System.out.println("\n=== TABLA DE SÍMBOLOS ===");
        if (symbolTable.isEmpty()) {
            System.out.println("Vacía");
        } else {
            for (Map.Entry<String, VariableInfo> entry : symbolTable.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
        }
    }
    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    public VariableInfo getVariableInfo(String identifier) {
        return symbolTable.get(identifier);
    }
    
    public boolean variableExists(String identifier) {
        return symbolTable.containsKey(identifier);
    }
    
    public void clear() {
        symbolTable.clear();
        errors.clear();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
}