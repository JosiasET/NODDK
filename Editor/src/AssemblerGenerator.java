import java.util.*;

public class AssemblerGenerator {
    public String generate(List<TACInstruction> instructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("; Código Ensamblador Generado\n");
        sb.append(".data\n");
        sb.append("    ; Variables declaradas aquí implícitamente\n");
        sb.append(".text\n");
        sb.append(".global main\n");
        sb.append("main:\n");

        for (TACInstruction inst : instructions) {
            sb.append(translate(inst));
        }

        // Final exit
        sb.append("    MOV EAX, 1      ; sys_exit\n");
        sb.append("    INT 0x80\n");

        return sb.toString();
    }

    private String translate(TACInstruction inst) {
        StringBuilder sb = new StringBuilder();
        // Comment showing original code
        sb.append("    ; ").append(inst.toString()).append("\n");

        String op = inst.op;
        String r = inst.result;
        String a1 = inst.arg1;
        String a2 = inst.arg2;

        if (op.equals("LABEL")) {
            return r + ":\n";
        }

        if (op.equals("GOTO")) {
            return "    JMP " + r + "\n";
        }

        if (op.equals("IF_FALSE")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    CMP EAX, 0\n"); // 0 is false
            sb.append("    JE ").append(r).append("\n");
            return sb.toString();
        }

        if (op.equals("=")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    MOV ").append(r).append(", EAX\n");
            return sb.toString();
        }

        if (op.equals("+")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    ADD EAX, ").append(resolve(a2)).append("\n");
            sb.append("    MOV ").append(r).append(", EAX\n");
            return sb.toString();
        }

        if (op.equals("-")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    SUB EAX, ").append(resolve(a2)).append("\n");
            sb.append("    MOV ").append(r).append(", EAX\n");
            return sb.toString();
        }

        if (op.equals("*")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    MOV EBX, ").append(resolve(a2)).append("\n");
            sb.append("    MUL EBX\n");
            sb.append("    MOV ").append(r).append(", EAX\n");
            return sb.toString();
        }

        if (op.equals("/")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    MOV EBX, ").append(resolve(a2)).append("\n");
            sb.append("    DIV EBX\n");
            sb.append("    MOV ").append(r).append(", EAX\n");
            return sb.toString();
        }

        if (op.equals("param")) {
            sb.append("    PUSH ").append(resolve(a1)).append("\n");
            return sb.toString();
        }

        if (op.equals("call")) {
            sb.append("    CALL ").append(a1).append("\n");
            // Clean up stack? ADD ESP, 4*num_params
            if (a2 != null) {
                try {
                    int n = Integer.parseInt(a2);
                    if (n > 0)
                        sb.append("    ADD ESP, ").append(n * 4).append("\n");
                } catch (Exception e) {
                }
            }
            if (r != null) {
                sb.append("    MOV ").append(r).append(", EAX\n");
            }
            return sb.toString();
        }

        if (op.equals("ret")) {
            if (a1 != null) {
                sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            }
            sb.append("    RET\n");
            return sb.toString();
        }

        // Logic
        if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">")) {
            sb.append("    MOV EAX, ").append(resolve(a1)).append("\n");
            sb.append("    CMP EAX, ").append(resolve(a2)).append("\n");
            // Set condition logic is complex in simple conversion, using jump helper or
            // SETcc
            // Simplified:
            sb.append("    MOV EAX, 0\n"); // default false
            // TODO: Correct SETcc instructions
        }

        return sb.toString();
    }

    // Determine if operand is variable or immediate
    private String resolve(String arg) {
        if (Character.isDigit(arg.charAt(0)) || arg.equals("true") || arg.equals("false")) {
            if (arg.equals("true"))
                return "1";
            if (arg.equals("false"))
                return "0";
            return arg; // Immediate
        }
        return "[" + arg + "]"; // Memory access
    }
}
