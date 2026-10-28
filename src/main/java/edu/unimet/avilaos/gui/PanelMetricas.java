package edu.unimet.avilaos.gui;

import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PanelMetricas extends JPanel {

    private final JLabel lblThroughput = new JLabel("Throughput: 0.0 proc/ciclo");
    private final JLabel lblUtilizacionCpu = new JLabel("Utilización CPU Promedio: 0%");
    private final JLabel lblTiempoRespuesta = new JLabel("Tiempo Respuesta Promedio: 0 ciclos");
    private final JLabel lblCumplimientoDeadlines = new JLabel("Cumplimiento Deadlines: 100%");
    private final JLabel lblEquidad = new JLabel("Índice de Equidad: 1.0");
    private final JLabel lblProducidosConsumidos = new JLabel("Producidos: 0 | Consumidos: 0");
    private final JLabel lblTiempoBloqueado = new JLabel("Tiempo Prom. Bloqueado en Semáforos: 0 ciclos");

    public PanelMetricas() {
        setLayout(new GridLayout(4, 2, 10, 10));
        setBorder(BorderFactory.createTitledBorder("Métricas de Rendimiento del Sistema"));

        add(lblThroughput);
        add(lblUtilizacionCpu);
        add(lblTiempoRespuesta);
        add(lblCumplimientoDeadlines);
        add(lblEquidad);
        add(lblProducidosConsumidos);
        add(lblTiempoBloqueado);
    }

    public void actualizarMetricas(double throughput, double utilCpu, double tRespuesta, double pctDeadlines, double equidad, int prod, int cons, double tBloq) {
        SwingUtilities.invokeLater(() -> {
            lblThroughput.setText(String.format("Throughput: %.2f proc/ciclo", throughput));
            lblUtilizacionCpu.setText(String.format("Utilización CPU Promedio: %.1f%%", utilCpu));
            lblTiempoRespuesta.setText(String.format("Tiempo Respuesta Promedio: %.1f ciclos", tRespuesta));
            lblCumplimientoDeadlines.setText(String.format("Cumplimiento Deadlines: %.1f%%", pctDeadlines));
            lblEquidad.setText(String.format("Índice de Equidad: %.2f", equidad));
            lblProducidosConsumidos.setText("Producidos: " + prod + " | Consumidos: " + cons);
            lblTiempoBloqueado.setText(String.format("Tiempo Prom. Bloqueado: %.1f ciclos", tBloq));
        });
    }
}