package edu.unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Formulario para crear un buffer: nombre, capacidad y computador anfitrion. Solo valida y pide. */
public class PanelCrearBuffer extends JPanel {

    private final JTextField campoId = new JTextField(10);
    private final JTextField campoCapacidad = new JTextField(10);
    private final JComboBox<String> comboAnfitrion = new JComboBox<>();
    private CreadorBuffers creador;

    public PanelCrearBuffer() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Crear buffer"));

        JPanel formulario = new JPanel(new GridBagLayout());
        agregarFila(formulario, 0, "Nombre", campoId);
        agregarFila(formulario, 1, "Capacidad", campoCapacidad);
        agregarFila(formulario, 2, "Computador anfitrión", comboAnfitrion);
        add(formulario, BorderLayout.CENTER);

        JButton botonCrear = new JButton("Crear buffer");
        botonCrear.addActionListener(e -> crear());
        JPanel sur = new JPanel();
        sur.add(botonCrear);
        add(sur, BorderLayout.SOUTH);

        setCantidadComputadores(2);
    }

    public void setCreador(CreadorBuffers creador) {
        this.creador = creador;
    }

    public final void setCantidadComputadores(int cantidad) {
        comboAnfitrion.removeAllItems();
        for (int i = 1; i <= cantidad; i++) {
            comboAnfitrion.addItem("Computador " + i);
        }
    }

    private void crear() {
        try {
            String id = campoId.getText().trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("El nombre del buffer es obligatorio.");
            }
            if (id.length() > 20 || id.contains(",")) {
                throw new IllegalArgumentException("El nombre del buffer debe tener máximo 20 caracteres y sin comas.");
            }
            String texto = campoCapacidad.getText().trim();
            int capacidad;
            try {
                capacidad = Integer.parseInt(texto);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("La capacidad debe ser un número entero.");
            }
            if (capacidad < 1 || capacidad > 1000) {
                throw new IllegalArgumentException("La capacidad debe estar entre 1 y 1000.");
            }
            int anfitrion = comboAnfitrion.getSelectedIndex() + 1;
            if (creador == null) {
                throw new IllegalStateException("Todavía no hay un sistema conectado.");
            }
            creador.crearBuffer(id, capacidad, anfitrion);
            campoId.setText("");
            campoCapacidad.setText("");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Datos inválidos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarFila(JPanel panel, int fila, String etiqueta, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = new Insets(4, 5, 4, 5);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(campo, c);
    }
}
