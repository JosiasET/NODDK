import java.util.*;

public class Optimizer {
    private boolean changed;

    public List<Token> optimize(List<Token> tokens) {
        List<Token> currentTokens = new ArrayList<>(tokens);

        System.out.println("   ⚙️  Iniciando fase de optimización...");
        int pass = 1;

        do {
            changed = false;
            int initialSize = currentTokens.size();

            // 1. Constant Folding (Plegado de constantes)
            currentTokens = foldConstants(currentTokens);

            // 2. Dead Code Elimination (Eliminación de código muerto)
            currentTokens = eliminateDeadCode(currentTokens);

            if (changed) {
                System.out.println("      Pass " + pass + ": Reducción de " + initialSize + " a " + currentTokens.size()
                        + " tokens");
                pass++;
            }
        } while (changed && pass < 10); // Límite de pasadas por seguridad

        return currentTokens;
    }

    // -------------------------------------------------------------------------
    // CONSTANT FOLDING: Evalúa operaciones constantes estáticas (e.g. 2 + 3 -> 5)
    // -------------------------------------------------------------------------
    private List<Token> foldConstants(List<Token> tokens) {
        // Prioridad 1: Multiplicación, División, Módulo
        tokens = foldOperations(tokens, EnumSet.of(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO));

        // Prioridad 2: Suma, Resta
        tokens = foldOperations(tokens, EnumSet.of(TokenType.PLUS, TokenType.MINUS));

        // Prioridad 3: Comparaciones (solo números por ahora)
        tokens = foldOperations(tokens, EnumSet.of(
                TokenType.EQUALS, TokenType.NOT_EQUALS,
                TokenType.LESS, TokenType.GREATER,
                TokenType.LESS_EQUAL, TokenType.GREATER_EQUAL));

        return tokens;
    }

    private List<Token> foldOperations(List<Token> tokens, Set<TokenType> operators) {
        List<Token> result = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            // Necesitamos al menos: NUMBER OP NUMBER
            if (i < tokens.size() - 2) {
                Token t1 = tokens.get(i);
                Token op = tokens.get(i + 1);
                Token t2 = tokens.get(i + 2);

                if (t1.type == TokenType.NUMBER && operators.contains(op.type) && t2.type == TokenType.NUMBER) {
                    try {
                        Token folded = performOperation(t1, op, t2);
                        result.add(folded);
                        i += 2; // Saltar los 2 tokens siguientes (op y t2)
                        changed = true;
                        continue;
                    } catch (Exception e) {
                        // Si hay error (ej. división por cero), no optimizar
                    }
                }
            }
            result.add(tokens.get(i));
        }
        return result;
    }

    private Token performOperation(Token t1, Token op, Token t2) {
        double v1 = Double.parseDouble(t1.value);
        double v2 = Double.parseDouble(t2.value);
        Object res = null;
        TokenType newType = TokenType.NUMBER;

        switch (op.type) {
            case PLUS:
                res = v1 + v2;
                break;
            case MINUS:
                res = v1 - v2;
                break;
            case MULTIPLY:
                res = v1 * v2;
                break;
            case DIVIDE:
                if (v2 != 0)
                    res = v1 / v2;
                break;
            case MODULO:
                res = v1 % v2;
                break;

            case EQUALS:
                res = (v1 == v2);
                newType = res.equals(true) ? TokenType.TRUE : TokenType.FALSE;
                break;
            case NOT_EQUALS:
                res = (v1 != v2);
                newType = res.equals(true) ? TokenType.TRUE : TokenType.FALSE;
                break;
            case LESS:
                res = (v1 < v2);
                newType = res.equals(true) ? TokenType.TRUE : TokenType.FALSE;
                break;
            case GREATER:
                res = (v1 > v2);
                newType = res.equals(true) ? TokenType.TRUE : TokenType.FALSE;
                break;
            case LESS_EQUAL:
                res = (v1 <= v2);
                newType = res.equals(true) ? TokenType.TRUE : TokenType.FALSE;
                break;
            case GREATER_EQUAL:
                res = (v1 >= v2);
                newType = res.equals(true) ? TokenType.TRUE : TokenType.FALSE;
                break;
        }

        if (res == null)
            throw new ArithmeticException("Operación inválida");

        String valStr;
        if (newType == TokenType.NUMBER) {
            double dVal = (Double) res;
            // Si es entero (ej 5.0), guardarlo como "5"
            if (dVal == (long) dVal) {
                valStr = String.valueOf((long) dVal);
            } else {
                valStr = String.valueOf(dVal);
            }
        } else {
            valStr = res.toString();
        }

        // Conservar línea/columna del primer token
        return new Token(newType, valStr, t1.line, t1.column);
    }

    // -------------------------------------------------------------------------
    // DEAD CODE ELIMINATION: Elimina bloques inalcanzables (if(false){...})
    // -------------------------------------------------------------------------
    private List<Token> eliminateDeadCode(List<Token> tokens) {
        List<Token> result = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            // Detectar: IF + LPAREN + FALSE + RPAREN + LBRACE
            if (i < tokens.size() - 5) {
                if (tokens.get(i).type == TokenType.IF &&
                        tokens.get(i + 1).type == TokenType.LPAREN &&
                        tokens.get(i + 2).type == TokenType.FALSE &&
                        tokens.get(i + 3).type == TokenType.RPAREN &&
                        tokens.get(i + 4).type == TokenType.LBRACE) {

                    // Encontrado bloque muerto. Buscar el RBRACE de cierre correspondiente
                    int depth = 1;
                    int j = i + 5;
                    boolean foundEnd = false;

                    while (j < tokens.size()) {
                        if (tokens.get(j).type == TokenType.LBRACE)
                            depth++;
                        if (tokens.get(j).type == TokenType.RBRACE) {
                            depth--;
                            if (depth == 0) {
                                foundEnd = true;
                                break;
                            }
                        }
                        j++;
                    }

                    if (foundEnd) {
                        // Saltar todo el bloque (desde i hasta j)
                        // System.out.println(" 🗑️ Eliminado bloque de código muerto (if false)");
                        i = j; // El bucle for hará i++ después, saltando también el RBRACE
                        changed = true;
                        continue;
                    }
                }
            }
            result.add(tokens.get(i));
        }
        return result;
    }
}
