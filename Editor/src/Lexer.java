import java.util.*;
import java.util.regex.*;

public class Lexer {
    private final String sourceCode;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private final List<Token> tokens = new ArrayList<>();
    
    private static final Pattern[] PATTERNS = {
        Pattern.compile("^f\"[^\"]*\""),     // FORMATTED_STRING - f"..."
        Pattern.compile("^\"[^\"]*\""),      // STRING normal - "..."
        Pattern.compile("^\\d+\\.\\d+"),     // NUMBER (float)
        Pattern.compile("^\\d+"),            // NUMBER (int)
        Pattern.compile("^'[^']*'"),         // CHAR
        Pattern.compile("^\\+\\+"),          // INCREMENT
        Pattern.compile("^--"),              // DECREMENT
        Pattern.compile("^<="),              // LESS_EQUAL
        Pattern.compile("^>="),              // GREATER_EQUAL
        Pattern.compile("^=="),              // EQUALS
        Pattern.compile("^!="),              // NOT_EQUALS
        Pattern.compile("^&&"),              // AND
        Pattern.compile("^\\|\\|"),          // OR
        Pattern.compile("^print\\b"),        // PRINT
        Pattern.compile("^println\\b"),      // PRINTLN
        Pattern.compile("^input\\b"),        // INPUT
        Pattern.compile("^if\\b"),           // IF
        Pattern.compile("^else\\b"),         // ELSE
        Pattern.compile("^while\\b"),        // WHILE
        Pattern.compile("^for\\b"),          // FOR
        Pattern.compile("^do\\b"),           // DO
        Pattern.compile("^break\\b"),        // BREAK
        Pattern.compile("^return\\b"),       // RETURN
        Pattern.compile("^function\\b"),     // FUNCTION
        Pattern.compile("^true\\b"),         // TRUE
        Pattern.compile("^false\\b"),        // FALSE
        Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*"), // IDENTIFIER
        Pattern.compile("^\\+"),             // PLUS
        Pattern.compile("^-"),               // MINUS
        Pattern.compile("^\\*"),             // MULTIPLY
        Pattern.compile("^/"),               // DIVIDE
        Pattern.compile("^%"),               // MODULO
        Pattern.compile("^="),               // ASSIGN
        Pattern.compile("^<"),               // LESS
        Pattern.compile("^>"),               // GREATER
        Pattern.compile("^!"),               // NOT
        Pattern.compile("^\\("),             // LPAREN
        Pattern.compile("^\\)"),             // RPAREN
        Pattern.compile("^\\{"),             // LBRACE
        Pattern.compile("^\\}"),             // RBRACE
        Pattern.compile("^;"),               // SEMICOLON
        Pattern.compile("^,")                // COMMA
    };
    
    private static final TokenType[] TOKEN_TYPES = {
        TokenType.FORMATTED_STRING,          // f"..."
        TokenType.STRING,                    // "..."
        TokenType.NUMBER, 
        TokenType.NUMBER,
        TokenType.CHAR,
        TokenType.INCREMENT, 
        TokenType.DECREMENT,
        TokenType.LESS_EQUAL, 
        TokenType.GREATER_EQUAL, 
        TokenType.EQUALS, 
        TokenType.NOT_EQUALS, 
        TokenType.AND, 
        TokenType.OR, 
        TokenType.PRINT,
        TokenType.PRINTLN, 
        TokenType.INPUT, 
        TokenType.IF, 
        TokenType.ELSE,
        TokenType.WHILE, 
        TokenType.FOR, 
        TokenType.DO, 
        TokenType.BREAK,
        TokenType.RETURN, 
        TokenType.FUNCTION, 
        TokenType.TRUE, 
        TokenType.FALSE,
        TokenType.IDENTIFIER, 
        TokenType.PLUS, 
        TokenType.MINUS, 
        TokenType.MULTIPLY,
        TokenType.DIVIDE, 
        TokenType.MODULO, 
        TokenType.ASSIGN, 
        TokenType.LESS,
        TokenType.GREATER, 
        TokenType.NOT, 
        TokenType.LPAREN, 
        TokenType.RPAREN,
        TokenType.LBRACE, 
        TokenType.RBRACE, 
        TokenType.SEMICOLON, 
        TokenType.COMMA
    };
    
    public Lexer(String sourceCode) {
        this.sourceCode = sourceCode;
    }
    
    public List<Token> tokenize() {
        while (position < sourceCode.length()) {
            skipWhitespace();
            if (position >= sourceCode.length()) break;
            
            boolean matched = false;
            for (int i = 0; i < PATTERNS.length; i++) {
                Matcher matcher = PATTERNS[i].matcher(sourceCode.substring(position));
                if (matcher.find()) {
                    String value = matcher.group();
                    Token token = new Token(TOKEN_TYPES[i], value, line, column);
                    tokens.add(token);
                    
                    updatePosition(value);
                    matched = true;
                    break;
                }
            }
            
            if (!matched) {
                throw new RuntimeException("Carácter no reconocido: '" + 
                    sourceCode.charAt(position) + "' en línea " + line + ", columna " + column);
            }
        }
        
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }
    
    private void skipWhitespace() {
        while (position < sourceCode.length() && 
               Character.isWhitespace(sourceCode.charAt(position))) {
            if (sourceCode.charAt(position) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            position++;
        }
    }
    
    private void updatePosition(String value) {
        int newlines = countNewlines(value);
        if (newlines > 0) {
            line += newlines;
            column = value.length() - value.lastIndexOf('\n');
        } else {
            column += value.length();
        }
        position += value.length();
    }
    
    private int countNewlines(String str) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }
}