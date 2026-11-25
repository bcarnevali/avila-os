package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/** Vista 2: computadores arriba; log, metricas y buffers abajo en pestañas. */
public class VistaSimulacion extends JPanel {

    private final JPanel panelComputadores = new JPanel();
    private final PanelLog panelLog = new PanelLog();
    private final PanelMetricas panelMetricas = new PanelMetricas();
    private final PanelDisco panelDisco = new PanelDisco();
    private final PanelSistemaArchivos panelArchivos = new PanelSistemaArchivos();

    private PanelComputador[] panelesPc = new PanelComputador[0];


    public VistaSimulacion() {
        setLayout(new BorderLayout(5, 5));

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Log de eventos", panelLog);
        pestanas.addTab("Métricas", panelMetricas);
        pestanas.addTab("Disco", panelDisco);
        pestanas.addTab("Archivos", panelArchivos);
        pestanas.setPreferredSize(new Dimension(0, 300));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(panelComputadores), pestanas);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);
    }

    /** Crea un panel por cada computador del cluster y reinicia las metricas. */
    public void cargarCluster(Cluster cluster) {
        if (cluster == null) return;
        int total = cluster.getCantidad();

        panelComputadores.removeAll();
        panelComputadores.setLayout(new GridLayout(1, total, 10, 10));
        panelMetricas.reiniciar();

        panelesPc = new PanelComputador[total];
        for (int i = 1; i <= total; i++) {
            Computador pc = cluster.getComputador(i);
            panelesPc[i - 1] = new PanelComputador("Computador " + pc.getId());
            panelesPc[i - 1].asociarComputador(pc);
            panelComputadores.add(panelesPc[i - 1]);
        }

        panelDisco.getMetricas().reiniciar();

        revalidate();
        repaint();
    }

    /** Refresca todo lo visible. Lo llama el Timer del controlador (hilo de Swing). */
    public void actualizarDatos(Cluster cluster) {
        if (cluster == null) return;
        for (int i = 1; i <= cluster.getCantidad() && i - 1 < panelesPc.length; i++) {
            Computador pc = cluster.getComputador(i);
            if (pc != null) {
                panelesPc[i - 1].actualizarDatos(pc);
            }
        }

        panelMetricas.actualizar(cluster);

        Computador pc = cluster.getComputador(1);
        panelDisco.actualizar(pc.getDisco(), cluster.getReloj().getCicloActual());
        panelDisco.getMetricas().setSuspensiones(pc.getMedianoPlazo().getSuspensiones(), pc.getMedianoPlazo().getReactivaciones());
        panelArchivos.refrescar();
    }

    public PanelDisco getPanelDisco() { return panelDisco; }
    public PanelSistemaArchivos getPanelArchivos() { return panelArchivos; }

    public PanelLog getPanelLog() {
        return panelLog;
    }

    public PanelComputador[] getPanelesPc() {
        return panelesPc;
    }
}
