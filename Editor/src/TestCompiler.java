import java.util.List;

public class TestCompiler {
    public static void main(String[] args) {
        String testCode = "pinMode(13, OUTPUT);\n" +
                "digitalWrite(13, HIGH);\n" +
                "delay(1000);\n" +
                "digitalWrite(13, LOW);\n";

        System.out.println("Testing code:\n" + testCode);

        CompilationManager manager = new CompilationManager();
        CompilationManager.CompilationResult result = manager.compile(testCode);

        if (result.hasErrors()) {
            System.out.println("FAILED: Compilation has errors.");
            System.out.println(result.getFullReport());
            System.exit(1);
        } else {
            System.out.println("SUCCESS: Compilation passed without errors.");
            System.out.println("TAC Output fragment: "
                    + result.tacOutput.substring(0, Math.min(result.tacOutput.length(), 100)) + "...");
            System.exit(0);
        }
    }
}
