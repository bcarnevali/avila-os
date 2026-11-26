package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import javax.swing.*;
import java.awt.*;

public class PanelComputador extends JPanel {

    private final String titulo;
    private edu.unimet.avilaos.cluster.Computador pcAsociado;
    private boolean sincronizandoCombo = false;
    private java.util.function.Consumer<TipoPolitica> controlPolitica;

    public void setControlPolitica(java.util.function.Consumer<TipoPolitica> control) {
        this.controlPolitica = control;
    }

    private final JLabel lblCpuEstado = new JLabel("CPU: Ociosa | Modo: USUARIO | PC: 0");
    private final JLabel lblPrioridadProceso = new JLabel("Prioridad: -");
    private final JLabel lblDeadlineProceso = new JLabel("Deadline: -");
    private final JProgressBar pbMemoria = new JProgressBar(0, 100);
    private final JComboBox<TipoPolitica> cbPolitica = new JComboBox<>(TipoPolitica.values());

    private final ModeloTablaPCB modeloNuevos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloListos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloBloqueados = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloListosSuspendidos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloBloqueadosSuspendidos = new ModeloTablaPCB();
    private final ModeloTablaPCB modeloTerminados = new ModeloTablaPCB();

    public PanelComputador(String titulo) {
        this.titulo = titulo;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(titulo));

        JPanel panelSuperior = new JPanel(new GridLayout(3, 1, 5, 5));

        JPanel panelInfoCpu = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panelInfoCpu.add(lblCpuEstado);
        panelInfoCpu.add(lblPrioridadProceso);
        panelInfoCpu.add(lblDeadlineProceso);

        JPanel panelPolitica = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelPolitica.add(new JLabel("Política de CPU:"));
        cbPolitica.addActionListener(e -> {
            TipoPolitica seleccionada = (TipoPolitica) cbPolitica.getSelectedItem();
            if (seleccionada != null && !sincronizandoCombo) {
                cambiarPoliticaSimulacion(seleccionada);
            }
        });
        panelPolitica.add(cbPolitica);

        pbMemoria.setStringPainted(true);
        pbMemoria.setString("RAM: 0 / 0");

        panelSuperior.add(panelInfoCpu);
        panelSuperior.add(pbMemoria);
        panelSuperior.add(panelPolitica);

        add(panelSuperior, BorderLayout.NORTH);

        JPanel panelColas = new JPanel(new GridLayout(3, 2, 6, 6));
        panelColas.add(crearSubPanelTabla("Nuevos", modeloNuevos));
        panelColas.add(crearSubPanelTabla("Listos", modeloListos));
        panelColas.add(crearSubPanelTabla("Bloqueados", modeloBloqueados));
        panelColas.add(crearSubPanelTabla("Listo / Suspendido", modeloListosSuspendidos));
        panelColas.add(crearSubPanelTabla("Bloqueado / Suspendido", modeloBloqueadosSuspendidos));
        panelColas.add(crearSubPanelTabla("Terminados", modeloTerminados));

        add(panelColas, BorderLayout.CENTER);
    }

    private JPanel crearSubPanelTabla(String nombreCola, ModeloTablaPCB modelo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(nombreCola));
        panel.add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        return panel;
    }

    public void actualizarPermisosVisuales(boolean esAdmin) {
        cbPolitica.setEnabled(esAdmin);
    }

    public void asociarComputador(edu.unimet.avilaos.cluster.Computador pc) {
        this.pcAsociado = pc;
        if (pc != null && pc.getPlanificador() != null) {
            sincronizandoCombo = true;
            cbPolitica.setSelectedItem(pc.getPlanificador().getTipoPolitica());
            sincronizandoCombo = false;
        }
    }

    public void actualizarDatos(edu.unimet.avilaos.cluster.Computador pc) {
        if (pc == null) return;
        if (this.pcAsociado == null) {
            asociarComputador(pc);
        }

        PCB enEjecucion = pc.getCpu().getEnEjecucion();
        if (enEjecucion != null) {
            lblCpuEstado.setText("CPU: " + enEjecucion.getNombre()
                    + " | Modo: " + pc.getCpu().getModo()
                    + " | PC: " + pc.getCpu().getPc());
            lblPrioridadProceso.setText("Prioridad: " + enEjecucion.getPrioridad());
            lblDeadlineProceso.setText("Deadline: " + enEjecucion.getDeadlineRestante() + " ciclos"
                    + (enEjecucion.getPasoBuffer() > 0 ? " (buffer " + enEjecucion.getIdBuffer() + ")" : ""));
        } else {
            lblCpuEstado.setText("CPU: Ociosa | Modo: " + pc.getCpu().getModo() + " | PC: " + pc.getCpu().getPc());
            lblPrioridadProceso.setText("Prioridad: -");
            lblDeadlineProceso.setText("Deadline: -");
        }

        int totalRam = pc.getMemoria().getCapacidad();
        int usadaRam = pc.getMemoria().getUsada();
        pbMemoria.setMaximum(totalRam);
        pbMemoria.setValue(usadaRam);
        pbMemoria.setString("RAM usada: " + usadaRam + " | libre: " + (totalRam - usadaRam) + " | total: " + totalRam);

        modeloNuevos.setDatos(pc.getPlanificador().getNuevos());
        modeloListos.setDatos(pc.getPlanificador().getListos());
        modeloBloqueados.setDatos(pc.getPlanificador().getBloqueados());
        modeloListosSuspendidos.setDatos(pc.getPlanificador().getListosSuspendidos());
        modeloBloqueadosSuspendidos.setDatos(pc.getPlanificador().getBloqueadosSuspendidos());
        modeloTerminados.setDatos(pc.getPlanificador().getTerminados());
    }

    private void cambiarPoliticaSimulacion(TipoPolitica nuevaPolitica) {
        if (pcAsociado == null || pcAsociado.getPlanificador() == null) return;
        if (pcAsociado.getPlanificador().getTipoPolitica() == nuevaPolitica) return;
        try {
            if (controlPolitica == null) throw new IllegalStateException("Control de CPU no conectado.");
            controlPolitica.accept(nuevaPolitica);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            asociarComputador(pcAsociado);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación no permitida", JOptionPane.WARNING_MESSAGE);
        }
    }

    public String getTitulo() { return titulo; }
    public JComboBox<TipoPolitica> getCbPolitica() { return cbPolitica; }
    public ModeloTablaPCB getModeloNuevos() { return modeloNuevos; }
    public ModeloTablaPCB getModeloListos() { return modeloListos; }
    public ModeloTablaPCB getModeloBloqueados() { return modeloBloqueados; }
    public ModeloTablaPCB getModeloListosSuspendidos() { return modeloListosSuspendidos; }
    public ModeloTablaPCB getModeloBloqueadosSuspendidos() { return modeloBloqueadosSuspendidos; }
    public ModeloTablaPCB getModeloTerminados() { return modeloTerminados; }
}