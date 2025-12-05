import java.util.*;

public class TACOptimizer {

    public List<TACInstruction> optimize(List<TACInstruction> instructions) {
        List<TACInstruction> current = new ArrayList<>(instructions);
        boolean changed;
        do {
            changed = false;
            // 1. Constant Folding & Propagation
            if (performConstantFolding(current))
                changed = true;

            // 2. Dead Code Elimination
            if (performDeadCodeElimination(current))
                changed = true;

        } while (changed);
        return current;
    }

    private boolean performConstantFolding(List<TACInstruction> instructions) {
        boolean changed = false;
        Map<String, String> constants = new HashMap<>();

        for (int i = 0; i < instructions.size(); i++) {
            TACInstruction inst = instructions.get(i);

            // Try to resolve operands
            String arg1 = constants.getOrDefault(inst.arg1, inst.arg1);
            String arg2 = constants.getOrDefault(inst.arg2, inst.arg2);

            if (!Objects.equals(arg1, inst.arg1) || !Objects.equals(arg2, inst.arg2)) {
                inst.arg1 = arg1;
                inst.arg2 = arg2;
                changed = true;
            }

            // Check if we can fold
            if (inst.result != null && isArithmeticOrRelational(inst.op)) {
                if (isNumber(inst.arg1) && isNumber(inst.arg2)) {
                    try {
                        double v1 = Double.parseDouble(inst.arg1);
                        double v2 = Double.parseDouble(inst.arg2);
                        String res = compute(inst.op, v1, v2);

                        // Replace instruction with assignment
                        inst.op = "=";
                        inst.arg1 = res;
                        inst.arg2 = null;
                        changed = true;
                    } catch (Exception e) {
                        // ignore div by zero etc
                    }
                }
            }

            // If assignment to constant, store in map
            if (inst.op.equals("=") && isNumber(inst.arg1) && inst.result != null) {
                constants.put(inst.result, inst.arg1);
            }
        }
        return changed;
    }

    private boolean performDeadCodeElimination(List<TACInstruction> instructions) {
        // Count usages
        Map<String, Integer> usages = new HashMap<>();
        for (TACInstruction inst : instructions) {
            if (inst.arg1 != null && inst.arg1.startsWith("t")) {
                usages.put(inst.arg1, usages.getOrDefault(inst.arg1, 0) + 1);
            }
            if (inst.arg2 != null && inst.arg2.startsWith("t")) {
                usages.put(inst.arg2, usages.getOrDefault(inst.arg2, 0) + 1);
            }
            if (inst.op.equals("param") && inst.arg1 != null) {
                // param is a usage
            }
            if (inst.op.equals("IF_FALSE") && inst.arg1 != null) {
                // usage
            }
        }

        boolean changed = false;
        Iterator<TACInstruction> it = instructions.iterator();
        while (it.hasNext()) {
            TACInstruction inst = it.next();
            // removing only assignments to temporaries that are not used
            if ((inst.op.equals("=") || isArithmeticOrRelational(inst.op)) &&
                    inst.result != null && inst.result.startsWith("t")) {

                if (!usages.containsKey(inst.result)) {
                    it.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean isArithmeticOrRelational(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") ||
                op.equals("%") || op.equals("==") || op.equals("!=") || op.equals("<") ||
                op.equals(">") || op.equals("<=") || op.equals(">=");
    }

    private boolean isNumber(String s) {
        if (s == null)
            return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String compute(String op, double v1, double v2) {
        switch (op) {
            case "+":
                return clean(v1 + v2);
            case "-":
                return clean(v1 - v2);
            case "*":
                return clean(v1 * v2);
            case "/":
                return clean(v1 / v2);
            case "%":
                return clean(v1 % v2);
            case "==":
                return String.valueOf(v1 == v2);
            case "!=":
                return String.valueOf(v1 != v2);
            case "<":
                return String.valueOf(v1 < v2);
            case ">":
                return String.valueOf(v1 > v2);
            case "<=":
                return String.valueOf(v1 <= v2);
            case ">=":
                return String.valueOf(v1 >= v2);
            default:
                return "0";
        }
    }

    private String clean(double d) {
        if (d == (long) d)
            return String.format("%d", (long) d);
        return String.valueOf(d);
    }
}
