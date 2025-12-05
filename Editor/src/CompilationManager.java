import java.util.*;

/**
 * Coordinador principal de todo el proceso de compilación
 */
public class CompilationManager {
    private ErrorManager errorManager;
    private SemanticAnalyzer semanticAnalyzer;

    public CompilationManager() {
        this.errorManager = new ErrorManager();
        this.semanticAnalyzer = new SemanticAnalyzer(errorManager);
    }

    public CompilationResult compile(String sourceCode) {
        errorManager.clearErrors();
        semanticAnalyzer.clear();

        System.out.println("🔍 INICIANDO COMPILACIÓN...");
        System.out.println("=".repeat(60));

        // 1️⃣ ANÁLISIS LÉXICO
        System.out.println("1️⃣  ANALIZANDO LÉXICO...");
        Lexer lexer = new Lexer(sourceCode, errorManager);
        List<Token> tokens = null;

        try {
            tokens = lexer.tokenize();
            System.out.println("   ✅ Tokens generados: " + tokens.size());
        } catch (Exception e) {
            System.out.println("   ❌ Error en análisis léxico: " + e.getMessage());
        }

        if (errorManager.hasLexicalErrors()) {
            System.out.println("   ⚠️  Errores léxicos encontrados: " + errorManager.getLexicalErrors().size());
        }

        // 🚀 OPTIMIZACIÓN DE CÓDIGO (TOKENS)
        // Nota: Mantenemos la optimización de tokens existente si se desea,
        // pero la optimización principal pedida suele ser sobre el código intermedio.
        if (tokens != null && !errorManager.hasLexicalErrors()) {
            Optimizer optimizer = new Optimizer();
            tokens = optimizer.optimize(tokens);
        }

        // 2️⃣ ANÁLISIS SINTÁCTICO
        String syntacticOutput = "";
        if (tokens != null && !errorManager.hasLexicalErrors()) {
            System.out.println("2️⃣  ANALIZANDO SINTÁCTICO...");
            Parser parser = new Parser(tokens, semanticAnalyzer, errorManager);

            try {
                syntacticOutput = parser.parse();
                if (!errorManager.hasSyntacticErrors()) {
                    System.out.println("   ✅ Análisis sintáctico completado exitosamente");
                } else {
                    System.out.println("   ⚠️  Análisis sintáctico completado con errores: "
                            + errorManager.getSyntacticErrors().size());
                }
            } catch (Exception e) {
                System.out.println("   ❌ Error en análisis sintáctico: " + e.getMessage());
                if (!errorManager.hasSyntacticErrors()) {
                    errorManager.addSyntacticError("Error durante el análisis sintáctico", 1, 1, e.getMessage());
                }
            }
        } else {
            System.out.println("2️⃣  SALTANDO ANÁLISIS SINTÁCTICO (errores léxicos previos)");
        }

        // 3️⃣ ANÁLISIS SEMÁNTICO
        String semanticOutput = "";
        if (tokens != null && !errorManager.hasLexicalErrors()) {
            System.out.println("3️⃣  ANALIZANDO SEMÁNTICO...");

            try {
                // ✅ LLAMAR AL MÉTODO LOCAL EN VEZ DEL DE CodeEditor
                boolean semanticValid = performSemanticAnalysis(tokens, semanticAnalyzer);

                if (semanticValid && !errorManager.hasSemanticErrors()) {
                    System.out.println("   ✅ Análisis semántico completado exitosamente");
                    semanticOutput = semanticAnalyzer.getSymbolTableAsString();
                } else {
                    System.out.println(
                            "   ⚠️  Errores semánticos encontrados: " + errorManager.getSemanticErrors().size());
                    semanticOutput = semanticAnalyzer.getErrorsAsString();
                }
            } catch (Exception e) {
                System.out.println("   ❌ Error en análisis semántico: " + e.getMessage());
                errorManager.addSemanticError("Error durante el análisis semántico", 1, 1, e.getMessage());
            }
        } else {
            System.out.println("3️⃣  SALTANDO ANÁLISIS SEMÁNTICO (errores previos)");
        }

        // 4️⃣ GENERACIÓN DE CÓDIGO INTERMEDIO (TAC), OPTIMIZACIÓN Y ENSAMBLADOR
        StringBuilder tacOutput = new StringBuilder();
        StringBuilder optimizedTacOutput = new StringBuilder();
        StringBuilder assemblyOutput = new StringBuilder();

        // Declarar listas de instrucciones fuera del bloque if para que sean accesibles
        // al retornar
        List<TACInstruction> tacInstructions = new ArrayList<>();
        List<TACInstruction> optimizedInstructions = new ArrayList<>();

        if (tokens != null && !errorManager.hasErrors()) {
            System.out.println("4️⃣  GENERANDO CÓDIGO DE TRES DIRECCIONES...");
            try {
                // Generar TAC
                TACGenerator tacGenerator = new TACGenerator(tokens);
                tacInstructions = tacGenerator.generate();

                tacOutput.append("=== CÓDIGO DE TRES DIRECCIONES ===\n");
                for (TACInstruction inst : tacInstructions) {
                    tacOutput.append(inst.toString()).append("\n");
                }

                // Optimizar TAC
                System.out.println("5️⃣  OPTIMIZANDO CÓDIGO DE TRES DIRECCIONES...");
                TACOptimizer tacOptimizer = new TACOptimizer();
                optimizedInstructions = tacOptimizer.optimize(tacInstructions);

                optimizedTacOutput.append("=== CÓDIGO OPTIMIZADO (TAC) ===\n");
                for (TACInstruction inst : optimizedInstructions) {
                    optimizedTacOutput.append(inst.toString()).append("\n");
                }

                // Generar Ensamblador
                System.out.println("6️⃣  GENERANDO CÓDIGO ENSAMBLADOR...");
                AssemblerGenerator assemblerGenerator = new AssemblerGenerator();
                String asm = assemblerGenerator.generate(optimizedInstructions);
                assemblyOutput.append(asm);

                // Generar Arduino (C++)
                System.out.println("7️⃣  GENERANDO CÓDIGO ARDUINO (ESP32)...");
                ArduinoGenerator arduinoGenerator = new ArduinoGenerator();
                String arduinoCode = arduinoGenerator.generate(optimizedInstructions);

                assemblyOutput.append("\n\n=== CÓDIGO ARDUINO (ESP32) ===\n");
                assemblyOutput.append(arduinoCode);

            } catch (Exception e) {
                System.out.println("   ❌ Error en generación de código: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=".repeat(60));
        System.out.println("🏁 COMPILACIÓN FINALIZADA");

        return new CompilationResult(tokens, syntacticOutput, semanticOutput,
                tacOutput.toString(), optimizedTacOutput.toString(), assemblyOutput.toString(),
                errorManager, tacInstructions, optimizedInstructions);
    }

    /**
     * ✅ MÉTODO COPIADO DE CodeEditor - Realiza el análisis semántico
     */
    private boolean performSemanticAnalysis(List<Token> tokens, SemanticAnalyzer semanticAnalyzer) {
        try {
            boolean inFunction = false;
            String currentFunction = null;
            List<String> functionParams = new ArrayList<>();

            // ✅ PRIMERA PASADA: Declarar todas las variables (SOLO la primera asignación)
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);

                if (token.type == TokenType.FUNCTION) {
                    inFunction = true;
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type == TokenType.IDENTIFIER) {
                        currentFunction = tokens.get(i + 1).value;
                        i++;
                    }
                    continue;
                }

                if (token.type == TokenType.RBRACE && inFunction) {
                    inFunction = false;
                    currentFunction = null;
                    functionParams.clear();
                    semanticAnalyzer.exitScope();
                    continue;
                }

                if (inFunction && currentFunction != null && token.type == TokenType.LPAREN) {
                    int j = i + 1;
                    while (j < tokens.size() && tokens.get(j).type != TokenType.RPAREN) {
                        if (tokens.get(j).type == TokenType.IDENTIFIER) {
                            String paramName = tokens.get(j).value;
                            functionParams.add(paramName);
                            if (!semanticAnalyzer.isReservedWord(paramName)) {
                                semanticAnalyzer.declareVariable(paramName, "unknown", tokens.get(j).line);
                            }
                        }
                        j++;
                    }
                    continue;
                }

                // ✅ DECLARACIÓN DE VARIABLES - SOLO si no estamos en una función
                if (!inFunction && token.type == TokenType.IDENTIFIER && i + 2 < tokens.size()) {
                    Token next = tokens.get(i + 1);
                    Token nextNext = tokens.get(i + 2);

                    if (next.type == TokenType.ASSIGN) {
                        String identifier = token.value;

                        if (!semanticAnalyzer.isReservedWord(identifier)) {
                            // ✅ VERIFICAR si la variable YA EXISTE (es reasignación, no declaración)
                            if (!semanticAnalyzer.variableExists(identifier)) {
                                Object value = extractValueFromToken(nextNext);

                                if (value != null) {
                                    // ✅ SOLO declarar si no existe
                                    semanticAnalyzer.checkDeclaration(identifier, value, token.line);
                                } else {
                                    semanticAnalyzer.declareVariable(identifier, "unknown", token.line);
                                }
                            }
                            // Si ya existe, no hacemos nada aquí (se manejará en reasignaciones)
                        }
                    }
                }

                if (token.type == TokenType.LBRACE && currentFunction != null) {
                    semanticAnalyzer.enterScope("func_" + currentFunction);
                }
            }

            // ✅ SEGUNDA PASADA: Manejar REASIGNACIONES y operaciones
            for (int i = 0; i < tokens.size() - 2; i++) {
                Token token = tokens.get(i);

                // ✅ DETECTAR REASIGNACIONES de variables existentes
                if (token.type == TokenType.IDENTIFIER && i + 2 < tokens.size()) {
                    Token next = tokens.get(i + 1);
                    Token nextNext = tokens.get(i + 2);

                    if (next.type == TokenType.ASSIGN) {
                        String identifier = token.value;

                        if (!semanticAnalyzer.isReservedWord(identifier)) {
                            Object value = extractValueFromToken(nextNext);

                            if (value != null) {
                                if (semanticAnalyzer.variableExists(identifier)) {
                                    // ✅ ES UNA REASIGNACIÓN
                                    semanticAnalyzer.checkAssignment(identifier, value, token.line);
                                } else {
                                    // ✅ ES UNA DECLARACIÓN TARDÍA (no se detectó en primera pasada)
                                    semanticAnalyzer.checkDeclaration(identifier, value, token.line);
                                }
                            }
                        }
                    }
                }

                // ✅ DETECTAR OPERACIONES CON TIPOS INCOMPATIBLES
                if (i > 0 && i < tokens.size() - 1 && isOperator(token.type) && !inFunction) {
                    Token prev = tokens.get(i - 1);
                    Token next = tokens.get(i + 1);

                    String operator = getOperatorSymbol(token.type);

                    // ✅ CASO 1: Ambos son identificadores (variables)
                    if (prev.type == TokenType.IDENTIFIER && next.type == TokenType.IDENTIFIER) {
                        String leftVar = prev.value;
                        String rightVar = next.value;

                        if (!semanticAnalyzer.isReservedWord(leftVar) &&
                                !semanticAnalyzer.isReservedWord(rightVar) &&
                                semanticAnalyzer.variableExists(leftVar) &&
                                semanticAnalyzer.variableExists(rightVar)) {

                            SemanticAnalyzer.VariableInfo leftInfo = semanticAnalyzer.getVariableInfo(leftVar);
                            SemanticAnalyzer.VariableInfo rightInfo = semanticAnalyzer.getVariableInfo(rightVar);

                            // ✅ VERIFICAR TIPOS ESTRICTAMENTE
                            if (!leftInfo.type.equals(rightInfo.type)) {
                                semanticAnalyzer.addError("Línea " + token.line +
                                        ": No se puede operar " + leftInfo.type + " '" + leftVar +
                                        "' con " + rightInfo.type + " '" + rightVar + "'");
                            } else {
                                semanticAnalyzer.checkBinaryOperationTypes(
                                        leftInfo.value, rightInfo.value, operator, token.line);
                            }
                        }
                    }

                    // ✅ CASO 2: Izquierda es variable, derecha es literal
                    else if (prev.type == TokenType.IDENTIFIER &&
                            (next.type == TokenType.NUMBER || next.type == TokenType.STRING ||
                                    next.type == TokenType.TRUE || next.type == TokenType.FALSE)) {

                        String leftVar = prev.value;

                        if (!semanticAnalyzer.isReservedWord(leftVar) && semanticAnalyzer.variableExists(leftVar)) {
                            SemanticAnalyzer.VariableInfo leftInfo = semanticAnalyzer.getVariableInfo(leftVar);
                            Object rightValue = extractValueFromToken(next);
                            String rightType = semanticAnalyzer.inferTypeFromValue(rightValue, next.line);

                            if (!leftInfo.type.equals(rightType)) {
                                semanticAnalyzer.addError("Línea " + token.line +
                                        ": No se puede operar " + leftInfo.type + " '" + leftVar +
                                        "' con " + rightType + " '" + next.value + "'");
                            } else {
                                semanticAnalyzer.checkBinaryOperationTypes(
                                        leftInfo.value, rightValue, operator, token.line);
                            }
                        }
                    }

                    // ✅ CASO 3: Izquierda es literal, derecha es variable
                    else if ((prev.type == TokenType.NUMBER || prev.type == TokenType.STRING ||
                            prev.type == TokenType.TRUE || prev.type == TokenType.FALSE) &&
                            next.type == TokenType.IDENTIFIER) {

                        String rightVar = next.value;

                        if (!semanticAnalyzer.isReservedWord(rightVar) && semanticAnalyzer.variableExists(rightVar)) {
                            Object leftValue = extractValueFromToken(prev);
                            String leftType = semanticAnalyzer.inferTypeFromValue(leftValue, prev.line);
                            SemanticAnalyzer.VariableInfo rightInfo = semanticAnalyzer.getVariableInfo(rightVar);

                            if (!leftType.equals(rightInfo.type)) {
                                semanticAnalyzer.addError("Línea " + token.line +
                                        ": No se puede operar " + leftType + " '" + prev.value +
                                        "' con " + rightInfo.type + " '" + rightVar + "'");
                            } else {
                                semanticAnalyzer.checkBinaryOperationTypes(
                                        leftValue, rightInfo.value, operator, token.line);
                            }
                        }
                    }

                    // ✅ CASO 4: Ambos son literales
                    else if ((prev.type == TokenType.NUMBER || prev.type == TokenType.STRING ||
                            prev.type == TokenType.TRUE || prev.type == TokenType.FALSE) &&
                            (next.type == TokenType.NUMBER || next.type == TokenType.STRING ||
                                    next.type == TokenType.TRUE || next.type == TokenType.FALSE)) {

                        Object leftValue = extractValueFromToken(prev);
                        Object rightValue = extractValueFromToken(next);

                        if (leftValue != null && rightValue != null) {
                            semanticAnalyzer.checkBinaryOperationTypes(
                                    leftValue, rightValue, operator, token.line);
                        }
                    }
                }
            }

            // ✅ TERCERA PASADA: Verificar uso de variables no inicializadas
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);

