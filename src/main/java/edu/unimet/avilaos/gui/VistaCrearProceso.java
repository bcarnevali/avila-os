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

/**
 * Vista 1: formulario para crear procesos. Solo recoge datos; no decide nada
 * del SO.
 */
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

    /**
     * Quien conecte la GUI con el sistema se registra aquí.
     */
    public void setCreador(CreadorProcesos creador) {
        this.creador = creador;
    }

    /**
     * Rehace la lista de computadores según la configuración.
     */
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
        try {
            SolicitudProceso solicitud = leerSolicitud();
            if (creador == null) {
                JOptionPane.showMessageDialog(this,
                        "Los datos son válidos, pero todavía no hay un sistema conectado.",
                        "Sin conexión", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            creador.crearProceso(solicitud);
            limpiarCampos();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Datos inválidos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private SolicitudProceso leerSolicitud() {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (nombre.length() > 30) {
            throw new IllegalArgumentException("El nombre no puede pasar de 30 caracteres.");
        }

        TipoProceso tipo = (TipoProceso) comboTipo.getSelectedItem();
        int instrucciones = leerEntero(campoInstrucciones, "Instrucciones", 1, 1_000_000);
        int memoria = leerEntero(campoMemoria, "Memoria requerida", 1, 1_000_000);
        int prioridad = leerEntero(campoPrioridad, "Prioridad", 1, 10);
        int deadline = leerEntero(campoDeadline, "Deadline", 1, 1_000_000);
        int idComputador = leerComputador();

        int ciclosEntreES = 0;
        int ciclosES = 0;
        String buffer = null;
        int ciclosPorElemento = 0;
        int elementos = 0;

        if (tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR) {
            buffer = campoBuffer.getText().trim();
            if (buffer.isEmpty()) {
                throw new IllegalArgumentException("Debe indicar el buffer.");
            }
            if (buffer.length() > 20) {
                throw new IllegalArgumentException("El nombre del buffer no puede pasar de 20 caracteres.");
            }
            ciclosPorElemento = leerEntero(campoCiclosPorElemento, "Ciclos por elemento", 1, 1_000_000);
            elementos = leerEntero(campoElementos, "Elementos requeridos", 1, 1_000_000);
        } else {
            int minimoES = (tipo == TipoProceso.IO_BOUND) ? 1 : 0;
            ciclosEntreES = leerEntero(campoCiclosEntreES, "Pide E/S cada", minimoES, 1_000_000);
            ciclosES = leerEntero(campoCiclosES, "Ciclos que tarda la E/S", minimoES, 1_000_000);
        }

        return new SolicitudProceso(
            nombre, 
            tipo, 
            prioridad, 
            idComputador,       
            memoria,           
            instrucciones, 
            deadline, 
            buffer, 
            ciclosPorElemento, 
            elementos, 
            ciclosEntreES, 
            ciclosES
        );
    }

    private int leerEntero(JTextField campo, String nombreCampo, int minimo, int maximo) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("El campo \"" + nombreCampo + "\" es obligatorio.");
        }
        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El campo \"" + nombreCampo + "\" debe ser un número entero.");
        }
        if (valor < minimo || valor > maximo) {
            throw new IllegalArgumentException("El campo \"" + nombreCampo
                    + "\" debe estar entre " + minimo + " y " + maximo + ".");
        }
        return valor;
    }

    private int leerComputador() {
        int indice = comboComputador.getSelectedIndex();
        if (indice < 0) {
            throw new IllegalArgumentException("Debe elegir un computador.");
        }
        return indice == 0 ? SolicitudProceso.AUTOMATICO : indice;
    }

    private void limpiarCampos() {
        campoNombre.setText("");
        campoInstrucciones.setText("");
        campoMemoria.setText("");
        campoPrioridad.setText("");
        campoDeadline.setText("");
        campoCiclosEntreES.setText("");
        campoCiclosES.setText("");
        campoBuffer.setText("");
        campoCiclosPorElemento.setText("");
        campoElementos.setText("");
    }
}