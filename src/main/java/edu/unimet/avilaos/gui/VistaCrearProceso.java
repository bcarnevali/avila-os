package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.modelo.enums.TipoProceso;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
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
import javax.swing.SwingConstants;

/** Vista 1: formulario para crear procesos. Solo recoge datos; no decide nada del SO. */
public class VistaCrearProceso extends JPanel {

    private static final String PANEL_ES = "es";
    private static final String PANEL_BUFFER = "buffer";

    private final JTextField campoNombre = new JTextField(20);
    private final JTextField campoInstrucciones = new JTextField(20);
    private final JTextField campoMemoria = new JTextField(20);
    private final JTextField campoPrioridad = new JTextField(20);
    private final JTextField campoDeadline = new JTextField(20);
    private final JComboBox<TipoProceso> comboTipo = new JComboBox<>(TipoProceso.values());
    private final JComboBox<String> comboComputador = new JComboBox<>();

    private final JTextField campoCiclosEntreES = new JTextField(20);
    private final JTextField campoCiclosES = new JTextField(20);

    private final JTextField campoBuffer = new JTextField(20);
    private final JTextField campoCiclosPorElemento = new JTextField(20);
    private final JTextField campoElementos = new JTextField(20);

    private final CardLayout cardsTipo = new CardLayout();
    private final JPanel panelTipo = new JPanel(cardsTipo);

    private CreadorProcesos creador;

    public VistaCrearProceso() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(new JLabel("Vista 1: Crear proceso", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        agregarFila(formulario, 0, "Nombre", campoNombre);
        agregarFila(formulario, 1, "Instrucciones", campoInstrucciones);
        agregarFila(formulario, 2, "Memoria requerida", campoMemoria);
        agregarFila(formulario, 3, "Prioridad", campoPrioridad);
        agregarFila(formulario, 4, "Deadline (ciclos)", campoDeadline);
        agregarFila(formulario, 5, "Tipo de proceso", comboTipo);
        agregarFila(formulario, 6, "Computador", comboComputador);

        panelTipo.add(crearPanelES(), PANEL_ES);
        panelTipo.add(crearPanelBuffer(), PANEL_BUFFER);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 5, 5, 5);
        formulario.add(panelTipo, c);

        JPanel centro = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centro.add(formulario);
        add(centro, BorderLayout.CENTER);

        JButton botonCrear = new JButton("Crear proceso");
        botonCrear.addActionListener(e -> crear());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sur.add(botonCrear);
        add(sur, BorderLayout.SOUTH);

        comboTipo.addActionListener(e -> mostrarPanelDelTipo());
        setCantidadComputadores(2);
        mostrarPanelDelTipo();
    }

    /** Quien conecte la GUI con el sistema se registra aquí. */
    public void setCreador(CreadorProcesos creador) {
        this.creador = creador;
    }

    /** Rehace la lista de computadores según la configuración. */
    public final void setCantidadComputadores(int cantidad) {
        comboComputador.removeAllItems();
        comboComputador.addItem("Automático");
        for (int i = 1; i <= cantidad; i++) {
            comboComputador.addItem("Computador " + i);
        }
    }

    private JPanel crearPanelES() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("CPU bound / I/O bound"));
        agregarFila(panel, 0, "Pide E/S cada (instrucciones)", campoCiclosEntreES);
        agregarFila(panel, 1, "Ciclos que tarda la E/S", campoCiclosES);
        return panel;
    }

    private JPanel crearPanelBuffer() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Productor / consumidor"));
        agregarFila(panel, 0, "Buffer", campoBuffer);
        agregarFila(panel, 1, "Ciclos por elemento", campoCiclosPorElemento);
        agregarFila(panel, 2, "Elementos requeridos", campoElementos);
        return panel;
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

    private void mostrarPanelDelTipo() {
        TipoProceso tipo = (TipoProceso) comboTipo.getSelectedItem();
        if (tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR) {
            cardsTipo.show(panelTipo, PANEL_BUFFER);
        } else {
            cardsTipo.show(panelTipo, PANEL_ES);
        }
    }

    private void crear() {
        JOptionPane.showMessageDialog(this, "Validación pendiente");
    }
}