                if (token.type == TokenType.IDENTIFIER &&
                        !semanticAnalyzer.isReservedWord(token.value) &&
                        semanticAnalyzer.variableExists(token.value)) {

                    // Verificar que la variable esté inicializada cuando se usa (no en
                    // asignaciones)
                    if (i > 0) {
                        Token prev = tokens.get(i - 1);
                        // Si no es una asignación, verificar inicialización
                        if (prev.type != TokenType.ASSIGN) {
                            semanticAnalyzer.checkVariableInitialized(token.value, token.line);
                        }
                    } else if (i == 0) {
                        // Si es el primer token y no es asignación, verificar inicialización
                        semanticAnalyzer.checkVariableInitialized(token.value, token.line);
                    }
                }
            }

            return !semanticAnalyzer.hasErrors();

        } catch (Exception e) {
            System.err.println("Error en análisis semántico: " + e.getMessage());
            e.printStackTrace();
            semanticAnalyzer.addError("Error durante el análisis semántico: " + e.getMessage());
            return false;
        }
    }

    // ✅ MÉTODOS AUXILIARES COPIADOS DE CodeEditor

    private Object extractValueFromToken(Token token) {
        if (token == null)
            return null;

        try {
            switch (token.type) {
                case NUMBER:
                    if (token.value.contains(".")) {
                        return Float.parseFloat(token.value);
                    } else {
                        return Integer.parseInt(token.value);
                    }
                case STRING:
                    return token.value.substring(1, token.value.length() - 1);
                case TRUE:
                    return true;
                case FALSE:
                    return false;
                case IDENTIFIER:
                    if (semanticAnalyzer.variableExists(token.value)) {
                        SemanticAnalyzer.VariableInfo info = semanticAnalyzer.getVariableInfo(token.value);
                        return info.value;
                    }
                    if (token.value.matches(".*[0-9].*")) {
                        return 0;
                    } else {
                        return "";
                    }
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parseando valor: " + token.value);
            return null;
        }
    }

    private boolean isOperator(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINUS ||
                type == TokenType.MULTIPLY || type == TokenType.DIVIDE ||
                type == TokenType.MODULO || type == TokenType.EQUALS ||
                type == TokenType.NOT_EQUALS || type == TokenType.LESS ||
                type == TokenType.GREATER || type == TokenType.LESS_EQUAL ||
                type == TokenType.GREATER_EQUAL || type == TokenType.AND ||
                type == TokenType.OR;
    }

    private String getOperatorSymbol(TokenType type) {
        switch (type) {
            case PLUS:
                return "+";
            case MINUS:
                return "-";
            case MULTIPLY:
                return "*";
            case DIVIDE:
                return "/";
            case MODULO:
                return "%";
            case EQUALS:
                return "==";
            case NOT_EQUALS:
                return "!=";
            case LESS:
                return "<";
            case GREATER:
                return ">";
            case LESS_EQUAL:
                return "<=";
            case GREATER_EQUAL:
                return ">=";
            case AND:
                return "&&";
            case OR:
                return "||";
            default:
                return "";
        }
    }

    public static class CompilationResult {
        public final List<Token> tokens;
        public final String syntacticOutput;
        public final String semanticOutput;
        public final String tacOutput;
        public final String optimizedTacOutput;
        public final String assemblyOutput;
        public final ErrorManager errorManager;

        // Nuevos campos para acceso directo a instrucciones
        public final List<TACInstruction> tacInstructions;
        public final List<TACInstruction> optimizedTacInstructions;

        public CompilationResult(List<Token> tokens, String syntacticOutput,
                String semanticOutput, String tacOutput, String optimizedTacOutput, String assemblyOutput,
                ErrorManager errorManager,
                List<TACInstruction> tacInstructions,
                List<TACInstruction> optimizedTacInstructions) {
            this.tokens = tokens;
            this.syntacticOutput = syntacticOutput;
            this.semanticOutput = semanticOutput;
            this.tacOutput = tacOutput;
            this.optimizedTacOutput = optimizedTacOutput;
            this.assemblyOutput = assemblyOutput;
            this.errorManager = errorManager;
            this.tacInstructions = tacInstructions;
            this.optimizedTacInstructions = optimizedTacInstructions;
        }

        public boolean hasErrors() {
            return errorManager.hasErrors();
        }

        public String getFullReport() {
            // ✅ SOLO MOSTRAR LOS ERRORES, NADA MÁS
            return errorManager.getSimpleErrorsReport();
        }

        /**
         * ✅ Método para obtener solo los errores (por si lo necesitas)
         */
        public String getSimpleErrors() {
            return errorManager.getSimpleErrorsReport();
        }
    }
}