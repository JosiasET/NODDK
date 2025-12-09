
import java.util.*;

public class TestArduinoParam {
    public static void main(String[] args) {
        System.out.println("🧪 Probando Preservación de Variables en Params...");

        List<TACInstruction> code = new ArrayList<>();
        // edad = 17
        code.add(new TACInstruction("=", "17", null, "edad"));
        // param edad (Should NOT become param 17)
        code.add(new TACInstruction("param", "edad", null, null));
        // t1 = 5
        code.add(new TACInstruction("=", "5", null, "t1"));
        // param t1 (Should become param 5, because t1 is temp)
        code.add(new TACInstruction("param", "t1", null, null));

        TACOptimizer optimizer = new TACOptimizer();
        List<TACInstruction> optimized = optimizer.optimize(code);

        System.out.println("\n--- Result ---");
        boolean edadPreserved = false;
        boolean tempReplaced = false;

        for (TACInstruction inst : optimized) {
            System.out.println(inst);
            if (inst.op.equals("param") && inst.arg1.equals("edad")) {
                edadPreserved = true;
            }
            if (inst.op.equals("param") && inst.arg1.equals("5")) {
                tempReplaced = true;
            }
        }

        if (edadPreserved)
            System.out.println("✅ 'param edad' preserved.");
        else
            System.out.println("❌ 'param edad' replaced by constant!");

        if (tempReplaced)
            System.out.println("✅ 'param t1' replaced by 'param 5'.");
        else
            System.out.println("⚠️ 'param t1' NOT replaced (optional but checking behavior).");
    }
}
