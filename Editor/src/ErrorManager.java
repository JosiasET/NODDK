import java.util.ArrayList;
import java.util.List;

/**
 * Gestor centralizado de errores para el compilador
 */
public class ErrorManager {
    private List<CompilationError> errors;
    
    public ErrorManager() {
        this.errors = new ArrayList<>();
    }
    
    public static class CompilationError {
        public enum ErrorType {
            LEXICAL, SYNTACTIC, SEMANTIC
        }
        
        public ErrorType type;
        public String message;
        public int line;
        public int column;
        public String details;
        
        public CompilationError(ErrorType type, String message, int line, int column, String details) {
            this.type = type;
            this.message = message;
            this.line = line;
            this.column = column;
            this.details = details;
        }
        
        /**
         * ✅ NUEVO: Formato específico que solicitas
         
        public String getSimpleMessage() {
            String typeStr = "";
            switch (type) {
                case LEXICAL: typeStr = "Léxico"; break;
                case SYNTACTIC: typeStr = "Sintáctico"; break;
                case SEMANTIC: typeStr = "Semántico"; break;
            }
            return String.format("Error %s (Línea %d, Columna %d): %s", 
                typeStr, line, column, details);
        }*/

        public String getSimpleMessage() {
            return String.format("Error(linea %d, columna %d) \"%s\"", 
                line, column, details != null ? details : message);
        }       

        
        
        @Override
        public String toString() {
            return getSimpleMessage();
        }
    }
    

    public String getSimpleErrorsReport() {
        if (errors.isEmpty()) {
            return "No se encontraron errores de compilación";
        }
        
        StringBuilder report = new StringBuilder();
        
        for (CompilationError error : errors) {
            // Formato: Error(linea #, columna #) "Descripción del error"
            report.append("Error(linea ")
                .append(error.line)
                .append(", columna ")
                .append(error.column)
                .append(") \"")
                .append(error.details != null ? error.details : error.message)
                .append("\"\n");
        }
        
        return report.toString().trim();
    }
    // Métodos para agregar errores
    public void addLexicalError(String message, int line, int column, String details) {
        errors.add(new CompilationError(CompilationError.ErrorType.LEXICAL, message, line, column, details));
    }
    
    public void addSyntacticError(String message, int line, int column, String details) {
        errors.add(new CompilationError(CompilationError.ErrorType.SYNTACTIC, message, line, column, details));
    }
    
    public void addSemanticError(String message, int line, int column, String details) {
        errors.add(new CompilationError(CompilationError.ErrorType.SEMANTIC, message, line, column, details));
    }
    
    // Métodos de consulta
    public List<CompilationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<CompilationError> getLexicalErrors() {
        return errors.stream()
            .filter(error -> error.type == CompilationError.ErrorType.LEXICAL)
            .toList();
    }
    
    public List<CompilationError> getSyntacticErrors() {
        return errors.stream()
            .filter(error -> error.type == CompilationError.ErrorType.SYNTACTIC)
            .toList();
    }
    
    public List<CompilationError> getSemanticErrors() {
        return errors.stream()
            .filter(error -> error.type == CompilationError.ErrorType.SEMANTIC)
            .toList();
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasLexicalErrors() {
        return errors.stream().anyMatch(error -> error.type == CompilationError.ErrorType.LEXICAL);
    }
    
    public boolean hasSyntacticErrors() {
        return errors.stream().anyMatch(error -> error.type == CompilationError.ErrorType.SYNTACTIC);
    }
    
    public boolean hasSemanticErrors() {
        return errors.stream().anyMatch(error -> error.type == CompilationError.ErrorType.SEMANTIC);
    }
    
    public int getTotalErrors() {
        return errors.size();
    }
    
    public void clearErrors() {
        errors.clear();
    }
    
    /**
     * ✅ NUEVO: Método para obtener solo los mensajes simples
    ___________________________________________________________________________________________
    public String getSimpleErrorsReport() {
        if (errors.isEmpty()) {
            return "No se encontraron errores de compilación";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("📋 ERRORES DE COMPILACIÓN:\n");
        report.append("=".repeat(60)).append("\n");
        
        for (CompilationError error : errors) {
            report.append(" ").append(error.getSimpleMessage()).append("\n");
        }
        
        report.append("=".repeat(60)).append("\n");
        report.append(String.format("Total: %d error(es)", errors.size()));
        
        return report.toString();
    }*/
    
    /**
     * ✅ NUEVO: Método para obtener errores como lista simple
     */
    public List<String> getSimpleErrorMessages() {
        List<String> simpleMessages = new ArrayList<>();
        for (CompilationError error : errors) {
            simpleMessages.add(error.getSimpleMessage());
        }
        return simpleMessages;
    }
    
    // ... el método getErrorsReport() original se mantiene por si lo necesitas ...
    public String getErrorsReport() {
        if (errors.isEmpty()) {
            return "No se encontraron errores de compilación";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("📊 REPORTE DETALLADO DE ERRORES:\n");
        report.append("=".repeat(80)).append("\n");
        report.append(String.format("Total de errores: %d\n", errors.size()));
        report.append(String.format("Léxicos: %d | Sintácticos: %d | Semánticos: %d\n", 
            getLexicalErrors().size(), getSyntacticErrors().size(), getSemanticErrors().size()));
        report.append("=".repeat(80)).append("\n");
        
        if (hasLexicalErrors()) {
            report.append("\n🔤 ERRORES LÉXICOS:\n");
            report.append("-".repeat(40)).append("\n");
            for (CompilationError error : getLexicalErrors()) {
                report.append(" ").append(error.getSimpleMessage()).append("\n");
            }
        }
        
        if (hasSyntacticErrors()) {
            report.append("\n📝 ERRORES SINTÁCTICOS:\n");
            report.append("-".repeat(40)).append("\n");
            for (CompilationError error : getSyntacticErrors()) {
                report.append(" ").append(error.getSimpleMessage()).append("\n");
            }
        }
        
        if (hasSemanticErrors()) {
            report.append("\n🎯 ERRORES SEMÁNTICOS:\n");
            report.append("-".repeat(40)).append("\n");
            for (CompilationError error : getSemanticErrors()) {
                report.append(" ").append(error.getSimpleMessage()).append("\n");
            }
        }
        
        return report.toString();
    }
}