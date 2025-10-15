import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;

import java.util.List;

public class CodeEditor {
    private JTabbedPane tabbedPane;
    private JTextPane eastTextPane;
    private JTextPane consoleTextPane;
    private JPanel westPanel;
    private JPanel eastPanel;
    private JPanel southPanel;
    private JButton ejecutarBtn;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private File rootDir = new File(System.getProperty("user.home") + File.separator + "code-workspace");
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private Map<File, JTextPane> openFiles = new HashMap<>();
    
    private final Color COLOR_FONDO = new Color(135, 206, 250);
    private final Color COLOR_ACENTO = Color.GRAY;
    private final Color COLOR_TEXTO = Color.BLACK;
    private final Color COLOR_EDITOR = Color.WHITE;
    private final Color COLOR_LINE_NUMBERS = new Color(240, 240, 240);
    
    // Configuración de fuentes
    private final Font FUENTE_CODIGO = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private final Font FUENTE_BOTONES = new Font("SansSerif", Font.BOLD, 14);
    private final Font FUENTE_ARBOL = new Font("SansSerif", Font.PLAIN, 13);
    private final Font FUENTE_CONSOLA = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    public CodeEditor() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Professional Code Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout());

        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        crearPanelSuperior(frame);
        southPanel = crearPanelInferior();
        westPanel = crearPanelOeste();
        eastPanel = crearPanelEste();
        tabbedPane = crearEditorCentral();

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_FONDO);
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(westPanel, BorderLayout.WEST);
        frame.add(eastPanel, BorderLayout.EAST);
        frame.add(southPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void crearPanelSuperior(JFrame frame) {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(COLOR_FONDO);
        northPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_ACENTO));

        JPanel topControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topControlPanel.setBackground(COLOR_FONDO);

        JButton toggleWestBtn = crearBotonSuperior("◀ Panel", "Mostrar/Ocultar panel lateral");
        ejecutarBtn = crearBotonSuperior("⚡ Ejecutar", "Ejecutar código o mostrar consola");
        JButton abrirCarpetaBtn = crearBotonSuperior("📂 Abrir Carpeta", "Abrir carpeta de trabajo");
        JButton guardarBtn = crearBotonSuperior("💾 Guardar", "Guardar archivo actual");

        topControlPanel.add(toggleWestBtn);
        topControlPanel.add(abrirCarpetaBtn);
        topControlPanel.add(guardarBtn);
        topControlPanel.add(ejecutarBtn);

        northPanel.add(topControlPanel, BorderLayout.WEST);
        frame.add(northPanel, BorderLayout.NORTH);

        toggleWestBtn.addActionListener(e -> {
            westPanel.setVisible(!westPanel.isVisible());
            frame.revalidate();
            toggleWestBtn.setText(westPanel.isVisible() ? "◀ Panel" : "▶ Panel");
        });

        ejecutarBtn.addActionListener(e -> {
            Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent instanceof JPanel) {
                JPanel panel = (JPanel) selectedComponent;
                Component view = ((JScrollPane) panel.getComponent(0)).getViewport().getView();
                if (view instanceof JTextPane) {
                    JTextPane currentPane = (JTextPane) view;
                    String codigo = currentPane.getText();
                    
                    try {
                        // ==================== ANÁLISIS LÉXICO ====================
                        Lexer lexer = new Lexer(codigo);
                        List<Token> tokens = lexer.tokenize();
                        
                        // ==================== ANÁLISIS SEMÁNTICO ====================
                        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
                        boolean semanticValid = performSemanticAnalysis(tokens, semanticAnalyzer);
                        
                        // ==================== ANÁLISIS SINTÁCTICO ====================
                        // CORREGIDO: Pasar el semanticAnalyzer al Parser para que no haga conversiones implícitas
                        Parser parser = new Parser(tokens, semanticAnalyzer);
                        String resultado = parser.parse();
                        
                        if (consoleTextPane == null) {
                            consoleTextPane = new JTextPane();
                            consoleTextPane.setEditable(false);
                            consoleTextPane.setFont(FUENTE_CONSOLA);
                            consoleTextPane.setBackground(Color.BLACK);
                            consoleTextPane.setForeground(Color.WHITE);
                        }
                        
                        // CORREGIDO: Mostrar resultados SEMÁNTICOS con errores reales
                        StringBuilder output = new StringBuilder();
                        
                        output.append("🔍 ANÁLISIS SEMÁNTICO:\n");
                        output.append("=".repeat(50)).append("\n");
                        
                        if (semanticValid) {
                            output.append("✅ ANÁLISIS SEMÁNTICO EXITOSO\n\n");
                            output.append(semanticAnalyzer.getSymbolTableAsString());
                        } else {
                            // CORREGIDO: Usar el método real para obtener errores
                            output.append(semanticAnalyzer.getErrorsAsString());
                            output.append("\n");
                            output.append(semanticAnalyzer.getSymbolTableAsString());
                        }
                        
                        output.append("\n📋 RESULTADO DEL PARSER:\n");
                        output.append("=".repeat(50)).append("\n");
                        output.append(resultado);
                        
                        consoleTextPane.setText(output.toString());
                        toggleConsola();
                        
                    } catch (Exception ex) {
                        mostrarErrorEnConsola(ex);
                    }
                }
            }
        });

        abrirCarpetaBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                rootDir = chooser.getSelectedFile();
                actualizarArbol();
            }
        });

        guardarBtn.addActionListener(e -> guardarArchivoActual());
    }

    /**
     * CORREGIDO: Análisis semántico mejorado que detecta operaciones mixtas
     */
    private boolean performSemanticAnalysis(List<Token> tokens, SemanticAnalyzer semanticAnalyzer) {
        try {
            // Primera pasada: declaraciones de variables
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                
                // Detectar declaraciones de variables (patrón: identificador = expresión)
                if (token.type == TokenType.IDENTIFIER && i + 2 < tokens.size()) {
                    Token next = tokens.get(i + 1);
                    Token nextNext = tokens.get(i + 2);
                    
                    if (next.type == TokenType.ASSIGN) {
                        String identifier = token.value;
                        Object value = extractValueFromToken(nextNext);
                        
                        if (value != null) {
                            semanticAnalyzer.checkDeclaration(identifier, value, token.line);
                        }
                    }
                }
            }
            
            // Segunda pasada: uso de variables y operaciones
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                
                // Detectar uso de variables
                if (token.type == TokenType.IDENTIFIER) {
                    // Verificar acceso al scope y si está inicializada
                    semanticAnalyzer.checkScopeAccess(token.value, token.line);
                    semanticAnalyzer.checkVariableInitialized(token.value, token.line);
                }
                
                // CORREGIDO: Detectar operaciones mixtas int + float
                if (i > 0 && i < tokens.size() - 1 && isOperator(token.type)) {
                    Token prev = tokens.get(i - 1);
                    Token next = tokens.get(i + 1);
                    
                    // Obtener tipos de los operandos
                    String leftType = getOperandType(prev, semanticAnalyzer);
                    String rightType = getOperandType(next, semanticAnalyzer);
                    String operator = getOperatorSymbol(token.type);
                    
                    // Si ambos operandos tienen tipos válidos y son diferentes
                    if (leftType != null && rightType != null && !leftType.equals(rightType)) {
                        // CORREGIDO: Usar el método correcto para reportar errores
                        semanticAnalyzer.addError("Línea " + token.line + ": No se puede operar " + 
                                leftType + " con " + rightType + " usando '" + operator + 
                                "'. Los tipos deben ser exactamente iguales.");
                    }
                    
                    // Verificación adicional usando el método del semanticAnalyzer
                    Object leftValue = extractValueFromToken(prev);
                    Object rightValue = extractValueFromToken(next);
                    
                    if (leftValue != null && rightValue != null) {
                        // CORREGIDO: Usar el nuevo método que solo verifica tipos sin ejecutar
                        semanticAnalyzer.checkBinaryOperationTypes(leftValue, rightValue, operator, token.line);
                    }
                }
            }
            
            return !semanticAnalyzer.hasErrors();
            
        } catch (Exception e) {
            System.err.println("Error en análisis semántico: " + e.getMessage());
            semanticAnalyzer.addError("Error durante el análisis semántico: " + e.getMessage());
            return false;
        }
    }

    /**
     * NUEVO: Obtener el tipo de un operando
     */
    private String getOperandType(Token token, SemanticAnalyzer semanticAnalyzer) {
        if (token.type == TokenType.NUMBER) {
            if (token.value.contains(".")) {
                return "float";
            } else {
                return "int";
            }
        } else if (token.type == TokenType.STRING) {
            return "String";
        } else if (token.type == TokenType.TRUE || token.type == TokenType.FALSE) {
            return "boolean";
        } else if (token.type == TokenType.IDENTIFIER) {
            // Buscar en la tabla de símbolos
            if (semanticAnalyzer.variableExists(token.value)) {
                SemanticAnalyzer.VariableInfo info = semanticAnalyzer.getVariableInfo(token.value);
                return info.type;
            }
        }
        return null;
    }

    private boolean isOperator(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINUS || 
            type == TokenType.MULTIPLY || type == TokenType.DIVIDE ||
            type == TokenType.MODULO || type == TokenType.EQUALS ||
            type == TokenType.NOT_EQUALS || type == TokenType.LESS ||
            type == TokenType.GREATER || type == TokenType.LESS_EQUAL ||
            type == TokenType.GREATER_EQUAL || type == TokenType.AND ||
            type == TokenType.OR;
    }

    private String getOperatorSymbol(TokenType type) {
        switch (type) {
            case PLUS: return "+";
            case MINUS: return "-";
            case MULTIPLY: return "*";
            case DIVIDE: return "/";
            case MODULO: return "%";
            case EQUALS: return "==";
            case NOT_EQUALS: return "!=";
            case LESS: return "<";
            case GREATER: return ">";
            case LESS_EQUAL: return "<=";
            case GREATER_EQUAL: return ">=";
            case AND: return "&&";
            case OR: return "||";
            default: return "";
        }
    }

    /**
     * Extrae valores de los tokens literales
     */
    private Object extractValueFromToken(Token token) {
        if (token == null) return null;
        
        try {
            switch (token.type) {
                case NUMBER:
                    if (token.value.contains(".")) {
                        return Float.parseFloat(token.value);
                    } else {
                        return Integer.parseInt(token.value);
                    }
                case STRING:
                    return token.value.substring(1, token.value.length() - 1);
                case TRUE:
                    return true;
                case FALSE:
                    return false;
                case IDENTIFIER:
                    // Para simulación, retornar valores por defecto basados en el nombre
                    if (token.value.matches(".*[a-zA-Z].*")) {
                        return 0; // Valor numérico por defecto
                    }
                    return 0;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parseando valor: " + token.value);
            return null;
        }
    }

    /**
     * Muestra errores en la consola
     */
    private void mostrarErrorEnConsola(Exception ex) {
        if (consoleTextPane == null) {
            consoleTextPane = new JTextPane();
            consoleTextPane.setEditable(false);
            consoleTextPane.setFont(FUENTE_CONSOLA);
            consoleTextPane.setBackground(Color.BLACK);
            consoleTextPane.setForeground(Color.WHITE);
        }
        
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("❌ ERROR DURANTE EL ANÁLISIS:\n");
        errorMsg.append("=".repeat(50)).append("\n");
        errorMsg.append(ex.getMessage()).append("\n\n");
        errorMsg.append("Stack trace:\n");
        for (StackTraceElement element : ex.getStackTrace()) {
            if (element.getClassName().contains("CodeEditor") || 
                element.getClassName().contains("Lexer") ||
                element.getClassName().contains("Parser") ||
                element.getClassName().contains("SemanticAnalyzer")) {
                errorMsg.append("  at ").append(element).append("\n");
            }
        }
        
        consoleTextPane.setText(errorMsg.toString());
        toggleConsola();
    }

    // ... (el resto de los métodos de CodeEditor se mantienen igual)

    private JButton crearBotonSuperior(String texto, String tooltip) {
        JButton btn = new JButton(texto);
        btn.setFont(FUENTE_BOTONES);
        btn.setBackground(Color.WHITE);
        btn.setForeground(COLOR_TEXTO);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACENTO, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_ACENTO);
                btn.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(COLOR_TEXTO);
            }
        });

        return btn;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setPreferredSize(new Dimension(0, 150));
        panel.setVisible(false);

        consoleTextPane = new JTextPane();
        consoleTextPane.setEditable(false);
        consoleTextPane.setBackground(Color.BLACK);
        consoleTextPane.setForeground(Color.WHITE);
        consoleTextPane.setFont(FUENTE_CONSOLA);

        JScrollPane consoleScroll = new JScrollPane(consoleTextPane);
        consoleScroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(consoleScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelOeste() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setPreferredSize(new Dimension(250, 0));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setBackground(COLOR_FONDO);
        fileTree.setForeground(COLOR_TEXTO);
        fileTree.setFont(FUENTE_ARBOL);
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        fileTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        File file = (File) node.getUserObject();
                        if (file.isFile()) {
                            abrirArchivoEnPestana(file);
                        }
                    }
                }
            }
        });

        loadFileTree(rootDir, root);
        JScrollPane treeScroll = new JScrollPane(fileTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton newFileBtn = crearBotonLateral("📄 Nuevo Archivo", e -> crearNuevoArchivo());
        JButton newFolderBtn = crearBotonLateral("📁 Nueva Carpeta", e -> crearNuevaCarpeta());

        btnPanel.add(newFileBtn);
        btnPanel.add(newFolderBtn);

        panel.add(treeScroll, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton crearBotonLateral(String texto, ActionListener accion) {
        JButton btn = new JButton(texto);
        btn.setFont(FUENTE_ARBOL);
        btn.setBackground(Color.WHITE);
        btn.setForeground(COLOR_TEXTO);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACENTO, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        btn.setFocusPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_ACENTO);
                btn.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(COLOR_TEXTO);
            }
        });

        btn.addActionListener(accion);
        return btn;
    }

    private void abrirArchivoEnPestana(File file) {
        if (openFiles.containsKey(file)) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component tab = tabbedPane.getComponentAt(i);
                if (tab instanceof JPanel) {
                    JScrollPane scroll = (JScrollPane) ((JPanel) tab).getComponent(0);
                    if (scroll.getViewport().getView() == openFiles.get(file)) {
                        tabbedPane.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }

        JTextPane textPane = new JTextPane();
        textPane.setFont(FUENTE_CODIGO);
        textPane.setBackground(COLOR_EDITOR);
        textPane.setForeground(COLOR_TEXTO);
        textPane.setCaretColor(COLOR_TEXTO);

        JTextPane lineNumbers = new JTextPane();
        lineNumbers.setEditable(false);
        lineNumbers.setFont(FUENTE_CODIGO);
        lineNumbers.setBackground(COLOR_LINE_NUMBERS);
        lineNumbers.setForeground(COLOR_ACENTO);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.getViewport().setBackground(COLOR_EDITOR);
        scrollPane.setRowHeaderView(lineNumbers);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                actualizarUI(textPane, lineNumbers);
            }

            public void removeUpdate(DocumentEvent e) {
                actualizarUI(textPane, lineNumbers);
            }

            public void changedUpdate(DocumentEvent e) {
                actualizarUI(textPane, lineNumbers);
            }
        });

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            textPane.setText(content.toString());
        } catch (Exception ex) {
            if (consoleTextPane != null) {
                consoleTextPane.setText("Error al abrir archivo: " + ex.getMessage());
            }
        }

        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel tabHeader = new JPanel(new BorderLayout());
        tabHeader.setOpaque(false);

        JLabel title = new JLabel(" " + file.getName());
        title.setForeground(COLOR_TEXTO);
        title.setFont(FUENTE_ARBOL);

        JButton closeBtn = new JButton("×");
        closeBtn.setForeground(COLOR_ACENTO);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setContentAreaFilled(false);

        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.RED);
            }

            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(COLOR_ACENTO);
            }
        });

        closeBtn.addActionListener(e -> {
            openFiles.remove(file);
            tabbedPane.remove(tabPanel);
        });

        tabHeader.add(title, BorderLayout.CENTER);
        tabHeader.add(closeBtn, BorderLayout.EAST);

        int tabIndex = tabbedPane.getTabCount();
        tabbedPane.addTab(file.getName(), tabPanel);
        tabbedPane.setTabComponentAt(tabIndex, tabHeader);
        tabbedPane.setSelectedComponent(tabPanel);

        openFiles.put(file, textPane);
        actualizarUI(textPane, lineNumbers);
    }

    private void actualizarUI(JTextPane editor, JTextPane lineNumbers) {
        String content = editor.getText();
        if (eastTextPane != null) {
            eastTextPane.setText(content);
        }

        int lineCount = content.split("\n").length;
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbers.setText(numbers.toString());
    }

    private JTabbedPane crearEditorCentral() {
        JTabbedPane pane = new JTabbedPane();
        pane.setBackground(COLOR_FONDO);
        pane.setForeground(COLOR_TEXTO);
        pane.setBorder(BorderFactory.createEmptyBorder());

        pane.addChangeListener(e -> {
            Component selected = pane.getSelectedComponent();
            if (selected != null && selected instanceof JPanel) {
                Component comp = ((JPanel) selected).getComponent(0);
                if (comp instanceof JScrollPane) {
                    JScrollPane scroll = (JScrollPane) comp;
                    if (scroll.getViewport().getView() instanceof JTextPane) {
                        JTextPane editor = (JTextPane) scroll.getViewport().getView();
                        if (eastTextPane != null) {
                            eastTextPane.setText(editor.getText());
                        }
                    }
                }
            }
        });

        return pane;
    }

    private JPanel crearPanelEste() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setPreferredSize(new Dimension(300, 0));

        eastTextPane = new JTextPane();
        eastTextPane.setEditable(false);
        eastTextPane.setBackground(COLOR_EDITOR);
        eastTextPane.setForeground(COLOR_TEXTO);
        eastTextPane.setFont(FUENTE_CODIGO.deriveFont(11f));

        JScrollPane eastScroll = new JScrollPane(eastTextPane);
        eastScroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(eastScroll, BorderLayout.CENTER);

        return panel;
    }

    private void crearNuevoArchivo() {
        File parentDir = obtenerDirectorioActual();
        if (parentDir == null) {
            return;
        }

        String nombre = JOptionPane.showInputDialog("Nombre del nuevo archivo:");
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        File nuevoArchivo = new File(parentDir, nombre);
        try {
            if (nuevoArchivo.createNewFile()) {
                actualizarArbol();
                abrirArchivoEnPestana(nuevoArchivo);
            }
        } catch (IOException ex) {
            if (consoleTextPane != null) {
                consoleTextPane.setText("Error al crear archivo: " + ex.getMessage());
            }
        }
    }

    private void crearNuevaCarpeta() {
        File parentDir = obtenerDirectorioActual();
        if (parentDir == null) {
            return;
        }

        String nombre = JOptionPane.showInputDialog("Nombre de la nueva carpeta:");
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        File nuevaCarpeta = new File(parentDir, nombre);
        if (nuevaCarpeta.mkdir()) {
            actualizarArbol();
        }
    }

    private void guardarArchivoActual() {
        Component tab = tabbedPane.getSelectedComponent();
        if (tab == null || !(tab instanceof JPanel)) {
            return;
        }

        Component comp = ((JPanel) tab).getComponent(0);
        if (!(comp instanceof JScrollPane)) {
            return;
        }

        JScrollPane scroll = (JScrollPane) comp;
        if (!(scroll.getViewport().getView() instanceof JTextPane)) {
            return;
        }

        JTextPane editor = (JTextPane) scroll.getViewport().getView();

        for (Map.Entry<File, JTextPane> entry : openFiles.entrySet()) {
            if (entry.getValue().equals(editor)) {
                try (FileWriter writer = new FileWriter(entry.getKey())) {
                    writer.write(editor.getText());
                    if (consoleTextPane != null) {
                        consoleTextPane.setText("Archivo guardado exitosamente!");
                    }
                } catch (Exception ex) {
                    if (consoleTextPane != null) {
                        consoleTextPane.setText("Error al guardar: " + ex.getMessage());
                    }
                }
                return;
            }
        }
        if (consoleTextPane != null) {
            consoleTextPane.setText("No se pudo encontrar el archivo abierto para guardar.");
        }
    }

    private void toggleConsola() {
        boolean visible = !southPanel.isVisible();
        southPanel.setVisible(visible);
        southPanel.revalidate();
        
        if (visible) {
            ejecutarBtn.setText("⬇ Ocultar Consola");
            if (consoleTextPane != null && consoleTextPane.getText().trim().isEmpty()) {
                consoleTextPane.setText("Consola lista...\n");
            }
        } else {
            ejecutarBtn.setText("⚡ Ejecutar");
        }
    }

    private File obtenerDirectorioActual() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) {
            return rootDir;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        File file = (File) node.getUserObject();
        return file.isDirectory() ? file : file.getParentFile();
    }

    private void loadFileTree(File dir, DefaultMutableTreeNode node) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(file);
            node.add(child);
            if (file.isDirectory()) {
                loadFileTree(file, child);
            }
        }
    }

    private void actualizarArbol() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);
        loadFileTree(rootDir, root);
        treeModel.setRoot(root);
        treeModel.reload();
        fileTree.expandPath(new TreePath(root.getPath()));
    }

    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            File file = (File) ((DefaultMutableTreeNode) value).getUserObject();
            setIcon(fileSystemView.getSystemIcon(file));
            setText(fileSystemView.getSystemDisplayName(file));
            setBackgroundNonSelectionColor(tree.getBackground());
            setForeground(COLOR_TEXTO);
            setFont(FUENTE_ARBOL);

            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            new CodeEditor();
        });
    }
}