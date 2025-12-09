public class TestOptimizer {
    public static void main(String[] args) {
        System.out.println("TESTING OPTIMIZER FIXES...");

        // Scenario:
        // 1. Calculations should be optimized (Constant Folding)
        // 2. Variables passed to print() should PRESERVE their name (No Constant
        // Propagation into param)

        String code = "int edad = 22;\n" +
                "int futuro = edad + 10;\n" +
                "println(edad);\n" + // Should generate 'param edad', not 'param 22'
                "println(futuro);"; // Should generate 'param futuro', not 'param 32'

        System.out.println("SOURCE CODE:");
        System.out.println(code);
        System.out.println("--------------------------------------------------");

        CompilationManager.CompilationResult result = manager.compile(code);

        if (result.hasErrors()) {
            System.out.println("\nERRORS:");
            System.out.println(result.getFullReport());
        } else {
            System.out.println("\nGENERATED TAC (OPTIMIZED):");
            System.out.println(result.optimizedTacOutput);

            System.out.println("\nGENERATED ARDUINO CODE:");
            System.out.println(result.assemblyOutput);
        }
    }
}
