package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/** Pantalla principal organizada en 5 pestañas a pantalla completa. */
public class VistaSimulacion extends JPanel {

    private final JPanel panelProcesos = new JPanel(new BorderLayout());
    private final PanelDisco panelDisco = new PanelDisco();
    private final PanelSistemaArchivos panelArchivos = new PanelSistemaArchivos();
    private final PanelMetricas panelMetricas = new PanelMetricas();
    private final PanelLog panelLog = new PanelLog();

    private PanelComputador[] panelesPc = new PanelComputador[0];

    public VistaSimulacion() {
        setLayout(new BorderLayout(4, 4));

        JLabel avisoInicial = new JLabel(
                "<html><center><h2>Bienvenido a ÁvilaOS</h2>"
                + "<p>Haz clic en <b>1. Cargar JSON</b> en la barra superior para seleccionar "
                + "el <b>JSON de procesos</b> y el <b>JSON de archivos</b> antes de iniciar.</p></center></html>",
                SwingConstants.CENTER);
        panelProcesos.add(avisoInicial, BorderLayout.CENTER);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Procesos", panelProcesos);
        pestanas.addTab("Disco", panelDisco);
        pestanas.addTab("Archivos", panelArchivos);
        pestanas.addTab("Métricas", panelMetricas);
        pestanas.addTab("Log", panelLog);

        add(pestanas, BorderLayout.CENTER);
    }

    /** Crea el panel del computador en la pestaña Procesos y reinicia las métricas. */
    public void cargarCluster(Cluster cluster) {
        if (cluster == null) return;
        int total = cluster.getCantidad();

        panelProcesos.removeAll();
        panelProcesos.setLayout(new GridLayout(1, total, 8, 8));
        panelMetricas.reiniciar();

        panelesPc = new PanelComputador[total];
        for (int i = 1; i <= total; i++) {
            Computador pc = cluster.getComputador(i);
            panelesPc[i - 1] = new PanelComputador("Computador " + pc.getId());
            panelesPc[i - 1].asociarComputador(pc);
            panelProcesos.add(panelesPc[i - 1]);
        }

        panelDisco.getMetricas().reiniciar();
        revalidate();
        repaint();
    }

    /** Refresca todo lo visible desde el Timer del controlador. */
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
        int susp = pc.getMedianoPlazo().getSuspensiones();
        int react = pc.getMedianoPlazo().getReactivaciones();
        panelDisco.getMetricas().setSuspensiones(susp, react);
        panelMetricas.setSuspensiones(susp, react);
        panelArchivos.refrescar();
    }

    public void actualizarPermisosVisuales(boolean esAdmin, String usuarioActual) {
        for (PanelComputador pc : panelesPc) {
            pc.actualizarPermisosVisuales(esAdmin);
        }
        panelDisco.actualizarPermisosVisuales(esAdmin);
        panelArchivos.sincronizarModoGlobal(esAdmin, usuarioActual);
    }

    public PanelDisco getPanelDisco() { return panelDisco; }
    public PanelSistemaArchivos getPanelArchivos() { return panelArchivos; }
    public PanelMetricas getPanelMetricas() { return panelMetricas; }
    public PanelLog getPanelLog() { return panelLog; }
    public PanelComputador[] getPanelesPc() { return panelesPc; }
}