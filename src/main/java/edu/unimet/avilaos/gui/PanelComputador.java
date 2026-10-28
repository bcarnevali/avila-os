package edu.unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class PanelComputador extends JPanel {

    private final String titulo;
    private final JLabel lblCpuEstado = new JLabel("CPU: Ociosa | Modo: USUARIO | PC: 0");
    private final JProgressBar pbMemoria = new JProgressBar(0, 100);
    private final JComboBox<String> cbPolitica = new JComboBox<>(new String[]{"FCFS", "RoundRobin", "EDF", "Prioridades"});

    private final ModeloTablaPCB modeloNuevos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloListos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloBloqueados = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloTerminados = new ModeloTablaPCB();

    public PanelComputador(String titulo) {
        this.titulo = titulo;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(titulo));

        // Panel superior: Estado de CPU, RAM y Selector de Política
        JPanel panelSuperior = new JPanel(new GridLayout(3, 1, 5, 5));
        
        JPanel panelPolitica = new JPanel(new BorderLayout());
        panelPolitica.add(new JLabel("Política: "), BorderLayout.WEST);
        panelPolitica.add(cbPolitica, BorderLayout.CENTER);

        pbMemoria.setStringPainted(true);
        pbMemoria.setString("RAM: 0 MB / 0 MB");

        panelSuperior.add(lblCpuEstado);
        panelSuperior.add(pbMemoria);
        panelSuperior.add(panelPolitica);

        add(panelSuperior, BorderLayout.NORTH);

        // Panel central: Tablas de las Colas del Sistema (Nuevos, Listos, Bloqueados, Terminados)
        JPanel panelColas = new JPanel(new GridLayout(2, 2, 5, 5));
        panelColas.add(crearSubPanelTabla("Nuevos", modeloNuevos));
        panelColas.add(crearSubPanelTabla("Listos", modeloListos));
        panelColas.add(crearSubPanelTabla("Bloqueados", modeloBloqueados));
        panelColas.add(crearSubPanelTabla("Terminados", modeloTerminados));

        add(panelColas, BorderLayout.CENTER);
    }

    private JPanel crearSubPanelTabla(String nombreCola, ModeloTablaPCB modelo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(nombreCola));
        panel.add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        return panel;
    }

    public String getTitulo() {
        return titulo;
    }
    /**
     * Actualiza la interfaz con el estado actual del Computador en cada ciclo de reloj.
     */
    public void actualizarDatos(edu.unimet.avilaos.cluster.Computador pc) {
        if (pc == null) return;

        // 1. Estado de la CPU
        boolean ejecutando = (pc.getCpu().getEnEjecucion() != null);
        String nombreProceso = ejecutando ? pc.getCpu().getEnEjecucion().getNombre() : "Ociosa";
        lblCpuEstado.setText("CPU: " + nombreProceso 
                           + " | Modo: " + pc.getCpu().getModo() 
                           + " | PC: " + pc.getCpu().getPc());

        // 2. Estado de la RAM
        int totalRam = pc.getMemoria().getCapacidad();
        int usadaRam = pc.getMemoria().getUsada();
        pbMemoria.setMaximum(totalRam);
        pbMemoria.setValue(usadaRam);
        pbMemoria.setString("RAM: " + usadaRam + " MB / " + totalRam + " MB");

        // 3. Modelos de Tabla (Listas de PCBs por cola)
        // 3. Modelos de Tabla (Listas de PCBs por cola)
        modeloNuevos.setDatos(pc.getPlanificador().getNuevos());
        modeloListos.setDatos(pc.getPlanificador().getListos());
        modeloBloqueados.setDatos(pc.getPlanificador().getBloqueados());
        modeloTerminados.setDatos(pc.getPlanificador().getTerminados());
    }

    // Getters necesarios para listeners y eventos de política
    public JComboBox<String> getCbPolitica() {
        return cbPolitica;
    }

    public ModeloTablaPCB getModeloNuevos() { return modeloNuevos; }
    public ModeloTablaPCB getModeloListos() { return modeloListos; }
    public ModeloTablaPCB getModeloBloqueados() { return modeloBloqueados; }
    public ModeloTablaPCB getModeloTerminados() { return modeloTerminados; }
}
