package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import edu.unimet.avilaos.sincronizacion.Buffer;
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
    private final JPanel panelBuffers = new JPanel();

    private PanelComputador[] panelesPc = new PanelComputador[0];
    private PanelBuffers[] panelesBuffers = new PanelBuffers[0];

    public VistaSimulacion() {
        setLayout(new BorderLayout(5, 5));

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Log de eventos", panelLog);
        pestanas.addTab("Métricas", panelMetricas);
        pestanas.addTab("Buffers", new JScrollPane(panelBuffers));
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

        panelesBuffers = new PanelBuffers[0];
        reconstruirBuffers(cluster.getBuffers());

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

        Buffer[] buffers = cluster.getBuffers();
        if (buffers.length != panelesBuffers.length) {
            reconstruirBuffers(buffers);
        }
        for (int i = 0; i < buffers.length; i++) {
            panelesBuffers[i].actualizar(buffers[i]);
        }
    }

    private void reconstruirBuffers(Buffer[] buffers) {
        panelBuffers.removeAll();
        if (buffers.length == 0) {
            panelBuffers.setLayout(new BorderLayout());
            panelBuffers.add(new JLabel("No hay buffers creados", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            panelBuffers.setLayout(new GridLayout(1, buffers.length, 10, 10));
        }
        panelesBuffers = new PanelBuffers[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            panelesBuffers[i] = new PanelBuffers("Buffer " + buffers[i].getId()
                    + " (en Computador " + buffers[i].getIdAnfitrion()
                    + ", capacidad " + buffers[i].getCapacidad() + ")");
            panelBuffers.add(panelesBuffers[i]);
        }
        panelBuffers.revalidate();
        panelBuffers.repaint();
    }

    public PanelLog getPanelLog() {
        return panelLog;
    }

    public PanelComputador[] getPanelesPc() {
        return panelesPc;
    }
}
