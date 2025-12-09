
import java.util.*;
import java.io.*;

public class DebugOptimizer {
    public static void main(String[] args) throws Exception {
        List<TACInstruction> code = new ArrayList<>();
        code.add(new TACInstruction("=", "0", null, "i"));
        code.add(new TACInstruction("LABEL", null, null, "L1"));
        code.add(new TACInstruction("<", "i", "2", "t1"));
        code.add(new TACInstruction("IF_FALSE", "t1", null, "L2"));
        code.add(new TACInstruction("print", "i", null, null));
        code.add(new TACInstruction("+", "i", "1", "t2"));
        code.add(new TACInstruction("=", "t2", null, "i"));
        code.add(new TACInstruction("GOTO", null, null, "L1"));
        code.add(new TACInstruction("LABEL", null, null, "L2"));

        TACOptimizer optimizer = new TACOptimizer();
        List<TACInstruction> optimized = optimizer.optimize(code);

        PrintWriter writer = new PrintWriter("debug_output.txt");
        for (TACInstruction inst : optimized) {
            writer.println(inst.toString());
        }
        writer.close();
    }
}
