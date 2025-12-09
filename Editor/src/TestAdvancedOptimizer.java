
import java.util.*;

public class TestAdvancedOptimizer {
    public static void main(String[] args) {
        System.out.println("🧪 Probando Optimizador Avanzado (Loops & Fixes)...");

        // TEST CASE: Loop Correction
        // Ensures that optimizations don't break loops by assuming variables are
        // constant
        // Original:
        // i = 0
        // L1:
        // t1 = i < 2
        // ifFalse t1 goto L2
        // print i
        // i = i + 1
        // goto L1
        // L2:

        List<TACInstruction> code = new ArrayList<>();
        code.add(new TACInstruction("=", "0", null, "i"));
        code.add(new TACInstruction("LABEL", null, null, "L1"));
        code.add(new TACInstruction("<", "i", "2", "t1"));
        code.add(new TACInstruction("IF_FALSE", "t1", null, "L2"));
        code.add(new TACInstruction("print", "i", null, null));
        code.add(new TACInstruction("+", "i", "1", "t2")); // i + 1
        code.add(new TACInstruction("=", "t2", null, "i")); // i = t2
        code.add(new TACInstruction("GOTO", null, null, "L1"));
        code.add(new TACInstruction("LABEL", null, null, "L2"));

        System.out.println("\n--- Original Loop Code ---");
        for (TACInstruction inst : code)
            System.out.println(inst);

        TACOptimizer optimizer = new TACOptimizer();
        List<TACInstruction> optimized = optimizer.optimize(code);

        System.out.println("\n--- Optimizado (Debe preservar la lógica del bucle) ---");
        boolean hasComparison = false;
        for (TACInstruction inst : optimized) {
            System.out.println(inst);
            if (inst.op.equals("<"))
                hasComparison = true;
        }

        if (hasComparison) {
            System.out.println("\n✅ ÉXITO: La comparación 'i < 2' se mantuvo (no se asumió constante).");
        } else {
            System.out.println("\n❌ ERROR: La comparación desapareció. ¡El bucle se rompió!");
        }

        // TEST CASE 2: Cosmetics
        System.out.println("\n--- Prueba de Cosmética IR ---");
        TACInstruction t = new TACInstruction("param", "hello", null, null);
        System.out.println("Input: param hello -> Output: " + t.toString());
        TACInstruction t2 = new TACInstruction("=", "123", null, "x");
        System.out.println("Input: x = 123 -> Output: " + t2.toString());
    }
}
