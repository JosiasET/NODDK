
import java.util.*;

public class TestArduinoGen {
    public static void main(String[] args) {
        List<TACInstruction> code = new ArrayList<>();
        // Simulate: t5 = contador < 3
        code.add(new TACInstruction("<", "contador", "3", "t5"));
        // Simulate: if (!t5) goto L3
        code.add(new TACInstruction("IF_FALSE", "t5", null, "L3"));

        ArduinoGenerator gen = new ArduinoGenerator();
        String output = gen.generate(code);

        System.out.println("--- Generated Code ---");
        System.out.println(output);

        if (output.contains("double t5")) {
            System.out.println("\n✅ SUCCESS: t5 declared.");
        } else {
            System.out.println("\n❌ FAILURE: t5 NOT declared.");
        }
    }
}
