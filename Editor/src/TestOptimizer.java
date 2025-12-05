public class TestOptimizer {
    public static void main(String[] args) {
        // Test Case 1: Constant Folding (2 + 3 * 4 should become 14)
        // Test Case 2: Dead Code Elimination (if (false) block should be removed)
        String code = "println(\"Optimized result: \" + (2 + 3 * 4));\n" +
                "if (false) {\n" +
                "    println(\"This should not exist\");\n" +
                "}";

        System.out.println("SOURCE CODE:");
        System.out.println(code);
        System.out.println();

        CompilationManager manager = new CompilationManager();
        manager.compile(code);
    }
}
