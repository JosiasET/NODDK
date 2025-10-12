import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;

public class Sintaxis_Noddk_GUI {

    // Expresiones regulares
    private static final Pattern IDENTIFICADOR_VALIDO = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern NUMERO = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");
    private static final Pattern CADENA = Pattern.compile("^\"(.*?)\"$");
    private static final Pattern CARACTER = Pattern.compile("^'.'$");
    private static final Pattern BOOLEANO = Pattern.compile("^(true|false)$");
    private static final Pattern DECLARACION = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+);?$");
    private static final Pattern PRINT = Pattern.compile("^print\\((f?)(\"(.*?)\")\\);?$");
    private static final Pattern VARIABLE_INTERPOLACION = Pattern.compile("\\{(\\w+)\\}");
    private static final Pattern IF_STATEMENT = Pattern.compile("^if\\s*\\((.+)\\)\\s*\\{");
    private static final Pattern ELSE_STATEMENT = Pattern.compile("^\\s*\\}\\s*else\\s*\\{");
    private static final Pattern CLOSE_BRACE = Pattern.compile("^\\s*\\}\\s*$");

    // Tabla de símbolos para almacenar variables
    private static final Map<String, Object> variables = new HashMap<>();

    // Variables para manejar bloques if-else
    private static boolean enBloqueIf = false;
    private static boolean condicionIf = false;
    private static boolean enBloqueElse = false;
    private static int nivelBloque = 0;
    private static List<String> bloqueActual = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> crearGUI());
        modoConsola();
    }

    private static void modoConsola() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Escribe declaraciones de variables según la gramática Noddk.");
        System.out.println("Escribe 'salir' para finalizar.\n");

        while (true) {
            System.out.print(">> ");
            String linea = scanner.nextLine().trim();

            if (linea.equalsIgnoreCase("salir")) {
                System.out.println("👋 Programa finalizado.");
                break;
            }

            procesarLinea(linea, true);
        }

        scanner.close();
    }

    private static void crearGUI() {
        JFrame frame = new JFrame("Noddk Interpreter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputArea);

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        JButton runButton = new JButton("Ejecutar");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] lineas = inputArea.getText().split("\\n");
                outputArea.setText("");
                reiniciarEstadoBloques();

                for (String linea : lineas) {
                    if (!linea.trim().isEmpty()) {
                        String resultado = procesarLinea(linea.trim(), false);
                        if (resultado != null && !resultado.isEmpty()) {
                            outputArea.append(resultado + "\n");
                        }
                    }
                }
                // Ejecutar cualquier bloque pendiente al final
                ejecutarBloqueSiNecesario();
            }
        });

        panel.add(inputScroll, BorderLayout.CENTER);
        panel.add(outputScroll, BorderLayout.SOUTH);
        panel.add(runButton, BorderLayout.EAST);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void reiniciarEstadoBloques() {
        enBloqueIf = false;
        condicionIf = false;
        enBloqueElse = false;
        nivelBloque = 0;
        bloqueActual.clear();
    }

    private static String procesarLinea(String linea, boolean modoConsola) {
        // Primero verificar si estamos dentro de un bloque if/else
        if (manejarBloques(linea, modoConsola)) {
            return "";
        }

        Matcher matcherPrint = PRINT.matcher(linea);
        Matcher matcherIf = IF_STATEMENT.matcher(linea);

        if (matcherIf.matches()) {
            String condicion = matcherIf.group(1);
            condicionIf = evaluarCondicion(condicion);
            enBloqueIf = true;
            nivelBloque++;
            return "";
        } else if (matcherPrint.matches()) {
            if (enBloqueIf || enBloqueElse) {
                bloqueActual.add(linea);
                return "";
            } else {
                String fPrefix = matcherPrint.group(1);
                String contenidoComillas = matcherPrint.group(3);
                return "📤 " + procesarPrint(contenidoComillas, !fPrefix.isEmpty());
            }
        } else if (validarDeclaracion(linea)) {
            if (enBloqueIf || enBloqueElse) {
                bloqueActual.add(linea);
                return "";
            } else {
                return "✅ " + linea + " -> Declaración válida.";
            }
        } else {
            return "❌ " + linea + " -> Declaración inválida.";
        }
    }

    private static boolean manejarBloques(String linea, boolean modoConsola) {
        Matcher matcherElse = ELSE_STATEMENT.matcher(linea);
        Matcher matcherCloseBrace = CLOSE_BRACE.matcher(linea);

        if (enBloqueIf && matcherElse.matches()) {
            // Transición del bloque if al bloque else
            if (nivelBloque == 1) {
                ejecutarBloqueSiNecesario();
                enBloqueIf = false;
                enBloqueElse = true;
            }
            return true;
        } else if ((enBloqueIf || enBloqueElse) && matcherCloseBrace.matches()) {
            nivelBloque--;
            if (nivelBloque == 0) {
                ejecutarBloqueSiNecesario();
                enBloqueIf = false;
                enBloqueElse = false;
            }
            return true;
        }
        return false;
    }

    private static void ejecutarBloqueSiNecesario() {
        if (bloqueActual.isEmpty())
            return;

        boolean ejecutarBloque = (enBloqueIf && condicionIf) || (enBloqueElse && !condicionIf);

        if (ejecutarBloque) {
            for (String lineaBloque : bloqueActual) {
                procesarLinea(lineaBloque, false);
            }
        }

        bloqueActual.clear();
    }

    private static boolean evaluarCondicion(String condicion) {
        try {
            // Reemplazar variables en la condición
            String condicionEvaluada = condicion;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                condicionEvaluada = condicionEvaluada.replaceAll("\\b" + entry.getKey() + "\\b",
                        entry.getValue().toString());
            }

            // Evaluar la condición
            return (boolean) new javax.script.ScriptEngineManager()
                    .getEngineByName("js")
                    .eval(condicionEvaluada);
        } catch (Exception e) {
            return false;
        }
    }

    private static String procesarPrint(String contenido, boolean interpolacion) {
        if (!interpolacion) {
            return contenido;
        }

        Matcher matcherVariables = VARIABLE_INTERPOLACION.matcher(contenido);
        StringBuffer resultado = new StringBuffer();

        while (matcherVariables.find()) {
            String nombreVariable = matcherVariables.group(1);
            Object valor = variables.get(nombreVariable);
            matcherVariables.appendReplacement(resultado, valor != null ? valor.toString() : "null");
        }
        matcherVariables.appendTail(resultado);

        return resultado.toString();
    }

    private static boolean validarDeclaracion(String linea) {
        Matcher matcher = DECLARACION.matcher(linea);

        if (matcher.matches()) {
            String identificador = matcher.group(1);
            String valorExpresion = matcher.group(2).trim();

            if (valorExpresion.endsWith(";")) {
                valorExpresion = valorExpresion.substring(0, valorExpresion.length() - 1).trim();
            }

            if (!IDENTIFICADOR_VALIDO.matcher(identificador).matches()) {
                return false;
            }

            Object valor = evaluarExpresion(valorExpresion);
            if (valor != null) {
                variables.put(identificador, valor);
                return true;
            }
        }

        return false;
    }

    private static Object evaluarExpresion(String expresion) {
        if (NUMERO.matcher(expresion).matches()) {
            if (expresion.contains(".")) {
                return Double.parseDouble(expresion);
            } else {
                return Integer.parseInt(expresion);
            }
        } else if (CADENA.matcher(expresion).matches()) {
            return expresion.substring(1, expresion.length() - 1);
        } else if (CARACTER.matcher(expresion).matches()) {
            return expresion.charAt(1);
        } else if (BOOLEANO.matcher(expresion).matches()) {
            return Boolean.parseBoolean(expresion);
        }

        try {
            String expresionEvaluada = expresion;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                expresionEvaluada = expresionEvaluada.replaceAll("\\b" + entry.getKey() + "\\b",
                        entry.getValue().toString());
            }

            return evaluarAritmetica(expresionEvaluada);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object evaluarAritmetica(String expresion) {
        try {
            return new javax.script.ScriptEngineManager().getEngineByName("js").eval(expresion);
        } catch (Exception e) {
            return null;
        }
    }
}