package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.Disco;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Pestaña del disco: el mapa de bloques con el cabezal, el selector de política,
 * la cola de E/S y las métricas con el gráfico. Solo observa al disco.
 */
public class PanelDisco extends JPanel {

    private final PanelMapaDisco mapa = new PanelMapaDisco();
    private final PanelControlDisco control = new PanelControlDisco();
    private final PanelColaES cola = new PanelColaES();
    private final PanelMetricasDisco metricas = new PanelMetricasDisco();

    public PanelDisco() {
        super(new BorderLayout(6, 6));
        JPanel izquierda = new JPanel(new BorderLayout(0, 6));
        izquierda.add(control, BorderLayout.NORTH);
        izquierda.add(mapa, BorderLayout.CENTER);

        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cola, metricas);
        derecha.setResizeWeight(0.4);
        derecha.setBorder(null);

        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        principal.setResizeWeight(0.6);
        principal.setBorder(null);
        add(principal, BorderLayout.CENTER);
    }

    public void setControl(ControlDisco controlDisco) {
        control.setControl(controlDisco);
    }

    public void actualizar(Disco disco, long ciclo) {
        if (disco == null) {
            return;
        }
        mapa.actualizar(disco);
        control.actualizar(disco.getPlanificador());
        cola.actualizar(disco.getPlanificador(), ciclo);
        metricas.actualizar(disco.getPlanificador(), ciclo);
    }

    public PanelMetricasDisco getMetricas() {
        return metricas;
    }
}
