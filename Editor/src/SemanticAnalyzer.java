import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Analizador Semántico para el lenguaje Noddk
 * Realiza verificaciones de tipos, límites y reglas semánticas
 */
public class SemanticAnalyzer {
    
    // Tabla de símbolos para almacenar variables y sus información
    private Map<String, VariableInfo> symbolTable;
    private Set<String> errors;
    
    
    // Nuevas estructuras para reglas adicionales
    private Set<String> initializedVariables;
    private Stack<String> scopeStack;
    private Map<String, Map<String, VariableInfo>> scopeSymbolTables;
    private int loopComplexity;
    private boolean hasReturnStatement;
    private ErrorManager errorManager;
    
    // Límites de tipos de datos según especificación
    private static final int INT_MIN = -2147483648;
    private static final int INT_MAX = 2147483647;
    private static final float FLOAT_MIN = 1.4e-45f;
    private static final float FLOAT_MAX = 3.4e38f;
    private static final char CHAR_MIN = '\u0000';
    private static final char CHAR_MAX = '\uffff';
    private static final int STRING_MAX_LENGTH = 2147483647;
    
    /**
     * Información de una variable en la tabla de símbolos
     */
    public static class VariableInfo {
        public String type;
        public Object value;
        public int line;
        public boolean isConstant;
        
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
            return String.format("%-10s %-15s %-8s %s", 
                type, value, isConstant ? "CONST" : "VAR", "Línea " + line);
        }
    }
    
    // Constructor
    public SemanticAnalyzer(ErrorManager errorManager) {
        this.symbolTable = new HashMap<>();
        this.errors = new HashSet<>();
        this.initializedVariables = new HashSet<>();
        this.scopeStack = new Stack<>();
        this.scopeSymbolTables = new HashMap<>();
        this.loopComplexity = 0;
        this.hasReturnStatement = false;
        this.errorManager = errorManager;
        
        // Inicializar scope global
        enterScope("global");
    }
    
    // ==================== REGLAS DE ALCANCE (SCOPE) ====================
    
    public void enterScope(String scopeName) {
        scopeStack.push(scopeName);
        scopeSymbolTables.put(scopeName, new HashMap<>());
    }

   
    
    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            String scopeName = scopeStack.pop();
            scopeSymbolTables.remove(scopeName);
        }
    }
    
    public boolean checkScopeAccess(String identifier, int line) {
        // Buscar en scopes desde el más interno al más externo
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            String currentScope = scopeStack.get(i);
            Map<String, VariableInfo> scopeTable = scopeSymbolTables.get(currentScope);
            if (scopeTable.containsKey(identifier)) {
                return true;
            }
        }
        
        addError("Línea " + line + ": Variable '" + identifier + "' fuera de alcance o no declarada");
        return false;
    }
    
    // ==================== REGLA 1: DETECCIÓN DE TIPOS Y LÍMITES ====================
    
    public String inferTypeFromValue(Object value, int line) {
        if (value instanceof Integer) {
            return "int";
        } else if (value instanceof Float || value instanceof Double) {
            return "float";
        } else if (value instanceof Character) {
            checkCharLimits((Character) value, line);
            return "char";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else if (value instanceof String) {
            checkStringLimits((String) value, line);
            return "String";
        }
        addError("Línea " + line + ": Tipo de dato no reconocido: " + value);
        return "error";
    }
    
    private String inferIntegerType(String value, int line) {
        try {
            long longValue = Long.parseLong(value);
            if (longValue >= INT_MIN && longValue <= INT_MAX) {
                return "int";
            } else {
                addError("Línea " + line + ": Valor entero fuera de rango: " + value);
                return "error";
            }
        } catch (NumberFormatException e) {
            addError("Línea " + line + ": Valor entero inválido: " + value);
            return "error";
        }
    }
    
    private String inferFloatType(float value, int line) {
        float absValue = Math.abs(value);
        if (absValue == 0 || (absValue >= FLOAT_MIN && absValue <= FLOAT_MAX)) {
            return "float";
        } else {
            addError("Línea " + line + ": Valor float fuera de rango: " + value);
            return "error";
        }
    }
    
    private void checkCharLimits(char value, int line) {
        if (value < CHAR_MIN || value > CHAR_MAX) {
            addError("Línea " + line + ": Valor char fuera de rango: '" + value + "'");
        }
    }
    
    private void checkStringLimits(String value, int line) {
        if (value.length() > STRING_MAX_LENGTH) {
            addError("Línea " + line + ": String excede longitud máxima: " + value.length());
        }
    }
    
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
        }
    }
    
    // ==================== REGLA 2: DECLARACIÓN DE VARIABLES ====================
    
    public boolean checkDeclaration(String identifier, Object value, int line) {
        // Verificar nombre de variable válido
        if (!isValidIdentifier(identifier)) {
            addError("Línea " + line + ": Nombre de variable inválido: '" + identifier + "'");
            return false;
        }
        
        // No se pueden usar palabras reservadas
        if (isReservedWord(identifier)) {
            addError("Línea " + line + ": No se puede usar palabra reservada: '" + identifier + "'");
            return false;
        }
        
        // Verificar si la variable ya existe en el scope actual
        String currentScope = scopeStack.peek();
        Map<String, VariableInfo> currentScopeTable = scopeSymbolTables.get(currentScope);
        if (currentScopeTable.containsKey(identifier)) {
            addError("Línea " + line + ": Variable '" + identifier + "' ya está declarada en este ámbito");
            return false;
        }
        
        // Inferir tipo y verificar límites
        String inferredType = inferTypeFromValue(value, line);
        if (inferredType.equals("error")) {
            return false;
        }
        
        checkValueLimits(inferredType, value, line);
        
        // Agregar a tabla de símbolos del scope actual
        VariableInfo varInfo = new VariableInfo(inferredType, value, line);
        currentScopeTable.put(identifier, varInfo);
        symbolTable.put(identifier, varInfo); // Para acceso global
        
        // CORREGIDO: Marcar como inicializada automáticamente cuando se declara con valor
        initializedVariables.add(identifier);
        
        return true;
    }
    
    /**
     * NUEVO: Declaración de variable sin valor inicial (solo en tabla de símbolos)
     */
    public boolean declareVariable(String identifier, String type, int line) {
        // Verificar nombre de variable válido
        if (!isValidIdentifier(identifier)) {
            addError("Línea " + line + ": Nombre de variable inválido: '" + identifier + "'");
            return false;
        }
        
        // No se pueden usar palabras reservadas
        if (isReservedWord(identifier)) {
            addError("Línea " + line + ": No se puede usar palabra reservada: '" + identifier + "'");
            return false;
        }
        
        // Verificar si la variable ya existe en el scope actual
        String currentScope = scopeStack.peek();
        Map<String, VariableInfo> currentScopeTable = scopeSymbolTables.get(currentScope);
        if (currentScopeTable.containsKey(identifier)) {
            addError("Línea " + line + ": Variable '" + identifier + "' ya está declarada en este ámbito");
            return false;
        }
        
        // Crear variable sin valor inicial
        Object defaultValue = getDefaultValue(type);
        VariableInfo varInfo = new VariableInfo(type, defaultValue, line);
        currentScopeTable.put(identifier, varInfo);
        symbolTable.put(identifier, varInfo);
        
        // CORREGIDO: NO marcar como inicializada - esperará una asignación
        // initializedVariables.add(identifier); // COMENTADO: No inicializar automáticamente
        
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
        return true;
    }
    
    // ==================== REGLA 3: ASIGNACIÓN DE VARIABLES ====================
    
    public boolean checkAssignment(String identifier, Object value, int line) {
        // ✅ Si la variable no existe, crearla automáticamente
        if (!symbolTable.containsKey(identifier)) {
            String inferredType = inferTypeFromValue(value, line);
            if (inferredType.equals("error")) {
                return false;
            }
            // Declarar la variable automáticamente
            declareVariable(identifier, inferredType, line);
        }
        
        // Verificar existencia y alcance
        if (!checkScopeAccess(identifier, line)) {
            return false;
        }
        
        VariableInfo varInfo = symbolTable.get(identifier);
        
        // Constantes no pueden reasignarse
        if (varInfo.isConstant) {
            addError("Línea " + line + ": No se puede reasignar constante '" + identifier + "'");
            return false;
        }
        
        if (isReservedWord(identifier)) {
            addError("Línea " + line + ": No se puede usar palabra reservada '" + identifier + "'");
            return false;
        }

        String inferredType = inferTypeFromValue(value, line);
        
        if (inferredType.equals("error")) {
            return false;
        }
        
        // Compatibilidad de tipos ESTRICTA
        if (!checkTypeCompatibilityStrict(varInfo.type, inferredType, line)) {
            addError("Línea " + line + ": Incompatibilidad de tipos en asignación. Esperado: " + 
                    varInfo.type + ", Obtenido: " + inferredType);
            return false;
        }
        
        // Verificar límites
        checkValueLimits(varInfo.type, value, line);
        
        // ✅ ACTUALIZAR valor Y marcar como inicializada (REASIGNACIÓN)
        varInfo.value = value;
        initializedVariables.add(identifier);
        
        return true;
    }
    
    /**
     * CORREGIDO: Verificación ESTRICTA de tipos - NO permite promoción automática
     */
    private boolean checkTypeCompatibilityStrict(String expected, String actual, int line) {
        // ✅ SOLO mismo tipo - NO promoción automática
        return expected.equals(actual);
    }
    
    // ==================== REGLA 4: VERIFICACIÓN DE INICIALIZACIÓN ====================
    
    public boolean checkVariableInitialized(String identifier, int line) {
        // CORREGIDO: Solo verificar si NO está en el conjunto de inicializadas
        if (!initializedVariables.contains(identifier)) {
            addError("Línea " + line + ": Variable '" + identifier + "' usada sin inicializar");
            return false;
        }
        return true;
    }
    
    /**
     * CORREGIDO: Verificar uso de variable en expresiones (más estricto)
     */
    public boolean checkVariableUsage(String identifier, int line) {
        // Primero verificar que existe
        if (!symbolTable.containsKey(identifier)) {
            addError("Línea " + line + ": Variable '" + identifier + "' no declarada");
            return false;
        }
        
        // Luego verificar que está inicializada
        return checkVariableInitialized(identifier, line);
    }
    
    public void markVariableInitialized(String identifier) {
        initializedVariables.add(identifier);
    }
    
    // ==================== REGLA 5: TIPOS EN ESTRUCTURAS DE CONTROL ====================
    
    public boolean checkConditionType(Object condition, int line) {
        if (!(condition instanceof Boolean)) {
            addError("Línea " + line + ": La condición debe ser de tipo boolean, se encontró: " + 
                    getTypeName(condition));
            return false;
        }
        return true;
    }
    
    public boolean checkLoopCondition(Object condition, int line) {
        if (!checkConditionType(condition, line)) {
            return false;
        }
        
        // Detectar condiciones que siempre son true (posible bucle infinito)
        if (condition instanceof Boolean && (Boolean) condition == true) {
            addError("Línea " + line + ": Advertencia - condición siempre verdadera, posible bucle infinito");
        }
        
        return true;
    }
    
    // ==================== REGLA 6: CONTROL DE BUCLES ====================
    
    public void enterLoop(int line) {
        loopComplexity++;
        if (loopComplexity > 10) {
            addError("Línea " + line + ": Anidamiento de bucles muy profundo (" + loopComplexity + ")");
        }
    }
    
    public void exitLoop() {
        if (loopComplexity > 0) {
            loopComplexity--;
        }
    }
    
    // ==================== REGLA 7: DETECCIÓN DE CÓDIGO INALCANZABLE ====================
    
    public void markReturnStatement(int line) {
        hasReturnStatement = true;
    }
    
    public boolean checkUnreachableCode(int line) {
        if (hasReturnStatement) {
            addError("Línea " + line + ": Código inalcanzable después de return");
            return false;
        }
        return true;
    }
    
    public void resetReturnAnalysis() {
        hasReturnStatement = false;
    }
    
    // ==================== REGLA 8: OPERACIONES ARITMÉTICAS ====================
    
    /**
     * CORREGIDO: Método que SOLO verifica tipos sin realizar operaciones
     */
    public boolean checkBinaryOperationTypes(Object left, Object right, String operator, int line) {
        String leftType = inferTypeFromValue(left, line);
        String rightType = inferTypeFromValue(right, line);
        
        // Concatenación de strings
        if (operator.equals("+") && (leftType.equals("String") || rightType.equals("String"))) {
            // ✅ Permitir concatenación de strings con otros tipos
            return true;
        }
        
        // No se pueden operar booleanos en operaciones aritméticas
        if (leftType.equals("boolean") || rightType.equals("boolean")) {
            addError("Línea " + line + ": No se pueden operar booleanos en operaciones aritméticas");
            return false;
        }
                
        // ✅ CORREGIDO: Verificación ESTRICTA - SOLO mismo tipo
        if (!areSameTypeStrict(leftType, rightType)) {
            addError("Línea " + line + ": No se puede operar " + leftType + " con " + rightType + 
                    ". Los tipos deben ser exactamente iguales.");
            return false;
        }
        
        // División por cero (solo verificación, no ejecución)
        if ((operator.equals("/") || operator.equals("%")) && isZero(right)) {
            addError("Línea " + line + ": División por cero no permitida");
            return false;
        }
        
        return true;
    }

    
    /**
     * CORREGIDO: Verificación ESTRICTA de tipos iguales
     */
    private boolean areSameTypeStrict(String type1, String type2) {
        // ✅ SOLO retorna true si son EXACTAMENTE el mismo tipo
        return type1.equals(type2);
    }
    
    // ==================== REGLA 9: DETECCIÓN DE OVERFLOW ====================
    
    private boolean checkArithmeticOverflow(Object left, Object right, String operator, int line) {
        if (left instanceof Integer && right instanceof Integer) {
            int a = (Integer) left;
            int b = (Integer) right;
            
            try {
                switch (operator) {
                    case "+":
                        Math.addExact(a, b);
                        break;
                    case "-":
                        Math.subtractExact(a, b);
                        break;
                    case "*":
                        Math.multiplyExact(a, b);
                        break;
                }
            } catch (ArithmeticException e) {
                addError("Línea " + line + ": Overflow en operación " + a + " " + operator + " " + b);
                return false;
            }
        }
        return true;
    }
    
    // ==================== OPERACIONES ARITMÉTICAS ESPECÍFICAS (MANTENIDAS) ====================
    
    /**
     * Método original mantenido para compatibilidad
     */
    public Object checkBinaryOperation(Object left, Object right, String operator, int line) {
        String leftType = inferTypeFromValue(left, line);
        String rightType = inferTypeFromValue(right, line);
        
        // Concatenación de strings
        if (operator.equals("+") && (leftType.equals("String") || rightType.equals("String"))) {
            return concatenateValues(left, right, line);
        }
        
        // No se pueden operar booleanos en operaciones aritméticas
        if (leftType.equals("boolean") || rightType.equals("boolean")) {
            addError("Línea " + line + ": No se pueden operar booleanos en operaciones aritméticas");
            return null;
        }
        
        // ✅ CORREGIDO: Verificación ESTRICTA - SOLO mismo tipo
        if (!areSameTypeStrict(leftType, rightType)) {
            addError("Línea " + line + ": No se puede operar " + leftType + " con " + rightType + 
                    ". Los tipos deben ser exactamente iguales.");
            return null;
        }
        
        // División por cero
        if ((operator.equals("/") || operator.equals("%")) && isZero(right)) {
            addError("Línea " + line + ": División por cero no permitida");
            return null;
        }
        
        // Verificar overflow
        if (!checkArithmeticOverflow(left, right, operator, line)) {
            return null;
        }
        
        return performOperation(left, right, operator, leftType, rightType, line);
    }
    
    private Object concatenateValues(Object left, Object right, int line) {
        String leftStr = left.toString();
        String rightStr = right.toString();
        String result = leftStr + rightStr;
        checkStringLimits(result, line);
        return result;
    }
    
    private boolean isZero(Object value) {
        if (value instanceof Integer) return (Integer) value == 0;
        if (value instanceof Float) return (Float) value == 0.0f;
        return false;
    }
    
    private Object performOperation(Object left, Object right, String operator, 
                                   String leftType, String rightType, int line) {
        try {
            // ✅ VERIFICACIÓN ADICIONAL: Asegurar que los tipos son iguales
            if (!leftType.equals(rightType)) {
                addError("Línea " + line + ": Tipos incompatibles: " + leftType + " y " + rightType);
                return null;
            }
            
            String resultType = leftType;
            
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
    
    private Object addValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float result = getFloatValue(left) + getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else if (resultType.equals("int")) {
            int result = getIntValue(left) + getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
        return null;
    }
    
    private Object subtractValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float result = getFloatValue(left) - getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else if (resultType.equals("int")) {
            int result = getIntValue(left) - getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
        return null;
    }
    
    private Object multiplyValues(Object left, Object right, String resultType, int line) {
        if (resultType.equals("float")) {
            float result = getFloatValue(left) * getFloatValue(right);
            checkValueLimits("float", result, line);
            return result;
        } else if (resultType.equals("int")) {
            int result = getIntValue(left) * getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
        return null;
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
        } else if (resultType.equals("int")) {
            int divisor = getIntValue(right);
            if (divisor == 0) {
                addError("Línea " + line + ": División por cero");
                return null;
            }
            int result = getIntValue(left) / divisor;
            checkValueLimits("int", result, line);
            return result;
        }
        return null;
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
        } else if (resultType.equals("int")) {
            int divisor = getIntValue(right);
            if (divisor == 0) {
                addError("Línea " + line + ": Módulo por cero");
                return null;
            }
            int result = getIntValue(left) % getIntValue(right);
            checkValueLimits("int", result, line);
            return result;
        }
        return null;
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
        } else if (resultType.equals("int")) {
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
        return false;
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
    
    // ==================== FUNCIONES AUXILIARES ====================
    
    private float getFloatValue(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Float) return (Float) value;
        return 0.0f;
    }
    
    private int getIntValue(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Float) return ((Float) value).intValue();
        return 0;
    }
    
    private String getTypeName(Object value) {
        if (value instanceof Integer) return "int";
        if (value instanceof Float) return "float";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof String) return "String";
        if (value instanceof Character) return "char";
        return "desconocido";
    }
    
    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) return false;
        if (Character.isDigit(identifier.charAt(0))) return false;
        
        for (char c : identifier.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        return true;
    }
    
    public boolean isReservedWord(String identifier) {
        Set<String> reservedWords = Set.of(
            "if", "else", "while", "for", "do", "break", "return", 
            "function", "true", "false", "input", "print", "println",
            "switch", "case", "default"
        );
        return reservedWords.contains(identifier);
    }
    
    

    /**
     * CORREGIDO: Método público para agregar errores
     */
    public void addError(String error) {
        errors.add(error);
        System.err.println("❌ " + error);
        
    }
    
    
    private String extractDetailsFromError(String error) {
        // Remover "Línea X: " del mensaje si está presente
        if (error.contains("Línea") && error.contains(":")) {
            String[] parts = error.split(":", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        return error;
    }

    private int extractLineFromError(String error) {
        try {
            // Buscar "Línea X:" en el mensaje de error
            if (error.contains("Línea")) {
                String[] parts = error.split("Línea ");
                if (parts.length > 1) {
                    String linePart = parts[1].split(":")[0];
                    return Integer.parseInt(linePart.trim());
                }
            }
        } catch (Exception e) {
            // Si no podemos extraer la línea, usar 1 por defecto
        }
        return 1;
    }

    /**
     * NUEVO: Obtener valor por defecto para un tipo
    
    
     */
    private Object getDefaultValue(String type) {
        switch (type) {
            case "int": return 0;
            case "float": return 0.0f;
            case "boolean": return false;
            case "char": return '\u0000';
            case "String": return "";
            default: return null;
        }
    }
    
    // ==================== MÉTODOS NUEVOS PARA MOSTRAR ERRORES ====================
    
    /**
     * NUEVO: Obtener todos los errores como lista
     */
    public Set<String> getErrors() {
        return new HashSet<>(errors);
    }
    
    /**
     * NUEVO: Obtener errores como string formateado
     */
    public String getErrorsAsString() {
        if (errors.isEmpty()) {
            return "✅ No hay errores semánticos";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("❌ ERRORES SEMÁNTICOS ENCONTRADOS (").append(errors.size()).append("):\n");
            sb.append("=".repeat(60)).append("\n");
            for (String error : errors) {
                sb.append("• ").append(error).append("\n");
            }
            sb.append("=".repeat(60)).append("\n");
            return sb.toString();
        }
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public void printErrors() {
        System.out.println(getErrorsAsString());
    }
    
    public void printSymbolTable() {
        System.out.println("\n📊 TABLA DE SÍMBOLOS:");
        System.out.println("=".repeat(60));
        System.out.printf("%-10s %-15s %-8s %s\n", "VARIABLE", "VALOR", "TIPO", "UBICACIÓN");
        System.out.println("-".repeat(60));
        
        if (symbolTable.isEmpty()) {
            System.out.println("          << TABLA VACÍA >>");
        } else {
            for (Map.Entry<String, VariableInfo> entry : symbolTable.entrySet()) {
                System.out.printf("%-10s %s\n", entry.getKey(), entry.getValue());
            }
        }
        System.out.println("=".repeat(60));
    }
    
    public String getSymbolTableAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 TABLA DE SÍMBOLOS:\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append(String.format("%-10s %-15s %-8s %s\n", "VARIABLE", "VALOR", "TIPO", "UBICACIÓN"));
        sb.append("-".repeat(50)).append("\n");
        
        if (symbolTable.isEmpty()) {
            sb.append("          << TABLA VACÍA >>\n");
        } else {
            for (Map.Entry<String, VariableInfo> entry : symbolTable.entrySet()) {
                sb.append(String.format("%-10s %s\n", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("=".repeat(50)).append("\n");
        return sb.toString();
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
        initializedVariables.clear();
        scopeStack.clear();
        scopeSymbolTables.clear();
        loopComplexity = 0;
        hasReturnStatement = false;
        enterScope("global");
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public Map<String, VariableInfo> getSymbolTable() {
        return new HashMap<>(symbolTable);
    }
    
}

