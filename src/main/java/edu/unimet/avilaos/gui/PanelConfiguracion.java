package edu.unimet.avilaos.gui;

import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class PanelConfiguracion extends JPanel {

    private final JComboBox<String> comboPoliticas = new JComboBox<>(new String[]{
        "FCFS (First Come First Served)",
        "RR (Round Robin)",
        "SPN (Shortest Process Next)",
        "SRTN (Shortest Remaining Time Next)",
        "Prioridades",
        "EDF (Earliest Deadline First)"
    });

    private final JSpinner spinQuantum = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JSpinner spinCapacidadBuffer = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));

    public PanelConfiguracion() {
        setLayout(new GridLayout(3, 2, 10, 10));
        setBorder(BorderFactory.createTitledBorder("Configuración Inicial del Sistema"));

        add(new JLabel("Políticas de Planificación:"));
        add(comboPoliticas);

        add(new JLabel("Quantum (ms / ciclos):"));
        add(spinQuantum);

        add(new JLabel("Capacidad Máxima del Buffer:"));
        add(spinCapacidadBuffer);
    }

    public String getPoliticaSeleccionada() {
        return (String) comboPoliticas.getSelectedItem();
    }

    public int getQuantum() {
        return (int) spinQuantum.getValue();
    }

    public int getCapacidadBuffer() {
        return (int) spinCapacidadBuffer.getValue();
    }
}