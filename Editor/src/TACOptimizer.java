import java.util.*;

public class TACOptimizer {

    public List<TACInstruction> optimize(List<TACInstruction> instructions) {
        List<TACInstruction> current = new ArrayList<>(instructions);
        boolean changed;
        int pass = 0;

        System.out.println("   ⚙️ Iniciando Optimización de TAC...");

        do {
            changed = false;
            pass++;

            // 1. Optimización por Bloques Básicos (Local)
            // Divide el código en bloques y aplica optimizaciones locales (CSE)
            if (performBlockOptimization(current)) {
                changed = true;
                // System.out.println(" Block Opt applied");
            }

            // 2. Constant Folding & Propagation (Global)
            if (performConstantFolding(current)) {
                changed = true;
            }

            // 3. Dead Code Elimination (Global)
            if (performDeadCodeElimination(current)) {
                changed = true;
            }

        } while (changed && pass < 10);

        return current;
    }

    // =========================================================================
    // 1. BLOCK OPTIMIZATION (Bloques Básicos)
    // =========================================================================

    private boolean performBlockOptimization(List<TACInstruction> instructions) {
        List<List<TACInstruction>> blocks = makeBasicBlocks(instructions);
        boolean anyChange = false;

        List<TACInstruction> result = new ArrayList<>();

        for (List<TACInstruction> block : blocks) {
            boolean blockChanged = optimizeBasicBlock(block);
            if (blockChanged)
                anyChange = true;
            result.addAll(block);
        }

        if (anyChange) {
            instructions.clear();
            instructions.addAll(result);
        }
        return anyChange;
    }

    private List<List<TACInstruction>> makeBasicBlocks(List<TACInstruction> instructions) {
        List<List<TACInstruction>> blocks = new ArrayList<>();
        if (instructions.isEmpty())
            return blocks;

        List<TACInstruction> currentBlock = new ArrayList<>();
        blocks.add(currentBlock);

        for (int i = 0; i < instructions.size(); i++) {
            TACInstruction inst = instructions.get(i);

            // REGLA 1: Una etiqueta (LABEL) inicia un nuevo bloque
            if (inst.op.equals("LABEL")) {
                if (!currentBlock.isEmpty()) {
                    currentBlock = new ArrayList<>();
                    blocks.add(currentBlock);
                }
            }

            currentBlock.add(inst);

            // REGLA 2: Una instrucción de salto termina el bloque actual
            if (inst.op.equals("GOTO") || inst.op.equals("IF_FALSE") || inst.op.equals("ret")) {
                // El siguiente frame inicia nuevo bloque (si hay más instrucciones)
                if (i < instructions.size() - 1) {
                    currentBlock = new ArrayList<>();
                    blocks.add(currentBlock);
                }
            }
        }
        // Limpiar bloques vacíos
        blocks.removeIf(List::isEmpty);
        return blocks;
    }

