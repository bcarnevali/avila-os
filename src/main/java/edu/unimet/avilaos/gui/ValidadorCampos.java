package edu.unimet.avilaos.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;

/**
 * Validaciones de tipo y de rango para los campos numéricos de la interfaz.
 * Los campos solo aceptan dígitos (no se pueden escribir letras), se marcan en rojo
 * mientras el valor está fuera de rango, y al leerlos se lanza una excepción con un
 * mensaje claro que la vista muestra sin detener la simulación.
 */
public final class ValidadorCampos {

    /** Más de 9 dígitos ya no cabe en un int. */
    private static final int MAX_DIGITOS = 9;
    private static final Color ROJO = new Color(200, 40, 40);

    private ValidadorCampos() {
    }

    // ===================== JTextField =====================

    /** El campo solo acepta dígitos y se marca en rojo si el valor sale del rango. */
    public static void soloEnteros(JTextField campo, int minimo, int maximo) {
        instalarFiltro(campo);
        campo.setToolTipText("Número entero entre " + minimo + " y " + maximo);
        marcarFueraDeRango(campo, minimo, maximo);
    }

    /** Lee un entero de un campo de texto. Lanza IllegalArgumentException con un mensaje para el usuario. */
    public static int leer(JTextField campo, String nombre, int minimo, int maximo) {
        return convertir(campo.getText(), nombre, minimo, maximo);
    }

    // ===================== JSpinner =====================

    /**
     * El spinner solo acepta dígitos. Si el usuario escribe un valor fuera de rango,
     * el texto se conserva en rojo para que la lectura avise del error en lugar de
     * descartarlo en silencio.
     */
    public static void soloEnteros(JSpinner spinner) {
        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        int minimo = ((Number) modelo.getMinimum()).intValue();
        int maximo = ((Number) modelo.getMaximum()).intValue();
        JFormattedTextField campo = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        // El formateador del spinner instala su propio filtro, así que el filtro de dígitos
        // tiene que venir del formateador (sin separador de miles, solo enteros en rango)
        NumberFormatter formato = new NumberFormatter(new DecimalFormat("#")) {
            @Override
            protected DocumentFilter getDocumentFilter() {
                return new FiltroDigitos();
            }
        };
        formato.setValueClass(Integer.class);
        formato.setMinimum(minimo);
        formato.setMaximum(maximo);
        campo.setFormatterFactory(new DefaultFormatterFactory(formato));
        campo.setFocusLostBehavior(JFormattedTextField.PERSIST);
        spinner.setToolTipText("Número entero entre " + minimo + " y " + maximo);
        campo.setToolTipText(spinner.getToolTipText());
        marcarFueraDeRango(campo, minimo, maximo);
    }

    /** Lee el valor escrito en el spinner y lo valida. Lanza IllegalArgumentException si no es válido. */
    public static int leer(JSpinner spinner, String nombre) {
        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        int minimo = ((Number) modelo.getMinimum()).intValue();
        int maximo = ((Number) modelo.getMaximum()).intValue();
        JFormattedTextField campo = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        int valor = convertir(campo.getText(), nombre, minimo, maximo);
        spinner.setValue(valor);
        return valor;
    }

    // ===================== Celdas de JTable =====================

    /**
     * Editor de celdas para columnas enteras: solo acepta dígitos y no deja salir
     * de la celda con un valor fuera de rango, explicando por qué.
     */
    public static DefaultCellEditor editorEntero(String nombre, int minimo, int maximo) {
        JTextField campo = new JTextField();
        instalarFiltro(campo);
        return new DefaultCellEditor(campo) {
            @Override
            public Object getCellEditorValue() {
                String texto = campo.getText().trim();
                return texto.isEmpty() ? null : Integer.valueOf(texto);
            }

            @Override
            public boolean stopCellEditing() {
                try {
                    convertir(campo.getText(), nombre, minimo, maximo);
                } catch (IllegalArgumentException ex) {
                    campo.setBorder(BorderFactory.createLineBorder(ROJO));
                    JOptionPane.showMessageDialog(campo, ex.getMessage(),
                            "Dato inválido", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                campo.setBorder(null);
                return super.stopCellEditing();
            }

            @Override
            public Component getTableCellEditorComponent(javax.swing.JTable tabla, Object valor,
                    boolean seleccionada, int fila, int columna) {
                campo.setBorder(null);
                return super.getTableCellEditorComponent(tabla, valor, seleccionada, fila, columna);
            }
        };
    }

    // ===================== Auxiliares =====================

    private static int convertir(String texto, String nombre, int minimo, int maximo) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("El campo \"" + nombre + "\" es obligatorio.");
        }
        int valor;
        try {
            valor = Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El campo \"" + nombre + "\" debe ser un número entero.");
        }
        if (valor < minimo || valor > maximo) {
            throw new IllegalArgumentException("El campo \"" + nombre + "\" debe estar entre "
                    + minimo + " y " + maximo + ".");
        }
        return valor;
    }

    private static void instalarFiltro(JTextComponent campo) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroDigitos());
    }

    /** Pone el borde rojo mientras el texto no sea un entero dentro del rango. */
    private static void marcarFueraDeRango(JTextComponent campo, int minimo, int maximo) {
        Border normal = campo.getBorder();
        Border rojo = BorderFactory.createLineBorder(ROJO, 2);
        campo.getDocument().addDocumentListener(new DocumentListener() {
            private void revisar() {
                String t = campo.getText().trim();
                boolean valido;
                try {
                    int v = Integer.parseInt(t);
                    valido = v >= minimo && v <= maximo;
                } catch (NumberFormatException ex) {
                    valido = t.isEmpty(); // vacío no se marca hasta que se intente usar
                }
                campo.setBorder(valido ? normal : rojo);
            }

            @Override public void insertUpdate(DocumentEvent e) { revisar(); }
            @Override public void removeUpdate(DocumentEvent e) { revisar(); }
            @Override public void changedUpdate(DocumentEvent e) { revisar(); }
        });
    }

    /** Filtro que rechaza cualquier carácter que no sea dígito (también al pegar texto). */
    private static final class FiltroDigitos extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, texto, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int largo, String texto, AttributeSet attr)
                throws BadLocationException {
            if (texto == null || texto.isEmpty()) {
                super.replace(fb, offset, largo, texto, attr);
                return;
            }
            for (int i = 0; i < texto.length(); i++) {
                if (!Character.isDigit(texto.charAt(i))) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
            }
            if (fb.getDocument().getLength() - largo + texto.length() > MAX_DIGITOS) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            super.replace(fb, offset, largo, texto, attr);
        }
    }
}
