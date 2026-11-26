package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.modelo.enums.ModoUsuario;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Barra superior general: modo, estado de JSON, reloj y parámetros globales. */
public class PanelControles extends JPanel {

    private final JComboBox<ModoUsuario> comboModo = new JComboBox<>(ModoUsuario.values());
    private final JComboBox<String> comboUsuario = new JComboBox<>(new String[]{"user1", "user2"});
    private final JLabel lblEstadoModo = new JLabel("MODO: ADMINISTRADOR");

    private final JButton botonConfiguracion = new JButton("1. Cargar JSON");
    private final JButton botonCrearProceso = new JButton("Crear proceso");
    private final JLabel lblJsonEstado = new JLabel("JSON: Pendientes");

    private final JButton botonIniciar = new JButton("Iniciar");
    private final JButton botonPausar = new JButton("Pausar");
    private final JSpinner spinnerDuracion = new JSpinner(new SpinnerNumberModel(500, 50, 5000, 50));
    private final JSpinner spinnerQuantum = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JButton botonAplicar = new JButton("Aplicar");
    private final JLabel etiquetaCiclo = new JLabel("Ciclo: 0");

    private boolean simulacionCargada = false;
    private Runnable onCambioModo;

    public PanelControles() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));

        ValidadorCampos.soloEnteros(spinnerDuracion);
        ValidadorCampos.soloEnteros(spinnerQuantum);

        lblEstadoModo.setFont(lblEstadoModo.getFont().deriveFont(Font.BOLD));
        lblEstadoModo.setOpaque(true);
        lblEstadoModo.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        lblJsonEstado.setFont(lblJsonEstado.getFont().deriveFont(Font.BOLD));
        lblJsonEstado.setForeground(new Color(180, 40, 40));

        etiquetaCiclo.setFont(etiquetaCiclo.getFont().deriveFont(Font.BOLD));

        comboUsuario.setEnabled(false);
        comboModo.addActionListener(e -> {
            actualizarEstadoModo();
            if (onCambioModo != null) {
                onCambioModo.run();
            }
        });
        comboUsuario.addActionListener(e -> {
            actualizarEstadoModo();
            if (onCambioModo != null) {
                onCambioModo.run();
            }
        });

        add(lblEstadoModo);
        add(comboModo);
        add(comboUsuario);
        add(botonConfiguracion);
        add(lblJsonEstado);
        add(botonCrearProceso);
        add(botonIniciar);
        add(botonPausar);
        add(new JLabel("Ciclo (ms):"));
        add(spinnerDuracion);
        add(new JLabel("Quantum:"));
        add(spinnerQuantum);
        add(botonAplicar);
        add(etiquetaCiclo);

        botonPausar.setEnabled(false);
        botonCrearProceso.setEnabled(false);
        actualizarEstadoModo();
    }

    public void setOnCambioModo(Runnable onCambioModo) {
        this.onCambioModo = onCambioModo;
    }

    public void marcarJsonCargados(String nombreProcesos, String nombreArchivos) {
        this.simulacionCargada = true;
        lblJsonEstado.setText("JSON OK (" + nombreProcesos + " + " + nombreArchivos + ")");
        lblJsonEstado.setForeground(new Color(20, 125, 45));
        actualizarEstadoModo();
    }

    public void actualizarEstadoModo() {
        boolean esAdmin = getModoActual() == ModoUsuario.ADMINISTRADOR;
        comboUsuario.setEnabled(!esAdmin);
        if (esAdmin) {
            lblEstadoModo.setText("MODO: ADMINISTRADOR");
            lblEstadoModo.setBackground(new Color(215, 240, 220));
            lblEstadoModo.setForeground(new Color(15, 100, 35));
        } else {
            lblEstadoModo.setText("MODO: USUARIO (" + getUsuarioActual() + ")");
            lblEstadoModo.setBackground(new Color(255, 238, 205));
            lblEstadoModo.setForeground(new Color(150, 85, 0));
        }
        botonConfiguracion.setEnabled(esAdmin);
        botonCrearProceso.setEnabled(esAdmin && simulacionCargada);
        spinnerDuracion.setEnabled(esAdmin && simulacionCargada);
        spinnerQuantum.setEnabled(esAdmin && simulacionCargada);
        botonAplicar.setEnabled(esAdmin && simulacionCargada);
    }

    public ModoUsuario getModoActual() {
        ModoUsuario m = (ModoUsuario) comboModo.getSelectedItem();
        return m != null ? m : ModoUsuario.ADMINISTRADOR;
    }

    public String getUsuarioActual() {
        if (getModoActual() == ModoUsuario.ADMINISTRADOR) {
            return "admin";
        }
        Object sel = comboUsuario.getSelectedItem();
        return sel != null ? sel.toString() : "user1";
    }

    public void setCiclo(long ciclo) {
        etiquetaCiclo.setText("Ciclo: " + ciclo);
    }

    public JButton getBotonConfiguracion() { return botonConfiguracion; }
    public JButton getBotonCrearProceso() { return botonCrearProceso; }
    public JButton getBotonIniciar() { return botonIniciar; }
    public JButton getBotonPausar() { return botonPausar; }
    public JButton getBotonAplicar() { return botonAplicar; }

    public int getDuracionCiclo() { return ValidadorCampos.leer(spinnerDuracion, "Duración del ciclo (ms)"); }
    public int getQuantum() { return ValidadorCampos.leer(spinnerQuantum, "Quantum"); }
    public void setDuracionCiclo(int ms) { spinnerDuracion.setValue(ms); }
    public void setQuantum(int quantum) { spinnerQuantum.setValue(quantum); }
}