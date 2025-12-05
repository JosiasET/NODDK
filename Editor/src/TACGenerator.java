import java.util.*;

public class TACGenerator {
    private List<Token> tokens;
    private int position;
    private List<TACInstruction> instructions;
    private int tempCount;
    private int labelCount;

    public TACGenerator(List<Token> tokens) {
        this.tokens = tokens;
        this.instructions = new ArrayList<>();
        this.tempCount = 0;
        this.labelCount = 0;
        this.position = 0;
    }

    public List<TACInstruction> generate() {
        instructions.clear();
        tempCount = 0;
        labelCount = 0;
        position = 0;

        while (currentToken() != null && currentToken().type != TokenType.EOF) {
            instruction();
            if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                eat(TokenType.SEMICOLON);
            }
        }
        return instructions;
    }

    private Token currentToken() {
        return position < tokens.size() ? tokens.get(position) : null;
    }

    private void eat(TokenType expected) {
        Token token = currentToken();
        if (token != null && token.type == expected) {
            position++;
        }
    }

    private String newTemp() {
        return "t" + (tempCount++);
    }

    private String newLabel() {
        return "L" + (labelCount++);
    }

    private void emit(String op, String arg1, String arg2, String result) {
        instructions.add(new TACInstruction(op, arg1, arg2, result));
    }

    private void instruction() {
        Token token = currentToken();
        if (token == null)
            return;

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
                whileLoop();
                break;
            case FOR:
                forLoop();
                break;
            case DO:
                // doWhileLoop(); // Optional
                break;
            case SWITCH:
                switchStatement();
                break;
            case FUNCTION:
                function();
                break;
            case RETURN:
                returnStatement();
                break;
            default:
                expression(); // Consume expression if any
                break;
        }
    }

    private void switchStatement() {
        eat(TokenType.SWITCH);
        eat(TokenType.LPAREN);
        String switchExpr = expression();
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);

        String labelEnd = newLabel();

        while (currentToken() != null && currentToken().type != TokenType.RBRACE
                && currentToken().type != TokenType.EOF) {
            if (currentToken().type == TokenType.CASE) {
                eat(TokenType.CASE);
                String caseExpr = expression();
                eat(TokenType.COLON);

                String labelNext = newLabel();

                // Check condition
                String testTemp = newTemp();
                emit("==", switchExpr, caseExpr, testTemp);
                emit("IF_FALSE", testTemp, null, labelNext);

                // Body
                while (currentToken() != null && currentToken().type != TokenType.CASE &&
                        currentToken().type != TokenType.DEFAULT && currentToken().type != TokenType.RBRACE) {

                    if (currentToken().type == TokenType.BREAK) {
                        eat(TokenType.BREAK);
                        emit("GOTO", null, null, labelEnd);
                        if (currentToken().type == TokenType.SEMICOLON)
                            eat(TokenType.SEMICOLON);
                    } else {
                        instruction();
                        if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                            eat(TokenType.SEMICOLON);
                        }
                    }
                }

                emit("LABEL", null, null, labelNext);

            } else if (currentToken().type == TokenType.DEFAULT) {
                eat(TokenType.DEFAULT);
                eat(TokenType.COLON);

                while (currentToken() != null && currentToken().type != TokenType.RBRACE) {
                    if (currentToken().type == TokenType.BREAK) {
                        eat(TokenType.BREAK);
                        emit("GOTO", null, null, labelEnd);
                        if (currentToken().type == TokenType.SEMICOLON)
                            eat(TokenType.SEMICOLON);
                    } else {
                        instruction();
                        if (currentToken().type == TokenType.SEMICOLON) {
                            eat(TokenType.SEMICOLON);
                        }
                    }
                }
            } else {
                // Skip invalid token inside switch structure (e.g. semicolons) to avoid
                // infinite loop
                if (currentToken().type == TokenType.SEMICOLON) {
                    eat(TokenType.SEMICOLON);
                } else {
                    // Should probably error, but just eat to progress
                    position++;
                }
            }
        }
        eat(TokenType.RBRACE);
        emit("LABEL", null, null, labelEnd);
    }

    private void declarationOrAssignment() {
        String identifier = currentToken().value;
        // Check for function call
        if (position + 1 < tokens.size() && tokens.get(position + 1).type == TokenType.LPAREN) {
            functionCall();
            return;
        }

        eat(TokenType.IDENTIFIER);
        if (currentToken().type == TokenType.ASSIGN) {
            eat(TokenType.ASSIGN);
            String exprTemp = expression();
            emit("=", exprTemp, null, identifier);
        }
    }

    private void inputOutput() {
        Token token = currentToken(); // PRINT or PRINTLN
        eat(token.type);
        eat(TokenType.LPAREN);

        List<String> args = new ArrayList<>();
        if (currentToken().type != TokenType.RPAREN) {
            do {
                args.add(expression());
                if (currentToken().type == TokenType.COMMA) {
                    eat(TokenType.COMMA);
                }
            } while (currentToken().type != TokenType.RPAREN && currentToken().type != TokenType.EOF);
        }
        eat(TokenType.RPAREN);

        for (String arg : args) {
            emit("param", arg, null, null);
        }
        emit("call", token.type == TokenType.PRINTLN ? "println" : "print", String.valueOf(args.size()),
                "t" + tempCount++); // Dummy result
    }

    private void conditional() {
        eat(TokenType.IF);
        eat(TokenType.LPAREN);
        String condTemp = expression();
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);

        String labelFalse = newLabel();
        String labelEnd = newLabel();

        emit("IF_FALSE", condTemp, null, labelFalse);

        while (currentToken() != null && currentToken().type != TokenType.RBRACE
                && currentToken().type != TokenType.EOF) {
            instruction();
            if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                eat(TokenType.SEMICOLON);
            }
        }
        eat(TokenType.RBRACE);

        emit("GOTO", null, null, labelEnd);
        emit("LABEL", null, null, labelFalse);

        // Handle else if needed (not in basic parser but good to have)
        if (currentToken() != null && currentToken().type == TokenType.ELSE) {
            eat(TokenType.ELSE);
            eat(TokenType.LBRACE);
            while (currentToken() != null && currentToken().type != TokenType.RBRACE
                    && currentToken().type != TokenType.EOF) {
                instruction();
                if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                    eat(TokenType.SEMICOLON);
                }
            }
            eat(TokenType.RBRACE);
        }

        emit("LABEL", null, null, labelEnd);
    }

    private void whileLoop() {
        eat(TokenType.WHILE);
        eat(TokenType.LPAREN);

        String labelStart = newLabel();
        String labelEnd = newLabel();

        emit("LABEL", null, null, labelStart);

        String condTemp = expression();
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);

        emit("IF_FALSE", condTemp, null, labelEnd);

        while (currentToken() != null && currentToken().type != TokenType.RBRACE
                && currentToken().type != TokenType.EOF) {
            instruction();
            if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                eat(TokenType.SEMICOLON);
            }
        }
        eat(TokenType.RBRACE);

        emit("GOTO", null, null, labelStart);
        emit("LABEL", null, null, labelEnd);
    }

    private void forLoop() {
        eat(TokenType.FOR);
        eat(TokenType.LPAREN);

        // Init
        if (currentToken().type != TokenType.SEMICOLON) {
            declarationOrAssignment(); // or expression
        }
        eat(TokenType.SEMICOLON);

        String labelStart = newLabel();
        String labelEnd = newLabel();
        emit("LABEL", null, null, labelStart);

        // Condition
        String condTemp = "true";
        if (currentToken().type != TokenType.SEMICOLON) {
            condTemp = expression();
        }
        emit("IF_FALSE", condTemp, null, labelEnd);
        eat(TokenType.SEMICOLON);

        // Increment (save tokens to process later or process now?)
        // To do it correctly in single pass without AST is hard because increment comes
        // after body.
        // For simplicity in TAC Generator, we might just emit it at start of body or
        // store it.
        // Let's store tokens for increment.
        List<Token> incrementTokens = new ArrayList<>();
        while (currentToken().type != TokenType.RPAREN) {
            incrementTokens.add(currentToken());
            position++;
        }
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);

        // Body
        while (currentToken() != null && currentToken().type != TokenType.RBRACE
                && currentToken().type != TokenType.EOF) {
            instruction();
            if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                eat(TokenType.SEMICOLON);
            }
        }
        eat(TokenType.RBRACE);

        // Emit increment code here
        if (!incrementTokens.isEmpty()) {
            // Backup position, switch to increment tokens, generate, restore
            List<Token> savedTokens = this.tokens;
            int savedPos = this.position;

            this.tokens = incrementTokens;
            this.position = 0;
            // Hacky: append EOF to avoid index out of bounds if loops depend on it
            // But we just need to process expressions/assignments.
            while (position < tokens.size()) {
                // simplified handling for increment: assumming it's an assignment or expression
                if (currentToken().type == TokenType.IDENTIFIER) {
                    declarationOrAssignment();
                } else {
                    expression();
                }
                if (position < tokens.size() && currentToken().type == TokenType.SEMICOLON)
                    position++;
            }

            this.tokens = savedTokens;
            this.position = savedPos;
        }

        emit("GOTO", null, null, labelStart);
        emit("LABEL", null, null, labelEnd);
    }

    private void function() {
        eat(TokenType.FUNCTION);
        String name = currentToken().value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);

        emit("LABEL", null, null, "func_" + name);

        // Params
        while (currentToken().type != TokenType.RPAREN) {
            if (currentToken().type == TokenType.IDENTIFIER) {
                emit("pop", null, null, currentToken().value);
                eat(TokenType.IDENTIFIER);
            }
            if (currentToken().type == TokenType.COMMA)
                eat(TokenType.COMMA);
        }
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);

        while (currentToken() != null && currentToken().type != TokenType.RBRACE
                && currentToken().type != TokenType.EOF) {
            instruction();
            if (currentToken() != null && currentToken().type == TokenType.SEMICOLON) {
                eat(TokenType.SEMICOLON);
            }
        }
        eat(TokenType.RBRACE);
        emit("ret", null, null, null);
    }

    private void returnStatement() {
        eat(TokenType.RETURN);
        String result = null;
        if (currentToken().type != TokenType.SEMICOLON) {
            result = expression();
        }
        emit("ret", result, null, null);
    }

    private String functionCall() {
        String name = currentToken().value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);

        List<String> args = new ArrayList<>();
        if (currentToken().type != TokenType.RPAREN) {
            do {
                args.add(expression());
                if (currentToken().type == TokenType.COMMA) {
                    eat(TokenType.COMMA);
                }
            } while (currentToken().type != TokenType.RPAREN);
        }
        eat(TokenType.RPAREN);

        for (String arg : args) {
            emit("param", arg, null, null);
        }

        String result = newTemp();
        emit("call", name, String.valueOf(args.size()), result);
        return result;
    }

    // Expressions
    private String expression() {
        return logicalOr();
    }

    // OR
    private String logicalOr() {
        String left = logicalAnd();
        while (currentToken() != null && currentToken().type == TokenType.OR) {
            eat(TokenType.OR);
            String right = logicalAnd();
            String temp = newTemp();
            emit("OR", left, right, temp);
            left = temp;
        }
        return left;
    }

    // AND
    private String logicalAnd() {
        String left = equality();
        while (currentToken() != null && currentToken().type == TokenType.AND) {
            eat(TokenType.AND);
            String right = equality();
            String temp = newTemp();
            emit("AND", left, right, temp);
            left = temp;
        }
        return left;
    }

    // == !=
    private String equality() {
        String left = relational();
        while (currentToken() != null
                && (currentToken().type == TokenType.EQUALS || currentToken().type == TokenType.NOT_EQUALS)) {
            String op = currentToken().type == TokenType.EQUALS ? "==" : "!=";
            eat(currentToken().type);
            String right = relational();
            String temp = newTemp();
            emit(op, left, right, temp);
            left = temp;
        }
        return left;
    }

    // < > <= >=
    private String relational() {
        String left = additive();
        while (currentToken() != null && (currentToken().type == TokenType.LESS
                || currentToken().type == TokenType.GREATER ||
                currentToken().type == TokenType.LESS_EQUAL || currentToken().type == TokenType.GREATER_EQUAL)) {
            String op = "";
            switch (currentToken().type) {
                case LESS:
                    op = "<";
                    break;
                case GREATER:
                    op = ">";
                    break;
                case LESS_EQUAL:
                    op = "<=";
                    break;
                case GREATER_EQUAL:
                    op = ">=";
                    break;
            }
            eat(currentToken().type);
            String right = additive();
            String temp = newTemp();
            emit(op, left, right, temp);
            left = temp;
        }
        return left;
    }

    private String additive() {
        String left = multiplicative();
        while (currentToken() != null
                && (currentToken().type == TokenType.PLUS || currentToken().type == TokenType.MINUS)) {
            String op = currentToken().type == TokenType.PLUS ? "+" : "-";
            eat(currentToken().type);
            String right = multiplicative();
            String temp = newTemp();
            emit(op, left, right, temp);
            left = temp;
        }
        return left;
    }

    private String multiplicative() {
        String left = unary();
        while (currentToken() != null && (currentToken().type == TokenType.MULTIPLY
                || currentToken().type == TokenType.DIVIDE || currentToken().type == TokenType.MODULO)) {
            String op = "";
            switch (currentToken().type) {
                case MULTIPLY:
                    op = "*";
                    break;
                case DIVIDE:
                    op = "/";
                    break;
                case MODULO:
                    op = "%";
                    break;
            }
            eat(currentToken().type);
            String right = unary();
            String temp = newTemp();
            emit(op, left, right, temp);
            left = temp;
        }
        return left;
    }

    private String unary() {
        if (currentToken().type == TokenType.MINUS) {
            eat(TokenType.MINUS);
            String val = unary();
            String temp = newTemp();
            emit("MINUS", val, null, temp);
            return temp;
        }
        return primary();
    }

    private String primary() {
        Token token = currentToken();
        if (token.type == TokenType.LPAREN) {
            eat(TokenType.LPAREN);
            String expr = expression();
            eat(TokenType.RPAREN);
            return expr;
        } else if (token.type == TokenType.IDENTIFIER) {
            // Check for call
            if (position + 1 < tokens.size() && tokens.get(position + 1).type == TokenType.LPAREN) {
                return functionCall();
            }
            eat(TokenType.IDENTIFIER);
            return token.value;
        } else if (token.type == TokenType.NUMBER) {
            eat(TokenType.NUMBER);
            return token.value;
        } else if (token.type == TokenType.STRING) {
            eat(TokenType.STRING);
            return token.value; // quoted string
        } else if (token.type == TokenType.TRUE) {
            eat(TokenType.TRUE);
            return "true";
        } else if (token.type == TokenType.FALSE) {
            eat(TokenType.FALSE);
            return "false";
        }
        return "";
    }
}
