public enum TokenType {
    // Palabras reservadas
    PRINT, PRINTLN, INPUT, IF, ELSE, WHILE, FOR, DO, BREAK, RETURN, FUNCTION,
    TRUE, FALSE, SWITCH, CASE, DEFAULT,
    
    // Identificadores y literales
    IDENTIFIER, NUMBER, STRING, CHAR,
    
    // Operadores
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
    EQUALS, NOT_EQUALS, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL,
    AND, OR, NOT,
    INCREMENT, DECREMENT,
    ASSIGN,
    
    // Delimitadores
    LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON, COMMA, COLON,
    
    // Especiales
    FORMATTED_STRING, EOF
}