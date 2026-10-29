package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import javax.swing.*;
import java.awt.*;

public class PanelComputador extends JPanel {

    private final String titulo;
    private edu.unimet.avilaos.cluster.Computador pcAsociado;
    /** true mientras el combo se ajusta por codigo, para no registrar un cambio de politica falso. */
    private boolean sincronizandoCombo = false;

    // Componentes del Panel Superior (CPU, RAM y Selector de Política)
    private final JLabel lblCpuEstado = new JLabel("CPU: Ociosa | Modo: USUARIO | PC: 0");
    private final JLabel lblPrioridadProceso = new JLabel("Prioridad: -");
    private final JLabel lblDeadlineProceso = new JLabel("Deadline: -");
    private final JProgressBar pbMemoria = new JProgressBar(0, 100);
    private final JComboBox<TipoPolitica> cbPolitica = new JComboBox<>(TipoPolitica.values());

    // Tablas de Colas
    private final ModeloTablaPCB modeloNuevos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloListos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloBloqueados = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloTerminados = new ModeloTablaPCB();

    public PanelComputador(String titulo) {
        this.titulo = titulo;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(titulo));

        // 1. PANEL SUPERIOR (Información del Procesador y Memoria)
        JPanel panelSuperior = new JPanel(new GridLayout(4, 1, 5, 5));

        // Subpanel de Estado General CPU
        JPanel panelInfoCpu = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelInfoCpu.add(lblCpuEstado);

        // Subpanel de Detalle (Prioridad y Deadline)
        JPanel panelDetalleCpu = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panelDetalleCpu.add(lblPrioridadProceso);
        panelDetalleCpu.add(lblDeadlineProceso);

        // Subpanel de Política con Enum TipoPolitica
        JPanel panelPolitica = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panelPolitica.add(new JLabel("Política:"));
        
        cbPolitica.addActionListener(e -> {
            TipoPolitica seleccionada = (TipoPolitica) cbPolitica.getSelectedItem();
            if (seleccionada != null && !sincronizandoCombo) {
                cambiarPoliticaSimulacion(seleccionada);
            }
        });
        panelPolitica.add(cbPolitica);

        // Configuración Barra de Memoria
        pbMemoria.setStringPainted(true);
        pbMemoria.setString("RAM: 0 MB / 0 MB");

        panelSuperior.add(panelInfoCpu);
        panelSuperior.add(panelDetalleCpu);
        panelSuperior.add(pbMemoria);
        panelSuperior.add(panelPolitica);

        add(panelSuperior, BorderLayout.NORTH);

        // 2. PANEL CENTRAL (Tablas de las Colas del Sistema)
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

    /**
     * Enlaza este panel con la instancia real del Computador backend.
     */
    public void asociarComputador(edu.unimet.avilaos.cluster.Computador pc) {
        this.pcAsociado = pc;
        if (pc != null && pc.getPlanificador() != null) {
            sincronizandoCombo = true;
            cbPolitica.setSelectedItem(pc.getPlanificador().getTipoPolitica());
            sincronizandoCombo = false;
        }
    }

    /**
     * Actualiza la interfaz con el estado actual del Computador en cada ciclo de reloj.
     */
    public void actualizarDatos(edu.unimet.avilaos.cluster.Computador pc) {
        if (pc == null) return;
        
        // Guardar referencia al computador si aún no la tenía
        if (this.pcAsociado == null) {
            asociarComputador(pc);
        }

        // 1. Estado de CPU, Prioridad y Deadline
        PCB enEjecucion = pc.getCpu().getEnEjecucion();
        if (enEjecucion != null) {
            lblCpuEstado.setText("CPU: " + enEjecucion.getNombre() 
                               + " | Modo: " + pc.getCpu().getModo() 
                               + " | PC: " + pc.getCpu().getPc());
            lblPrioridadProceso.setText("Prioridad: " + enEjecucion.getPrioridad());
            lblDeadlineProceso.setText("Deadline: " + enEjecucion.getDeadlineRestante() + " ciclos");
        } else {
            lblCpuEstado.setText("CPU: Ociosa | Modo: " + pc.getCpu().getModo() + " | PC: " + pc.getCpu().getPc());
            lblPrioridadProceso.setText("Prioridad: -");
            lblDeadlineProceso.setText("Deadline: -");
        }

        // 2. Estado de la RAM
        int totalRam = pc.getMemoria().getCapacidad();
        int usadaRam = pc.getMemoria().getUsada();
        pbMemoria.setMaximum(totalRam);
        pbMemoria.setValue(usadaRam);
        pbMemoria.setString("RAM: " + usadaRam + " MB / " + totalRam + " MB");

        // 3. Listas de PCBs por cola
        modeloNuevos.setDatos(pc.getPlanificador().getNuevos());
        modeloListos.setDatos(pc.getPlanificador().getListos());
        modeloBloqueados.setDatos(pc.getPlanificador().getBloqueados());
        modeloTerminados.setDatos(pc.getPlanificador().getTerminados());
    }

    private void cambiarPoliticaSimulacion(TipoPolitica nuevaPolitica) {
        if (pcAsociado == null || pcAsociado.getPlanificador() == null) return;
        if (pcAsociado.getPlanificador().getTipoPolitica() == nuevaPolitica) return;
        long ciclo = pcAsociado.getCluster() != null ? pcAsociado.getCluster().getReloj().getCicloActual() : 0;
        pcAsociado.getPlanificador().cambiarPolitica(nuevaPolitica, ciclo);
    }

    public String getTitulo() { return titulo; }
    public JComboBox<TipoPolitica> getCbPolitica() { return cbPolitica; }
    public ModeloTablaPCB getModeloNuevos() { return modeloNuevos; }
    public ModeloTablaPCB getModeloListos() { return modeloListos; }
    public ModeloTablaPCB getModeloBloqueados() { return modeloBloqueados; }
    public ModeloTablaPCB getModeloTerminados() { return modeloTerminados; }
}