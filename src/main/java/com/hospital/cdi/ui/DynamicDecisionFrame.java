package com.hospital.cdi.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Creado por Alejandro Venegas Robles. En caso de incidencias, contactar con alejandro2196vr@gmail.com
public class DynamicDecisionFrame extends JFrame {

    private JPanel formPanel;
    private JLabel previewLabel;
    private String baseTextoNormativo;
    private String codigoCie10;
    private List<String> originalGroups;
    private List<JComponent> inputControls;

    /**
     * Initializes the dynamic decision tree generator for the Desktop application.
     * 
     * @param textoNormativo The parametric base text containing bracketed options.
     * @param requiereArbol  Flag indicating if the text needs dynamic parsing.
     * @param codigoCie10    The diagnostic code to be appended.
     */
    public DynamicDecisionFrame(String textoNormativo, boolean requiereArbol, String codigoCie10) {
        super("CDSS - Generador de Formularios Dinámico (Swing)");
        this.baseTextoNormativo = textoNormativo;
        this.codigoCie10 = codigoCie10;
        this.originalGroups = new ArrayList<>();
        this.inputControls = new ArrayList<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        try {
            initComponents(requiereArbol);
        } catch (Exception e) {
            // Manejo de excepciones exhaustivo solicitado
            String errorMsg = "Error Crítico durante la inicialización: Clase " + e.getStackTrace()[0].getClassName() +
                    ", Línea " + e.getStackTrace()[0].getLineNumber() + " - " + e.getMessage();
            JOptionPane.showMessageDialog(this, errorMsg, "Fallo Crítico", JOptionPane.ERROR_MESSAGE);
            System.err.println(errorMsg);
        }
    }

    private void initComponents(boolean requiereArbol) {
        formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        previewLabel = new JLabel();
        previewLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        previewLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JButton copyButton = new JButton("Copiar para Informe");
        copyButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        copyButton.addActionListener(e -> {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(previewLabel.getText()), null);
        });

        if (!requiereArbol) {
            // Si es FALSE: Muestra el TEXTO_NORMATIVO directamente
            previewLabel.setText(baseTextoNormativo);
        } else {
            // Si es TRUE: Aplica Expresión Regular para buscar corchetes
            JLabel headerLabel = new JLabel("Complete los siguientes campos paramétricos:");
            headerLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            headerLabel.setForeground(Color.DARK_GRAY);
            formPanel.add(headerLabel);
            formPanel.add(Box.createVerticalStrut(15));

            Pattern pattern = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(baseTextoNormativo);

            while (matcher.find()) {
                String fullMatch = matcher.group(0); // e.g. [Opcion 1 / Opcion 2]
                String content = matcher.group(1); // e.g. Opcion 1 / Opcion 2

                originalGroups.add(fullMatch);

                JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                fieldPanel.add(new JLabel("Parámetro [" + content + "]: "));

                if (content.contains("/")) {
                    // Si contiene el carácter '/': Genera componente de selección
                    String[] options = content.split("/");
                    for (int i = 0; i < options.length; i++) {
                        options[i] = options[i].trim();
                    }
                    JComboBox<String> comboBox = new JComboBox<>(options);
                    comboBox.addActionListener(e -> updatePreview());
                    fieldPanel.add(comboBox);
                    inputControls.add(comboBox);
                } else {
                    // Si NO contiene '/': Genera campo de texto
                    JTextField textField = new JTextField(20);
                    textField.setToolTipText(content);
                    // Listener en tiempo real para replicar evento 'input' de JS
                    textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                        public void changedUpdate(javax.swing.event.DocumentEvent e) {
                            updatePreview();
                        }

                        public void removeUpdate(javax.swing.event.DocumentEvent e) {
                            updatePreview();
                        }

                        public void insertUpdate(javax.swing.event.DocumentEvent e) {
                            updatePreview();
                        }
                    });
                    fieldPanel.add(textField);
                    inputControls.add(textField);
                }

                formPanel.add(fieldPanel);
            }
            updatePreview();
        }

        add(new JScrollPane(formPanel), BorderLayout.CENTER);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setBorder(BorderFactory.createTitledBorder("Vista Previa del Diagnóstico Final"));
        bottomWrapper.add(previewLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(copyButton);
        bottomWrapper.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomWrapper, BorderLayout.SOUTH);
    }

    /**
     * Reconstruye el string final sustituyendo cada corchete con su valor.
     */
    private void updatePreview() {
        try {
            String updatedText = baseTextoNormativo;
            for (int i = 0; i < originalGroups.size(); i++) {
                String original = originalGroups.get(i);
                JComponent comp = inputControls.get(i);

                String value = "";
                if (comp instanceof JComboBox) {
                    value = (String) ((JComboBox<?>) comp).getSelectedItem();
                } else if (comp instanceof JTextField) {
                    value = ((JTextField) comp).getText();
                    if (value.trim().isEmpty()) {
                        value = original; // Retener el placeholder si está vacío
                    }
                }

                updatedText = updatedText.replace(original, value);
            }

            if (codigoCie10 != null && !codigoCie10.trim().isEmpty()) {
                updatedText += " (CIE-10: " + codigoCie10 + ")";
            }

            previewLabel.setText(updatedText);

        } catch (Exception e) {
            // Exigencia del usuario: manejo de excepciones exhaustivo con StackTrace y
            // JOptionPane
            String errorMsg = "Fallo capturado en updatePreview: Clase " + e.getStackTrace()[0].getClassName() +
                    ", Línea " + e.getStackTrace()[0].getLineNumber() + " - " + e.getMessage();
            JOptionPane.showMessageDialog(this, errorMsg, "Error de Preview", JOptionPane.ERROR_MESSAGE);
            System.err.println(errorMsg);
        }
    }

    // Método main para demonstración en desarrollo local
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String testDict = "Insuficiencia cardiaca [Aguda / Crónica / Crónica agudizada] con fracción de eyección [Sistólica / Diastólica / Combinada] secundaria a [Etiología].";
            new DynamicDecisionFrame(testDict, true, "I50.-").setVisible(true);
        });
    }
}
