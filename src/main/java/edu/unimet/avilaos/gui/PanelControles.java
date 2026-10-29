package edu.unimet.avilaos.gui;

import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Controles de la simulación. Solo tiene los componentes; el controlador les da comportamiento. */
public class PanelControles extends JPanel {

    private final JButton botonIniciar = new JButton("Iniciar");
    private final JButton botonPausar = new JButton("Pausar");
    private final JSpinner spinnerDuracion = new JSpinner(new SpinnerNumberModel(500, 50, 5000, 50));
    private final JSpinner spinnerQuantum = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JButton botonAplicar = new JButton("Aplicar");
    private final JLabel etiquetaCiclo = new JLabel("Ciclo: 0");

    public PanelControles() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setBorder(BorderFactory.createEtchedBorder());
        add(botonIniciar);
        add(botonPausar);
        add(new JLabel("Duración del ciclo (ms):"));
        add(spinnerDuracion);
        add(new JLabel("Quantum:"));
        add(spinnerQuantum);
        add(botonAplicar);
        add(etiquetaCiclo);
        botonPausar.setEnabled(false);
    }

    public void setCiclo(long ciclo) {
        etiquetaCiclo.setText("Ciclo: " + ciclo);
    }

    public JButton getBotonIniciar() { return botonIniciar; }
    public JButton getBotonPausar() { return botonPausar; }
    public JButton getBotonAplicar() { return botonAplicar; }
    public int getDuracionCiclo() { return (Integer) spinnerDuracion.getValue(); }
    public int getQuantum() { return (Integer) spinnerQuantum.getValue(); }
    public void setDuracionCiclo(int ms) { spinnerDuracion.setValue(ms); }
    public void setQuantum(int quantum) { spinnerQuantum.setValue(quantum); }
}
