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
        cpp.append("// Pila de operandos para llamadas a función\n");
        cpp.append("std::vector<double> _stack;\n\n");

        // Variables
        cpp.append("// Variables Variables\n");
        Set<String> declaredVars = new HashSet<>();
        for (TACInstruction inst : instructions) {
            if (inst.result != null && !isLabel(inst.result) && !inst.result.isEmpty()) {
                declaredVars.add(inst.result);
            }
        }

        cpp.append("double ");
        boolean first = true;
        for (String var : declaredVars) {
            if (!first)
                cpp.append(", ");
            cpp.append(var);
            first = false;
        }
        if (!declaredVars.isEmpty())
            cpp.append(";\n\n");
        else
            cpp.append("temp_dummy;\n\n");

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
                cpp.append("_stack.push_back(").append(inst.arg1).append(");\n");
                continue;
            }
            if (inst.op.equals("call")) {
                if (inst.arg1.equals("print") || inst.arg1.equals("println")) {
                    int numArgs = Integer.parseInt(inst.arg2);
                    boolean isLn = inst.arg1.equals("println");
                    // Extraer argumentos de la pila
                    // En C++ esto es runtime.
                    cpp.append("for(int i=0; i<").append(numArgs).append("; i++) {\n");
                    cpp.append("    Serial.print(_stack[_stack.size() - ").append(numArgs).append(" + i]);\n");
                    cpp.append("    if(i < ").append(numArgs - 1).append(") Serial.print(\" \");\n");
                    cpp.append("  }\n");
                    if (isLn)
                        cpp.append("  Serial.println();\n");

                    cpp.append("  _stack.erase(_stack.end() - ").append(numArgs).append(", _stack.end());\n");
                }
                continue;
            }

            switch (inst.op) {
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
}