    // Optimización Local: Common Subexpression Elimination (CSE)
    private boolean optimizeBasicBlock(List<TACInstruction> block) {
        boolean changed = false;
        // Mapa de expresiones disponibles: "op arg1 arg2" -> "temp_variable_result"
        Map<String, String> availableExprs = new HashMap<>();

        for (int i = 0; i < block.size(); i++) {
            TACInstruction inst = block.get(i);

            // Solo nos interesan expresiones aritméticas/relacionales: t0 = a + b
            if (isArithmeticOrRelational(inst.op) && inst.arg2 != null && inst.result != null) {
                // Crear firma única de la expresión
                String expr = inst.op + " " + inst.arg1 + " " + inst.arg2;

                // Conmutatividad para + y * (a+b es igual a b+a)
                if (inst.op.equals("+") || inst.op.equals("*")) {
                    if (inst.arg1.compareTo(inst.arg2) > 0) {
                        expr = inst.op + " " + inst.arg2 + " " + inst.arg1;
                    }
                }

                if (availableExprs.containsKey(expr)) {
                    // ✅ CSE: ¡Encontramos subexpresión común!
                    // Si ya calculamos "a+b" antes y guardamos en "t1",
                    // y ahora vemos "t2 = a+b", reemplazamos por "t2 = t1"
                    String existingResult = availableExprs.get(expr);

                    // Solo si la variable original sigue viva (no reasignada)
                    // (La lógica de invalidación abajo se encarga de esto)

                    inst.op = "=";
                    inst.arg1 = existingResult;
                    inst.arg2 = null;
                    changed = true;
                    // System.out.println(" ♻️ CSE: Reemplazado cálculo repetido en bloque");
                } else {
                    // Nueva expresión disponible
                    availableExprs.put(expr, inst.result);
                }
            }

            // INVALIDACIÓN
            // Si una variable es reasignada, cualquier expresión previa que la usaba ya no
            // es válida.
            // Ej: t1 = a + b; a = 5; t2 = a + b; <- No podemos reusar t1 para t2
            if (inst.result != null) {
                String modifiedVar = inst.result;
                availableExprs.entrySet().removeIf(entry -> {
                    String key = entry.getKey(); // "op arg1 arg2"
                    String[] parts = key.split(" ");
                    if (parts.length >= 3) {
                        return parts[1].equals(modifiedVar) || parts[2].equals(modifiedVar);
                    }
                    return false;
                });
            }
        }
        return changed;
    }

    // =========================================================================
    // 2. CONSTANT FOLDING (Global)
    // =========================================================================

    private boolean performConstantFolding(List<TACInstruction> instructions) {
        boolean changed = false;
        Map<String, String> constants = new HashMap<>();

        for (int i = 0; i < instructions.size(); i++) {
            TACInstruction inst = instructions.get(i);

            // Propagación: Reemplazar usos de variables que sabemos son constantes
            String arg1 = constants.getOrDefault(inst.arg1, inst.arg1);
            String arg2 = constants.getOrDefault(inst.arg2, inst.arg2);

            if (!Objects.equals(arg1, inst.arg1) || !Objects.equals(arg2, inst.arg2)) {
                inst.arg1 = arg1;
                inst.arg2 = arg2;
                changed = true;
            }

            // Folding: Evaluar operaciones estáticas
            if (inst.result != null && isArithmeticOrRelational(inst.op)) {
                if (isNumber(inst.arg1) && isNumber(inst.arg2)) {
                    try {
                        double v1 = Double.parseDouble(inst.arg1);
                        double v2 = Double.parseDouble(inst.arg2);
                        String res = compute(inst.op, v1, v2);

                        inst.op = "=";
                        inst.arg1 = res;
                        inst.arg2 = null;
                        changed = true;
                    } catch (Exception e) {
                    }
                }
            }

            // Registrar nuevas constantes
            if (inst.op.equals("=") && isNumber(inst.arg1) && inst.result != null) {
                constants.put(inst.result, inst.arg1);
            }
            // Si la variable cambia a algo no constante, quitar del mapa
            else if (inst.result != null) {
                constants.remove(inst.result);
            }
        }
        return changed;
    }

    // =========================================================================
    // 3. DEAD CODE ELIMINATION (Global)
    // =========================================================================

    private boolean performDeadCodeElimination(List<TACInstruction> instructions) {
        Map<String, Integer> usages = new HashMap<>();

        // Contar usos
        for (TACInstruction inst : instructions) {
            if (inst.arg1 != null && inst.arg1.startsWith("t")) {
                usages.put(inst.arg1, usages.getOrDefault(inst.arg1, 0) + 1);
            }
            if (inst.arg2 != null && inst.arg2.startsWith("t")) {
                usages.put(inst.arg2, usages.getOrDefault(inst.arg2, 0) + 1);
            }
        }

        boolean changed = false;
        Iterator<TACInstruction> it = instructions.iterator();
        while (it.hasNext()) {
            TACInstruction inst = it.next();
            // Eliminar asignaciones a temporales ('t...') que nunca se usan
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

    // =========================================================================
    // Helpers
    // =========================================================================

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
