
import java.util.*;
import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int position = 0;
    private final Map<String, Object> symbolTable = new HashMap<>();
    private final StringBuilder output = new StringBuilder();
    
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    private Token currentToken() {
        return position < tokens.size() ? tokens.get(position) : null;
    }
    
    private void eat(TokenType expected) {
        Token token = currentToken();
        if (token != null && token.type == expected) {
            position++;
        } else {
            throw new RuntimeException("Se esperaba " + expected + 
                ", se encontró " + (token != null ? token.type : "EOF"));
        }
    }
    
    public String parse() {
        try {
            program();
            return output.toString();
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
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
                break;
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
                eat(TokenType.IDENTIFIER);
                if (!symbolTable.containsKey(token.value)) {
                    throw new RuntimeException("Variable no declarada: " + token.value);
                }
                return symbolTable.get(token.value);
                
            case NUMBER:
                eat(TokenType.NUMBER);
                if (token.value.contains(".")) {
                    return Double.parseDouble(token.value);
                } else {
                    return Integer.parseInt(token.value); // ✅ Mantener como int
                }
                
            case STRING:
                eat(TokenType.STRING);
                String strValue = token.value.substring(1, token.value.length() - 1);
                // ✅ Interpolación básica de strings
                return interpolateString(strValue);
                
            case FORMATTED_STRING:
                eat(TokenType.FORMATTED_STRING);
                String formattedStr = token.value.substring(2, token.value.length() - 1); // Quitar f""
                return interpolateString(formattedStr);
                
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
                throw new RuntimeException("Expresión inválida: " + token);
        }
    }

    private String interpolateString(String str) {
    StringBuilder result = new StringBuilder();
    int i = 0;
    while (i < str.length()) {
        if (str.charAt(i) == '{' && i + 1 < str.length()) {
            int end = str.indexOf('}', i + 1);
            if (end != -1) {
                String varName = str.substring(i + 1, end).trim();
                if (symbolTable.containsKey(varName)) {
                    result.append(symbolTable.get(varName));
                } else {
                    result.append("{").append(varName).append("}");
                }
                i = end + 1;
                continue;
            }
        }
        result.append(str.charAt(i));
        i++;
    }
    return result.toString();
}
    
    private boolean isComparisonOperator(TokenType type) {
        return type == TokenType.EQUALS || type == TokenType.NOT_EQUALS ||
               type == TokenType.LESS || type == TokenType.GREATER ||
               type == TokenType.LESS_EQUAL || type == TokenType.GREATER_EQUAL;
    }
    
    private Object evaluateArithmetic(Object left, Object right, Token operator) {
        double leftNum = toNumber(left);
        double rightNum = toNumber(right);
        
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
                case EQUALS: return leftNum == rightNum;
                case NOT_EQUALS: return leftNum != rightNum;
                case LESS: return leftNum < rightNum;
                case GREATER: return leftNum > rightNum;
                case LESS_EQUAL: return leftNum <= rightNum;
                case GREATER_EQUAL: return leftNum >= rightNum;
                default: return false;
            }
        }
        return false;
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
                values.add(expression());
                while (currentToken().type == TokenType.COMMA) {
                    eat(TokenType.COMMA);
                    values.add(expression());
                }
            }
            eat(TokenType.RPAREN);
            
            StringBuilder outputStr = new StringBuilder();
            for (Object value : values) {
                outputStr.append(value).append(" ");
            }
            
            if (token.type == TokenType.PRINTLN) {
                output.append("📤 Println: ").append(outputStr.toString().trim()).append("\n");
            } else {
                output.append("📤 Print: ").append(outputStr.toString().trim()).append("\n");
            }
        }
    }
    
    private void conditional() {
        eat(TokenType.IF);
        eat(TokenType.LPAREN);
        Object condition = expression();
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);
        
        output.append("🔍 Condición IF: ").append(condition).append("\n");
        
        if ((boolean)condition) {
            while (currentToken() != null && currentToken().type != TokenType.RBRACE) {
                instruction();
                if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                    eat(TokenType.SEMICOLON);
                }
            }
        } else {
            skipBlock();
        }
        eat(TokenType.RBRACE);
    }
    
    private void loop() {
        Token token = currentToken();
        if (token.type == TokenType.WHILE) {
            eat(TokenType.WHILE);
            eat(TokenType.LPAREN);
            Object condition = expression();
            eat(TokenType.RPAREN);
            eat(TokenType.LBRACE);
            
            output.append("🔄 Bucle WHILE: ").append(condition).append("\n");
            
            if ((boolean)condition) {
                while (currentToken() != null && currentToken().type != TokenType.RBRACE) {
                    instruction();
                    if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                        eat(TokenType.SEMICOLON);
                    }
                }
            } else {
                skipBlock();
            }
            eat(TokenType.RBRACE);
        }
    }
    
    private void function() {
        eat(TokenType.FUNCTION);
        String functionName = currentToken().value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);
        
        output.append("📋 Declarando función: ").append(functionName).append("\n");
        
        if (currentToken().type != TokenType.RPAREN) {
            while (currentToken().type == TokenType.IDENTIFIER || currentToken().type == TokenType.COMMA) {
                eat(currentToken().type);
            }
        }
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);
        
        skipBlock();
        eat(TokenType.RBRACE);
    }
    
    private void skipBlock() {
        int braceCount = 1;
        while (currentToken() != null && braceCount > 0) {
            if (currentToken().type == TokenType.LBRACE) {
                braceCount++;
            } else if (currentToken().type == TokenType.RBRACE) {
                braceCount--;
            }
            position++;
        }
    }
}