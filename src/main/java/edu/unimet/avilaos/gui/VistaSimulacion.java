package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

public class VistaSimulacion extends JPanel {

    private final JPanel panelComputadores = new JPanel();
    private final PanelLog panelLog = new PanelLog();
    private PanelComputador[] panelesPc = new PanelComputador[0];

    public VistaSimulacion() {
        setLayout(new BorderLayout(5, 5));

        JScrollPane scrollComputadores = new JScrollPane(panelComputadores);
        panelLog.setPreferredSize(new Dimension(0, 180));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollComputadores, panelLog);
        splitPane.setResizeWeight(0.75);

        add(splitPane, BorderLayout.CENTER);
    }

    /** Carga dinámicamente los computadores creados en el Cluster */
    public void cargarCluster(Cluster cluster) {
        if (cluster == null) return;

        panelComputadores.removeAll();
        int total = cluster.getCantidad();
        panelComputadores.setLayout(new GridLayout(1, total, 10, 10));

        panelesPc = new PanelComputador[total];

        for (int i = 1; i <= total; i++) {
            Computador pc = cluster.getComputador(i);
            PanelComputador panelPc = new PanelComputador("Computador " + pc.getId());
            
            panelesPc[i - 1] = panelPc;
            panelComputadores.add(panelPc);
        }

        panelComputadores.revalidate();
        panelComputadores.repaint();
    }

    /** Refresca el estado visual de cada computador en cada ciclo de reloj */
    public void actualizarDatos(Cluster cluster) {
        if (cluster == null || panelesPc.length == 0) return;

        for (int i = 1; i <= cluster.getCantidad(); i++) {
            if (i - 1 < panelesPc.length) {
                Computador pc = cluster.getComputador(i);
                if (pc != null) {
                    panelesPc[i - 1].actualizarDatos(pc);
                }
            }
        }
    }

    public PanelLog getPanelLog() {
        return panelLog;
    }

    public PanelComputador[] getPanelesPc() {
        return panelesPc;
    }
    
}