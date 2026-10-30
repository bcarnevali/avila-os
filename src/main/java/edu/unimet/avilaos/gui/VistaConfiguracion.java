package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.configuracion.BufferInicial;
import edu.unimet.avilaos.configuracion.ConfiguracionSimulacion;
import edu.unimet.avilaos.configuracion.GestorConfiguracion;
import edu.unimet.avilaos.configuracion.ReceptorConfiguracion;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Vista de configuracion: parametros iniciales de la simulacion, carga inicial de
 * procesos y buffers, y guardar/cargar todo en un archivo CSV. Solo arma la
 * configuracion; quien la aplica es el controlador (ReceptorConfiguracion).
 */
public class VistaConfiguracion extends JPanel {

    private final JSpinner spinnerDuracion = new JSpinner(new SpinnerNumberModel(500, 50, 5000, 50));
    private final JSpinner spinnerQuantum = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JSpinner spinnerLatencia = new JSpinner(new SpinnerNumberModel(3, 0, 50, 1));
    private final JSpinner spinnerComputadores = new JSpinner(new SpinnerNumberModel(2,
            ConfiguracionSimulacion.MIN_COMPUTADORES, ConfiguracionSimulacion.MAX_COMPUTADORES, 1));
    private final ModeloTablaComputadores modeloComputadores = new ModeloTablaComputadores();
    private final JTextArea areaCarga = new JTextArea(8, 40);

    private ListaEnlazada<SolicitudProceso> procesos = new ListaEnlazada<>();
    private ListaEnlazada<BufferInicial> buffers = new ListaEnlazada<>();
    private ReceptorConfiguracion receptor;

    public VistaConfiguracion() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Parametros generales
        JPanel parametros = new JPanel(new GridBagLayout());
        agregarFila(parametros, 0, "Duración del ciclo (ms)", spinnerDuracion);
        agregarFila(parametros, 1, "Quantum de Round Robin", spinnerQuantum);
        agregarFila(parametros, 2, "Latencia de la red (ciclos)", spinnerLatencia);
        agregarFila(parametros, 3, "Número de computadores", spinnerComputadores);
        spinnerComputadores.addChangeListener(e -> modeloComputadores.setCantidad((Integer) spinnerComputadores.getValue()));

