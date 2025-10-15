import java.util.*;

class ReturnException extends RuntimeException {
        public final Object value;
        
        public ReturnException(Object value) {
            this.value = value;
        }
    }

public class Parser {
    private final List<Token> tokens;
    private int position = 0;
    private final Map<String, Object> symbolTable = new HashMap<>();
    private final StringBuilder output = new StringBuilder();
     private final SemanticAnalyzer semanticAnalyzer;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.semanticAnalyzer = new SemanticAnalyzer();

    }
    
    public Parser(List<Token> tokens, SemanticAnalyzer semanticAnalyzer) {
        this.tokens = tokens;
        this.semanticAnalyzer = semanticAnalyzer;
    }

    private Token currentToken() {
        return position < tokens.size() ? tokens.get(position) : null;
    }
    
    private void eat(TokenType expected) {
        Token token = currentToken();
        if (token != null && token.type == expected) {
            position++;
        } else {
            String mensajeError;
            if (token == null) {
                mensajeError = String.format("❌ Error: Se esperaba %s pero se alcanzó el final del archivo", expected);
            } else {
                mensajeError = String.format(
                    "❌ Error en línea %d: Se esperaba %s, se encontró %s",
                    token.line,
                    expected,
                    token.type
                );
            }
            throw new RuntimeException(mensajeError);
        }
    }
    
    public String parse() {
        try {
            program();
            return output.toString();
        } catch (Exception e) {
            return "❌ " + e.getMessage();
        }
    }
    
    private void program() {
        while (currentToken() != null && currentToken().type != TokenType.EOF) {
            instruction();
            if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                eat(TokenType.SEMICOLON);
            }
        }
    }
    
    private void instruction() {
        Token token = currentToken();
        if (token == null) return;
        
        switch (token.type) {
            case IDENTIFIER:
                declarationOrAssignment();
                break;
            case PRINT:
            case PRINTLN:
                inputOutput();
                break;
            case IF:
                conditional();
                break;
            case WHILE:
            case FOR:
            case DO:
                loop();
                break;
            case BREAK:
                eat(TokenType.BREAK);
                output.append("⏹️  Break ejecutado\n");
                break;
            case RETURN:
                eat(TokenType.RETURN);
                Object returnValue = expression();
                output.append("↩️  Return: ").append(returnValue).append("\n");
                throw new ReturnException(returnValue); // Lanzar excepción en lugar de retornar
            case FUNCTION:
                function();
                break;
            default:
                Object exprResult = expression();
                if (exprResult != null) {
                    output.append("💡 Expresión: ").append(exprResult).append("\n");
                }
        }
    }
    
    
    private void declarationOrAssignment() {
        String identifier = currentToken().value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.ASSIGN);
        
        Object value = expression();
        
        if (!symbolTable.containsKey(identifier)) {
            output.append("📝 Declarando variable: ").append(identifier).append(" = ").append(value).append("\n");
            symbolTable.put(identifier, value);
        } else {
            output.append("🔄 Asignando variable: ").append(identifier).append(" = ").append(value).append("\n");
            symbolTable.put(identifier, value);
        }
    }
    
    private Object expression() {
        return logicalExpression();
    }
    
    private Object logicalExpression() {
        Object left = comparisonExpression();
        
        while (currentToken() != null && 
               (currentToken().type == TokenType.AND || currentToken().type == TokenType.OR)) {
            Token operator = currentToken();
            eat(operator.type);
            Object right = comparisonExpression();
            
            if (operator.type == TokenType.AND) {
                left = (boolean)left && (boolean)right;
            } else {
                left = (boolean)left || (boolean)right;
            }
        }
        
        return left;
    }
    
    private Object comparisonExpression() {
        Object left = arithmeticExpression();
        
        if (currentToken() != null && isComparisonOperator(currentToken().type)) {
            Token operator = currentToken();
            eat(operator.type);
            Object right = arithmeticExpression();
            
            left = evaluateComparison(left, right, operator);
        }
        
        return left;
    }
    
    private Object arithmeticExpression() {
        Object left = term();
        
        while (currentToken() != null && 
               (currentToken().type == TokenType.PLUS || currentToken().type == TokenType.MINUS)) {
            Token operator = currentToken();
            eat(operator.type);
            Object right = term();
            
            left = evaluateArithmetic(left, right, operator);
        }
        
        return left;
    }
    
    private Object term() {
        Object left = factor();
        
        while (currentToken() != null && 
               (currentToken().type == TokenType.MULTIPLY || 
                currentToken().type == TokenType.DIVIDE ||
                currentToken().type == TokenType.MODULO)) {
            Token operator = currentToken();
            eat(operator.type);
            Object right = factor();
            
            left = evaluateArithmetic(left, right, operator);
        }
        
        return left;
    }
    
    private Object factor() {
        if (currentToken() != null && 
            (currentToken().type == TokenType.PLUS || 
             currentToken().type == TokenType.MINUS ||
             currentToken().type == TokenType.NOT)) {
            Token operator = currentToken();
            eat(operator.type);
            Object value = primary();
            return evaluateUnary(value, operator);
        }
        
        return primary();
    }
    
    private Object primary() {
        Token token = currentToken();
        if (token == null) return null;
        
        switch (token.type) {
            case IDENTIFIER:
                // ✅ VERIFICAR si es llamada a función
                Token nextToken = position + 1 < tokens.size() ? tokens.get(position + 1) : null;
                if (nextToken != null && nextToken.type == TokenType.LPAREN) {
                    return functionCall(); // Es una llamada a función
                } else {
                    // Es una variable normal
                    eat(TokenType.IDENTIFIER);
                    if (!symbolTable.containsKey(token.value)) {
                        throw new RuntimeException("❌ Error en línea " + token.line + ": Variable '" + token.value + "' no declarada");
                    }
                    return symbolTable.get(token.value);
                }
                
            case NUMBER:
                eat(TokenType.NUMBER);
                if (token.value.contains(".")) {
                    return Double.parseDouble(token.value);
                } else {
                    return Integer.parseInt(token.value);
                }
                
            case STRING:
                eat(TokenType.STRING);
                return token.value.substring(1, token.value.length() - 1);
                
            case FORMATTED_STRING:
                eat(TokenType.FORMATTED_STRING);
                return interpolateFormattedString(token.value);
                
            case TRUE:
                eat(TokenType.TRUE);
                return true;
                
            case FALSE:
                eat(TokenType.FALSE);
                return false;
                
            case LPAREN:
                eat(TokenType.LPAREN);
                Object result = expression();
                eat(TokenType.RPAREN);
                return result;
                
            default:
                throw new RuntimeException("❌ Error en línea " + token.line + ": Expresión inválida: " + token);
        }
    }
    
    private Object functionCall() {
        String functionName = currentToken().value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);
        
        output.append("🔹 Llamando función: ").append(functionName).append("\n");
        
        if (!symbolTable.containsKey(functionName)) {
            throw new RuntimeException("❌ Error: Función '" + functionName + "' no declarada");
        }
        
        Map<String, Object> functionInfo = (Map<String, Object>) symbolTable.get(functionName);
        if (!"function".equals(functionInfo.get("type"))) {
            throw new RuntimeException("❌ Error: '" + functionName + "' no es una función");
        }
        
        @SuppressWarnings("unchecked")
        List<String> expectedParams = (List<String>) functionInfo.get("parameters");
        
        // ✅ PROCESAR argumentos
        List<Object> arguments = new ArrayList<>();
        if (currentToken().type != TokenType.RPAREN) {
            arguments.add(expression());
            while (currentToken().type == TokenType.COMMA) {
                eat(TokenType.COMMA);
                arguments.add(expression());
            }
        }
        eat(TokenType.RPAREN);
        
        // ✅ VALIDAR número de parámetros
        if (arguments.size() != expectedParams.size()) {
            throw new RuntimeException("❌ Error: Función '" + functionName + "' esperaba " + 
                expectedParams.size() + " parámetros, se proporcionaron " + arguments.size());
        }
        
        output.append("   Argumentos: ").append(arguments).append("\n");
        
        // ✅ GUARDAR el contexto actual (variables locales)
        Map<String, Object> previousSymbolTable = new HashMap<>(symbolTable);
        
        try {
            // ✅ ASIGNAR parámetros a la tabla de símbolos
            for (int i = 0; i < expectedParams.size(); i++) {
                symbolTable.put(expectedParams.get(i), arguments.get(i));
            }
            
            // ✅ EJECUTAR el cuerpo de la función
            int bodyStart = (int) functionInfo.get("bodyStart");
            int previousPosition = position;
            position = bodyStart;
            
            // Ejecutar instrucciones hasta encontrar return o fin de función
            while (currentToken() != null && 
                currentToken().type != TokenType.RBRACE && 
                currentToken().type != TokenType.EOF) {
                try {
                    instruction();
                    if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                        eat(TokenType.SEMICOLON);
                    }
                } catch (ReturnException e) {
                    // ✅ CAPTURAR el return y retornar el valor
                    output.append("   Función retorna: ").append(e.value).append("\n");
                    return e.value;
                }
            }
            
            position = previousPosition; // Restaurar posición
            
            // Si no hay return explícito, retornar null o 0
            output.append("   Función termina sin return explícito\n");
            return 0;
            
        } finally {
            // ✅ RESTAURAR tabla de símbolos (scope local)
            symbolTable.clear();
            symbolTable.putAll(previousSymbolTable);
        }
    }


    private boolean isComparisonOperator(TokenType type) {
        return type == TokenType.EQUALS || type == TokenType.NOT_EQUALS ||
               type == TokenType.LESS || type == TokenType.GREATER ||
               type == TokenType.LESS_EQUAL || type == TokenType.GREATER_EQUAL;
    }
    
    private Object evaluateArithmetic(Object left, Object right, Token operator) {
        double leftNum = toNumber(left);
        double rightNum = toNumber(right);
        
        if (left instanceof Integer && right instanceof Float) {
            throw new RuntimeException("No se puede operar int con float");
        }
        if (left instanceof Float && right instanceof Integer) {
            throw new RuntimeException("No se puede operar float con int");
        }

        switch (operator.type) {
            case PLUS: return leftNum + rightNum;
            case MINUS: return leftNum - rightNum;
            case MULTIPLY: return leftNum * rightNum;
            case DIVIDE: 
                if (rightNum == 0) throw new RuntimeException("División por cero");
                return leftNum / rightNum;
            case MODULO: return leftNum % rightNum;
            default: throw new RuntimeException("Operador inválido: " + operator.type);
        }
    }
    
    private Object evaluateComparison(Object left, Object right, Token operator) {
        if (left instanceof Number && right instanceof Number) {
            double leftNum = ((Number)left).doubleValue();
            double rightNum = ((Number)right).doubleValue();
            
            switch (operator.type) {
                case EQUALS: 
                    return leftNum == rightNum;
                case NOT_EQUALS: 
                    return leftNum != rightNum;
                case LESS: 
                    return leftNum < rightNum;
                case GREATER: 
                    return leftNum > rightNum;
                case LESS_EQUAL: 
                    return leftNum <= rightNum;
                case GREATER_EQUAL: 
                    return leftNum >= rightNum;
                default: 
                    return false;
            }
        }
        // ✅ Permitir comparación entre booleanos
        else if (left instanceof Boolean && right instanceof Boolean) {
            boolean leftBool = (Boolean)left;
            boolean rightBool = (Boolean)right;
            
            switch (operator.type) {
                case EQUALS: 
                    return leftBool == rightBool;
                case NOT_EQUALS: 
                    return leftBool != rightBool;
                default: 
                    throw new RuntimeException("❌ Error: Operador " + operator.type + " no válido para booleanos");
            }
        }
        // ✅ Permitir comparación entre strings
        else if (left instanceof String && right instanceof String) {
            String leftStr = (String)left;
            String rightStr = (String)right;
            
            switch (operator.type) {
                case EQUALS: 
                    return leftStr.equals(rightStr);
                case NOT_EQUALS: 
                    return !leftStr.equals(rightStr);
                default: 
                    throw new RuntimeException("❌ Error: Operador " + operator.type + " no válido para strings");
            }
        }
        // ❌ Tipos incompatibles
        else {
            throw new RuntimeException("❌ Error: No se pueden comparar " + 
                left.getClass().getSimpleName() + " y " + 
                right.getClass().getSimpleName() + " con " + operator.type);
        }
    }
    
    private Object evaluateUnary(Object value, Token operator) {
        switch (operator.type) {
            case PLUS: return toNumber(value);
            case MINUS: return -toNumber(value);
            case NOT: return !(boolean)value;
            default: return value;
        }
    }
    
    private double toNumber(Object value) {
        if (value instanceof Integer) return (Integer)value;
        if (value instanceof Double) return (Double)value;
        throw new RuntimeException("No se puede convertir a número: " + value);
    }
    
    private void inputOutput() {
        Token token = currentToken();
        if (token.type == TokenType.PRINT || token.type == TokenType.PRINTLN) {
            eat(token.type);
            eat(TokenType.LPAREN);
            List<Object> values = new ArrayList<>();
            
            if (currentToken().type != TokenType.RPAREN) {
                if (currentToken().type == TokenType.FORMATTED_STRING) {
                    Token strToken = currentToken();
                    eat(TokenType.FORMATTED_STRING);
                    String formattedValue = interpolateFormattedString(strToken.value);
                    values.add(formattedValue);
                } else if (currentToken().type == TokenType.STRING) {
                    Token strToken = currentToken();
                    eat(TokenType.STRING);
                    values.add(strToken.value.substring(1, strToken.value.length() - 1));
                } else {
                    values.add(expression());
                }
                
                while (currentToken().type == TokenType.COMMA) {
                    eat(TokenType.COMMA);
                    if (currentToken().type == TokenType.FORMATTED_STRING) {
                        Token strToken = currentToken();
                        eat(TokenType.FORMATTED_STRING);
                        String formattedValue = interpolateFormattedString(strToken.value);
                        values.add(formattedValue);
                    } else if (currentToken().type == TokenType.STRING) {
                        Token strToken = currentToken();
                        eat(TokenType.STRING);
                        values.add(strToken.value.substring(1, strToken.value.length() - 1));
                    } else {
                        values.add(expression());
                    }
                }
            }
            eat(TokenType.RPAREN);
            
            StringBuilder outputStr = new StringBuilder();
            for (Object value : values) {
                outputStr.append(value).append(" ");
            }
            
            String finalOutput = outputStr.toString().trim();
            if (token.type == TokenType.PRINTLN) {
                output.append("📤 Println: ").append(finalOutput).append("\n");
            } else {
                output.append("📤 Print: ").append(finalOutput).append("\n");
            }
        }
    }
    
    private String interpolateFormattedString(String formattedStr) {
        String content = formattedStr.substring(2, formattedStr.length() - 1);
        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean hasVariables = false;
        Token currentToken = currentToken();
        
        while (i < content.length()) {
            if (content.charAt(i) == '{' && i + 1 < content.length()) {
                int end = content.indexOf('}', i + 1);
                if (end != -1) {
                    String varName = content.substring(i + 1, end).trim();
                    if (symbolTable.containsKey(varName)) {
                        result.append(symbolTable.get(varName));
                        hasVariables = true;
                    } else {
                        throw new RuntimeException("❌ Error en línea " + (currentToken != null ? currentToken.line : 0) + 
                                               ": Variable '" + varName + "' no declarada en string formateado");
                    }
                    i = end + 1;
                    continue;
                } else {
                    throw new RuntimeException("❌ Error en línea " + (currentToken != null ? currentToken.line : 0) + 
                                           ": Llave de cierre '}' faltante en string formateado");
                }
            }
            result.append(content.charAt(i));
            i++;
        }
        
        if (!hasVariables) {
            throw new RuntimeException("❌ Error en línea " + (currentToken != null ? currentToken.line : 0) + 
                                   ": String formateado debe contener al menos una variable entre llaves { }");
        }
        
        return result.toString();
    }
    
    private void conditional() {
        eat(TokenType.IF);
        eat(TokenType.LPAREN);
        Object condition = expression();
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);
        
        output.append("🔍 Condición IF: ").append(condition).append("\n");
        
        boolean executed = false;
        
        // Bloque IF
        if ((boolean)condition) {
            executed = true;
            while (currentToken() != null && 
                currentToken().type != TokenType.RBRACE && 
                currentToken().type != TokenType.EOF) {
                instruction();
                if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                    eat(TokenType.SEMICOLON);
                }
            }
        } else {
            // Saltar bloque IF
            skipToMatchingBrace();
        }
        
        if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
            eat(TokenType.RBRACE);
        }
        
        // ✅ Manejar ELSE IF y ELSE
        while (currentToken() != null && currentToken().type == TokenType.ELSE) {
            eat(TokenType.ELSE);
            
            // Verificar si es ELSE IF o ELSE normal
            if (currentToken() != null && currentToken().type == TokenType.IF) {
                // ✅ Es ELSE IF
                eat(TokenType.IF);
                eat(TokenType.LPAREN);
                Object elseIfCondition = expression();
                eat(TokenType.RPAREN);
                eat(TokenType.LBRACE);
                
                output.append("🔍 Condición ELSE IF: ").append(elseIfCondition).append("\n");
                
                if (!executed && (boolean)elseIfCondition) {
                    executed = true;
                    while (currentToken() != null && 
                        currentToken().type != TokenType.RBRACE && 
                        currentToken().type != TokenType.EOF) {
                        instruction();
                        if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                            eat(TokenType.SEMICOLON);
                        }
                    }
                } else {
                    // Saltar bloque ELSE IF
                    skipToMatchingBrace();
                }
                
                if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
                    eat(TokenType.RBRACE);
                }
                
            } else {
                // ✅ Es ELSE normal
                eat(TokenType.LBRACE);
                
                output.append("🔍 Bloque ELSE\n");
                
                if (!executed) {
                    while (currentToken() != null && 
                        currentToken().type != TokenType.RBRACE && 
                        currentToken().type != TokenType.EOF) {
                        instruction();
                        if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                            eat(TokenType.SEMICOLON);
                        }
                    }
                } else {
                    // Saltar bloque ELSE
                    skipToMatchingBrace();
                }
                
                if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
                    eat(TokenType.RBRACE);
                }
                break; // Salir del while después del ELSE normal
            }
        }
    }
    
    private void loop() {
        Token token = currentToken();
    
        if (token.type == TokenType.WHILE) {
            // ✅ Bucle WHILE
            eat(TokenType.WHILE);
            eat(TokenType.LPAREN);
            
            // Guardar posición para repetir la condición
            int conditionStart = position;
            Object condition = expression();
            
            eat(TokenType.RPAREN);
            eat(TokenType.LBRACE);
            
            output.append("🔄 Iniciando bucle WHILE: ").append(condition).append("\n");
            
            int loopCount = 0;
            final int MAX_LOOPS = 1000; // Prevenir loops infinitos
            
            // Ejecutar el bucle mientras la condición sea verdadera
            while ((boolean)condition && loopCount < MAX_LOOPS) {
                // Ejecutar bloque del bucle
                int blockStart = position;
                while (currentToken() != null && 
                    currentToken().type != TokenType.RBRACE && 
                    currentToken().type != TokenType.EOF) {
                    instruction();
                    if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                        eat(TokenType.SEMICOLON);
                    }
                }
                
                // Volver a evaluar la condición
                position = conditionStart;
                condition = expression();
                position = blockStart; // Volver al inicio del bloque
                
                loopCount++;
                
                // Salir si se excede el máximo de iteraciones
                if (loopCount >= MAX_LOOPS) {
                    output.append("⚠️  Advertencia: Bucle interrumpido después de ").append(MAX_LOOPS).append(" iteraciones\n");
                    break;
                }
            }
            
            if (!(boolean)condition) {
                output.append("🔄 Bucle WHILE terminado (condición falsa)\n");
            }
            
            // Saltar el bloque del bucle (ya fue ejecutado)
            skipToMatchingBrace();
            
            if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
                eat(TokenType.RBRACE);
            }
            
        } else if (token.type == TokenType.FOR) {
            // ✅ Bucle FOR CORREGIDO
            eat(TokenType.FOR);
            eat(TokenType.LPAREN);
            
            output.append("🔄 Iniciando bucle FOR\n");
            
            // 1. INICIALIZACIÓN (puede ser declaración o asignación)
            if (currentToken().type != TokenType.SEMICOLON) {
                if (currentToken().type == TokenType.IDENTIFIER) {
                    Token nextToken = position + 1 < tokens.size() ? tokens.get(position + 1) : null;
                    if (nextToken != null && nextToken.type == TokenType.ASSIGN) {
                        declarationOrAssignment(); // i = 0
                    } else {
                        expression(); // solo expresión
                    }
                } else {
                    expression(); // otra expresión
                }
            }
            eat(TokenType.SEMICOLON);
            
            // 2. CONDICIÓN
            Object condition = true; // default si no hay condición
            if (currentToken().type != TokenType.SEMICOLON) {
                condition = expression();
            }
            eat(TokenType.SEMICOLON);
            
            // 3. INCREMENTO - permitir asignaciones como i = i + 1
            if (currentToken().type != TokenType.RPAREN) {
                // ✅ PERMITIR ASIGNACIONES en el incremento
                if (currentToken().type == TokenType.IDENTIFIER) {
                    Token nextToken = position + 1 < tokens.size() ? tokens.get(position + 1) : null;
                    
                    if (nextToken != null && nextToken.type == TokenType.ASSIGN) {
                        // Asignación: i = i + 1
                        String incrementVar = currentToken().value;
                        eat(TokenType.IDENTIFIER);
                        eat(TokenType.ASSIGN);
                        Object incrementValue = expression();
                        
                        output.append("📈 Incremento: ").append(incrementVar)
                            .append(" = ").append(incrementValue).append("\n");
                        
                        symbolTable.put(incrementVar, incrementValue);
                        
                    } else if (nextToken != null && 
                            (nextToken.type == TokenType.INCREMENT || nextToken.type == TokenType.DECREMENT)) {
                        // Incremento/decremento: i++ o i--
                        String incrementVar = currentToken().value;
                        eat(TokenType.IDENTIFIER);
                        Token incrementOp = currentToken();
                        eat(incrementOp.type);
                        
                        Object currentValue = symbolTable.get(incrementVar);
                        Object newValue;
                        
                        if (incrementOp.type == TokenType.INCREMENT) {
                            newValue = toNumber(currentValue) + 1;
                        } else {
                            newValue = toNumber(currentValue) - 1;
                        }
                        
                        output.append("📈 Incremento: ").append(incrementVar)
                            .append(incrementOp.type == TokenType.INCREMENT ? "++" : "--")
                            .append(" = ").append(newValue).append("\n");
                        
                        symbolTable.put(incrementVar, newValue);
                        
                    } else {
                        // Expresión simple
                        expression();
                    }
                } else {
                    // Otra expresión
                    expression();
                }
            }
            eat(TokenType.RPAREN);
            eat(TokenType.LBRACE);
            
            output.append("🔍 Condición FOR: ").append(condition).append("\n");
            
            // Ejecutar bloque del FOR
            if ((boolean)condition) {
                while (currentToken() != null && 
                    currentToken().type != TokenType.RBRACE && 
                    currentToken().type != TokenType.EOF) {
                    instruction();
                    if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                        eat(TokenType.SEMICOLON);
                    }
                }
            } else {
                skipToMatchingBrace();
            }
            
            if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
                eat(TokenType.RBRACE);
            }
        } else if (token.type == TokenType.DO) {
            // ✅ Bucle DO-WHILE
            eat(TokenType.DO);
            eat(TokenType.LBRACE);
            
            output.append("🔄 Iniciando bucle DO-WHILE\n");
            
            // Guardar posición del bloque para posibles repeticiones
            int blockStart = position;
            
            // EJECUTAR BLOQUE (siempre se ejecuta al menos una vez)
            while (currentToken() != null && 
                currentToken().type != TokenType.RBRACE && 
                currentToken().type != TokenType.EOF) {
                instruction();
                if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                    eat(TokenType.SEMICOLON);
                }
            }
            
            if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
                eat(TokenType.RBRACE);
            }
            
            // Verificar que viene WHILE después
            if (currentToken() != null && currentToken().type == TokenType.WHILE) {
                eat(TokenType.WHILE);
                eat(TokenType.LPAREN);
                Object condition = expression();
                eat(TokenType.RPAREN);
                
                output.append("🔍 Condición DO-WHILE: ").append(condition).append("\n");
                
                // Semicolon opcional
                if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                    eat(TokenType.SEMICOLON);
                }
                
                // En una implementación real, aquí se repetiría el bloque si condición es true
                if ((boolean)condition) {
                    output.append("🔄 DO-WHILE: La condición es verdadera (en un interprete real se repetiría)\n");
                } else {
                    output.append("🔄 DO-WHILE: La condición es falsa - bucle terminado\n");
                }
                
            } else {
                throw new RuntimeException("❌ Error: Se esperaba 'while' después de 'do'");
            }
        }
    }
    
    private void function() {
        eat(TokenType.FUNCTION);
        String functionName = currentToken().value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);
        
        output.append("📋 Declarando función: ").append(functionName).append("\n");
        
        // ✅ PARÁMETROS
        List<String> parameters = new ArrayList<>();
        if (currentToken().type != TokenType.RPAREN) {
            // Primer parámetro
            parameters.add(currentToken().value);
            eat(TokenType.IDENTIFIER);
            
            // Más parámetros
            while (currentToken().type == TokenType.COMMA) {
                eat(TokenType.COMMA);
                parameters.add(currentToken().value);
                eat(TokenType.IDENTIFIER);
            }
        }
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);
        
        output.append("   Parámetros: ").append(parameters).append("\n");
        
        // ✅ GUARDAR información de la función
        Map<String, Object> functionInfo = new HashMap<>();
        functionInfo.put("type", "function");
        functionInfo.put("parameters", parameters);
        functionInfo.put("bodyStart", position); // Guardar posición del cuerpo
        
        // Guardar función en tabla de símbolos
        symbolTable.put(functionName, functionInfo);
        
        output.append("   Cuerpo de función registrado\n");
        
        // ✅ SALTAR el cuerpo de la función (por ahora)
        skipToMatchingBrace();
        
        if (currentToken() != null && currentToken().type == TokenType.RBRACE) {
            eat(TokenType.RBRACE);
        }
    }
    
    // ✅ Método mejorado para saltar bloques
    private void skipToMatchingBrace() {
        int braceCount = 1;
        while (currentToken() != null && braceCount > 0 && currentToken().type != TokenType.EOF) {
            if (currentToken().type == TokenType.LBRACE) {
                braceCount++;
            } else if (currentToken().type == TokenType.RBRACE) {
                braceCount--;
            }
            position++;
        }
    }

    private void skipUntil(TokenType target) {
        while (currentToken() != null && currentToken().type != target) {
            position++;
        }
    }
}