package edu.unimet.avilaos.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class VistaCrearProceso extends JPanel {

    private final JTextField txtNombre = new JTextField(15);
    private final JTextField txtInstrucciones = new JTextField("10", 15);
    private final JTextField txtMemoria = new JTextField("64", 15);
    private final JTextField txtPrioridad = new JTextField("1", 15);
    private final JTextField txtDeadline = new JTextField("50", 15);
    private final JTextField txtCiclosIo = new JTextField("0", 15);
    
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"CPU_BOUND", "IO_BOUND", "PRODUCTOR", "CONSUMIDOR"});
    private final JComboBox<String> cbComputador = new JComboBox<>(new String[]{"Automático (ÁvilaOS)", "Computador 1", "Computador 2"});
    private final JButton btnCrear = new JButton("Crear Proceso");

    public VistaCrearProceso() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Formulario de Creación de Procesos"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        agregarCampo("Nombre del Proceso:", txtNombre, gbc, row++);
        agregarCampo("Tipo de Proceso:", cbTipo, gbc, row++);
        agregarCampo("Instrucciones Totales (>0):", txtInstrucciones, gbc, row++);
        agregarCampo("Memoria RAM Requerida (MB >0):", txtMemoria, gbc, row++);
        agregarCampo("Prioridad (>=1):", txtPrioridad, gbc, row++);
        agregarCampo("Deadline (Ciclos >0):", txtDeadline, gbc, row++);
        agregarCampo("Ciclos de E/S por ráfaga:", txtCiclosIo, gbc, row++);
        agregarCampo("Asignar a Computador:", cbComputador, gbc, row++);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(btnCrear, gbc);

        // Control dinámico de campos según tipo
        cbTipo.addActionListener(e -> {
            String tipo = (String) cbTipo.getSelectedItem();
            boolean esIo = "IO_BOUND".equals(tipo);
            txtCiclosIo.setEnabled(esIo);
            if (!esIo) txtCiclosIo.setText("0");
        });

        // Evento de validación al presionar el botón
        btnCrear.addActionListener(e -> validarYEnviar());
    }

    private void agregarCampo(String etiqueta, java.awt.Component comp, GridBagConstraints gbc, int fila) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        add(new JLabel(etiqueta), gbc);

        gbc.gridx = 1;
        add(comp, gbc);
    }

    private void validarYEnviar() {
        try {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("El nombre del proceso no puede estar vacío.");
            }

            int instrucciones = Integer.parseInt(txtInstrucciones.getText().trim());
            if (instrucciones <= 0) throw new IllegalArgumentException("Las instrucciones deben ser mayor a 0.");

            int memoria = Integer.parseInt(txtMemoria.getText().trim());
            if (memoria <= 0) throw new IllegalArgumentException("La memoria debe ser mayor a 0 MB.");

            int prioridad = Integer.parseInt(txtPrioridad.getText().trim());
            if (prioridad < 1) throw new IllegalArgumentException("La prioridad debe ser al menos 1.");

            int deadline = Integer.parseInt(txtDeadline.getText().trim());
            if (deadline <= 0) throw new IllegalArgumentException("El deadline debe ser mayor a 0.");

            String tipo = (String) cbTipo.getSelectedItem();
            int ciclosIo = Integer.parseInt(txtCiclosIo.getText().trim());
            if ("IO_BOUND".equals(tipo) && ciclosIo <= 0) {
                throw new IllegalArgumentException("Para procesos IO_BOUND, los ciclos de E/S deben ser al menos 1.");
            }

            JOptionPane.showMessageDialog(this, "Proceso '" + nombre + "' validado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarCampos();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error de tipo de dato: Por favor introduce valores numéricos enteros válidos en los campos correspondientes.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación Fallida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtInstrucciones.setText("10");
        txtMemoria.setText("64");
        txtPrioridad.setText("1");
        txtDeadline.setText("50");
        txtCiclosIo.setText("0");
    }
}