        // RAM y politica de cada computador
        JTable tabla = new JTable(modeloComputadores);
        tabla.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(TipoPolitica.values())));
        JPanel computadores = new JPanel(new BorderLayout());
        computadores.setBorder(BorderFactory.createTitledBorder("Computadores (RAM entre 16 y 100000)"));
        computadores.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel parametrosArriba = new JPanel(new BorderLayout());
        parametrosArriba.setBorder(BorderFactory.createTitledBorder("Parámetros"));
        parametrosArriba.add(parametros, BorderLayout.NORTH);

        JPanel arriba = new JPanel(new GridLayout(1, 2, 10, 10));
        arriba.setPreferredSize(new Dimension(0, 220));
        arriba.add(parametrosArriba);
        arriba.add(computadores);
        add(arriba, BorderLayout.NORTH);

        // Carga inicial de procesos y buffers
        areaCarga.setEditable(false);
        JPanel carga = new JPanel(new BorderLayout(5, 5));
        carga.setBorder(BorderFactory.createTitledBorder("Carga inicial (se crea al aplicar la configuración)"));
        carga.add(new JScrollPane(areaCarga), BorderLayout.CENTER);
        JPanel botonesCarga = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton botonCapturar = new JButton("Usar los procesos y buffers creados en esta simulación");
        botonCapturar.addActionListener(e -> capturarCarga());
        JButton botonVaciar = new JButton("Vaciar carga inicial");
        botonVaciar.addActionListener(e -> {
            procesos = new ListaEnlazada<>();
            buffers = new ListaEnlazada<>();
            mostrarCarga();
        });
        botonesCarga.add(botonCapturar);
        botonesCarga.add(botonVaciar);
        carga.add(botonesCarga, BorderLayout.SOUTH);
        add(carga, BorderLayout.CENTER);

        // Acciones
        JButton botonAplicar = new JButton("Aplicar y reiniciar la simulación");
        botonAplicar.addActionListener(e -> aplicar());
        JButton botonGuardar = new JButton("Guardar en archivo...");
        botonGuardar.addActionListener(e -> guardar());
        JButton botonCargar = new JButton("Cargar archivo...");
        botonCargar.addActionListener(e -> cargar());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sur.add(botonCargar);
        sur.add(botonGuardar);
        sur.add(botonAplicar);
        add(sur, BorderLayout.SOUTH);

        mostrarCarga();
    }

    /** El controlador se registra aqui y la vista muestra la configuracion con la que arranco. */
    public void setReceptor(ReceptorConfiguracion receptor) {
        this.receptor = receptor;
        if (receptor != null && receptor.getConfiguracionActual() != null) {
            mostrar(receptor.getConfiguracionActual());
        }
    }

    /** Arma la configuracion con lo que hay en pantalla. */
    private ConfiguracionSimulacion leer() {
        ConfiguracionSimulacion c = new ConfiguracionSimulacion();
        c.setDuracionCicloMs((Integer) spinnerDuracion.getValue());
        c.setQuantum((Integer) spinnerQuantum.getValue());
        c.setLatencia((Integer) spinnerLatencia.getValue());
        c.setComputadores(modeloComputadores.getRam(), modeloComputadores.getPoliticas());
        c.setProcesos(procesos);
        c.setBuffers(buffers);
        c.validar();
        return c;
    }

    private void mostrar(ConfiguracionSimulacion c) {
        spinnerDuracion.setValue(c.getDuracionCicloMs());
        spinnerQuantum.setValue(c.getQuantum());
        spinnerLatencia.setValue(c.getLatencia());
        spinnerComputadores.setValue(c.getCantidadComputadores());
        modeloComputadores.setDatos(c.getRamPorComputador(), c.getPoliticas());
        procesos = c.getProcesos();
        buffers = c.getBuffers();
        mostrarCarga();
    }

    private void mostrarCarga() {
        StringBuilder sb = new StringBuilder();
        sb.append("Buffers: ").append(buffers.tamano()).append('\n');
        for (int i = 0; i < buffers.tamano(); i++) {
            BufferInicial b = buffers.obtener(i);
            sb.append("  ").append(b.id()).append(" - capacidad ").append(b.capacidad())
                    .append(", en el Computador ").append(b.idAnfitrion()).append('\n');
        }
        sb.append("Procesos: ").append(procesos.tamano()).append('\n');
        for (int i = 0; i < procesos.tamano(); i++) {
            SolicitudProceso p = procesos.obtener(i);
            sb.append("  ").append(p.nombre()).append(" - ").append(p.tipo())
                    .append(", ").append(p.instrucciones()).append(" instr., memoria ").append(p.memoria())
                    .append(", prioridad ").append(p.prioridad()).append(", deadline ").append(p.deadline())
                    .append(", ").append(p.idComputador() == SolicitudProceso.AUTOMATICO
                            ? "automático" : "Computador " + p.idComputador());
            if (p.idBuffer() != null) {
                sb.append(", buffer ").append(p.idBuffer());
            }
            sb.append('\n');
        }
        areaCarga.setText(sb.toString());
        areaCarga.setCaretPosition(0);
    }

    private void capturarCarga() {
        if (receptor == null) return;
        ConfiguracionSimulacion carga = receptor.capturarCargaActual();
        procesos = carga.getProcesos();
        buffers = carga.getBuffers();
        mostrarCarga();
    }

    private void aplicar() {
        if (receptor == null) return;
        try {
            ConfiguracionSimulacion c = leer();
            int r = JOptionPane.showConfirmDialog(this,
                    "Se va a reiniciar la simulación con esta configuración. ¿Continuar?",
                    "Aplicar configuración", JOptionPane.OK_CANCEL_OPTION);
            if (r == JOptionPane.OK_OPTION) {
                receptor.aplicarConfiguracion(c);
                JOptionPane.showMessageDialog(this, "Configuración aplicada. Ve a Simulación y dale Iniciar.");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Configuración inválida", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() {
        try {
            ConfiguracionSimulacion c = leer();
            JFileChooser selector = crearSelector();
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File archivo = selector.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
            }
            GestorConfiguracion.guardar(c, archivo);
            JOptionPane.showMessageDialog(this, "Configuración guardada en " + archivo.getName()
                    + ".\nSi se llama " + GestorConfiguracion.ARCHIVO_POR_DEFECTO.getName()
                    + " y está en la carpeta del proyecto, se carga sola al abrir ÁvilaOS.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Configuración inválida", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargar() {
        JFileChooser selector = crearSelector();
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            mostrar(GestorConfiguracion.cargar(selector.getSelectedFile()));
            JOptionPane.showMessageDialog(this, "Configuración cargada. Revísala y dale \"Aplicar y reiniciar la simulación\".");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Archivo inválido", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser crearSelector() {
        JFileChooser selector = new JFileChooser(new File(System.getProperty("user.dir")));
        selector.setFileFilter(new FileNameExtensionFilter("Configuración CSV", "csv"));
        selector.setSelectedFile(GestorConfiguracion.ARCHIVO_POR_DEFECTO);
        return selector;
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
