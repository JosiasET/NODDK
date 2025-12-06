import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class ArduinoGenerator {

    public String generate(List<TACInstruction> instructions) {
        StringBuilder cpp = new StringBuilder();

        cpp.append("// Código generado para ESP32 (Arduino Framework)\n");
        cpp.append("#include <Arduino.h>\n\n");
        cpp.append("#include <vector>\n");

        // Definir estructura Variant para manejar números y texto en la pila
        cpp.append("struct Variant {\n");
        cpp.append("    double numVal;\n");
        cpp.append("    String strVal;\n");
        cpp.append("    int type; // 0=num, 1=str\n\n");
        cpp.append("    Variant(double d) : numVal(d), type(0) {}\n");
        cpp.append("    Variant(int i) : numVal(i), type(0) {}\n"); // constructor int
        cpp.append("    Variant(String s) : strVal(s), type(1) { numVal = 0; }\n");
        cpp.append("    Variant(const char* s) : strVal(s), type(1) { numVal = 0; }\n"); // constructor char*
        cpp.append("};\n\n");

        cpp.append("// Pila de operandos\n");
        cpp.append("std::vector<Variant> _stack;\n\n");

        // Inferencia de Tipos Básica
        cpp.append("// Variables Globales\n");
        Set<String> declaredVars = new HashSet<>();
        Set<String> stringVars = new HashSet<>();
        Set<String> doubleVars = new HashSet<>();

        // Fase 1: Detectar Variables y Tipos
        for (TACInstruction inst : instructions) {

            // Asignaciones directas STRING: s = "hola"
            if (inst.op.equals("=")) {
                if (inst.arg1 != null && inst.arg1.startsWith("\"")) {
                    stringVars.add(inst.result);
                    declaredVars.add(inst.result);
                } else {
                    // Posiblemente número u otra variable
                    if (inst.result != null && !inst.result.isEmpty() && !declaredVars.contains(inst.result)) {
                        doubleVars.add(inst.result);
                        declaredVars.add(inst.result);
                    }
                }
            }

            // Resultado de Operaciones Matemáticas -> double
            else if (isMathOp(inst.op)) {
                if (inst.result != null) {
                    doubleVars.add(inst.result);
                    declaredVars.add(inst.result);
                }
            }
        }

        doubleVars.removeAll(stringVars);

        // Declarar Doubles
        if (!doubleVars.isEmpty()) {
            cpp.append("double ");
            boolean first = true;
            for (String var : doubleVars) {
                if (!isLabel(var)) {
                    if (!first)
                        cpp.append(", ");
                    cpp.append(var);
                    first = false;
                }
            }
            cpp.append(";\n");
        }

        // Declarar Strings
        if (!stringVars.isEmpty()) {
            cpp.append("String ");
            boolean first = true;
            for (String var : stringVars) {
                if (!first)
                    cpp.append(", ");
                cpp.append(var);
                first = false;
            }
            cpp.append(";\n");
        }
        cpp.append("\n");

        cpp.append("void setup() {\n");
        cpp.append("  Serial.begin(115200);\n");
        cpp.append("  delay(1000);\n");
        cpp.append("  Serial.println(\"--- INICIO ---\");\n");
        cpp.append("}\n\n");

        cpp.append("void loop() {\n");

        for (TACInstruction inst : instructions) {
            if (inst.op.equals("LABEL")) {
                cpp.append(inst.result).append(":\n");
                continue;
            }

            cpp.append("  ");

            // Manejo especial PRINT
            if (inst.op.equals("param")) {
                // El argumento puede ser numero, string literal o variable
                // C++ Variant constructor handles overloads
                cpp.append("_stack.push_back(Variant(").append(inst.arg1).append("));\n");
                continue;
            }
            if (inst.op.equals("call")) {
                if (inst.arg1.equals("print") || inst.arg1.equals("println")) {
                    int numArgs = Integer.parseInt(inst.arg2);
                    boolean isLn = inst.arg1.equals("println");

                    cpp.append("for(int i=0; i<").append(numArgs).append("; i++) {\n");
                    cpp.append("    Variant v = _stack[_stack.size() - ").append(numArgs).append(" + i];\n");
                    cpp.append("    if(v.type == 1) Serial.print(v.strVal);\n");
                    cpp.append("    else Serial.print(v.numVal);\n");

                    cpp.append("    if(i < ").append(numArgs - 1).append(") Serial.print(\" \");\n");
                    cpp.append("  }\n");
                    if (isLn)
                        cpp.append("  Serial.println();\n");

                    cpp.append("  _stack.erase(_stack.end() - ").append(numArgs).append(", _stack.end());\n");
                }
                // ✅ SOPORTE PARA FUNCIONES DE ARDUINO (Adaptado a Variant)
                else if (inst.arg1.equals("pinMode")) {
                    cpp.append("  {\n");
                    cpp.append("    int mode = (int)_stack.back().numVal; _stack.pop_back();\n");
                    cpp.append("    int pin = (int)_stack.back().numVal; _stack.pop_back();\n");
                    cpp.append("    pinMode(pin, mode);\n");
                    cpp.append("  }\n");
                } else if (inst.arg1.equals("digitalWrite")) {
                    cpp.append("  {\n");
                    cpp.append("    int val = (int)_stack.back().numVal; _stack.pop_back();\n");
                    cpp.append("    int pin = (int)_stack.back().numVal; _stack.pop_back();\n");
                    cpp.append("    digitalWrite(pin, val);\n");
                    cpp.append("  }\n");
                } else if (inst.arg1.equals("delay")) {
                    cpp.append("  {\n");
                    cpp.append("    int ms = (int)_stack.back().numVal; _stack.pop_back();\n");
                    cpp.append("    delay(ms);\n");
                    cpp.append("  }\n");
                } else if (inst.arg1.equals("digitalRead")) {
                    cpp.append("  {\n");
                    cpp.append("    int pin = (int)_stack.back().numVal; _stack.pop_back();\n");
                    cpp.append("    double val = (double)digitalRead(pin);\n");
                    if (inst.result != null && !inst.result.isEmpty()) {
                        cpp.append("    ").append(inst.result).append(" = val;\n");
                    }
                    cpp.append("  }\n");
                }

                continue;
            }

            switch (inst.op)

            {
                case "=":
                    cpp.append(inst.result).append(" = ").append(inst.arg1).append(";\n");
                    break;
                case "+":
                    cpp.append(inst.result).append(" = ").append(inst.arg1).append(" + ").append(inst.arg2)
                            .append(";\n");
                    break;
                case "-":
                    cpp.append(inst.result).append(" = ").append(inst.arg1).append(" - ").append(inst.arg2)
                            .append(";\n");
                    break;
                case "*":
                    cpp.append(inst.result).append(" = ").append(inst.arg1).append(" * ").append(inst.arg2)
                            .append(";\n");
                    break;
                case "/":
                    cpp.append(inst.result).append(" = ").append(inst.arg1).append(" / ").append(inst.arg2)
                            .append(";\n");
                    break;
                case "%":
                    cpp.append(inst.result).append(" = (int)").append(inst.arg1).append(" % (int)").append(inst.arg2)
                            .append(";\n");
                    break;
                case "==":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" == ").append(inst.arg2)
                            .append(");\n");
                    break;
                case "!=":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" != ").append(inst.arg2)
                            .append(");\n");
                    break;
                case "<":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" < ").append(inst.arg2)
                            .append(");\n");
                    break;
                case ">":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" > ").append(inst.arg2)
                            .append(");\n");
                    break;
                case "<=":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" <= ").append(inst.arg2)
                            .append(");\n");
                    break;
                case ">=":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" >= ").append(inst.arg2)
                            .append(");\n");
                    break;
                case "AND":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" && ").append(inst.arg2)
                            .append(");\n");
                    break;
                case "OR":
                    cpp.append(inst.result).append(" = (").append(inst.arg1).append(" || ").append(inst.arg2)
                            .append(");\n");
                    break;
                case "GOTO":
                    cpp.append("goto ").append(inst.result).append(";\n");
                    break;
                case "IF_FALSE":
                    cpp.append("if (!").append(inst.arg1).append(") goto ").append(inst.result).append(";\n");
                    break;
                case "MINUS":
                    cpp.append(inst.result).append(" = -").append(inst.arg1).append(";\n");
                    break;
            }
        }

        cpp.append("\n  // Fin del programa\n");
        cpp.append("  while(1) { delay(100); }\n");
        cpp.append("}\n");
        return cpp.toString();

    }

    private boolean isLabel(String s) {
        return s.startsWith("L") && s.matches("L\\d+");
    }

    private boolean isMathOp(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%");
    }
}
