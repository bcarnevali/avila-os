package edu.unimet.avilaos.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PanelMetricas extends JPanel {

    // Tus etiquetas originales de métricas
    private final JLabel lblThroughput = new JLabel("Throughput: 0.0 proc/ciclo");
    private final JLabel lblUtilizacionCpu = new JLabel("Utilización CPU Promedio: 0%");
    private final JLabel lblTiempoRespuesta = new JLabel("Tiempo Respuesta Promedio: 0 ciclos");
    private final JLabel lblCumplimientoDeadlines = new JLabel("Cumplimiento Deadlines: 100%");
    private final JLabel lblEquidad = new JLabel("Índice de Equidad: 1.0");
    private final JLabel lblProducidosConsumidos = new JLabel("Producidos: 0 | Consumidos: 0");
    private final JLabel lblTiempoBloqueado = new JLabel("Tiempo Prom. Bloqueado en Semáforos: 0 ciclos");

    // Componentes del gráfico JFreeChart
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private final JFreeChart chart;

    public PanelMetricas() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Métricas de Rendimiento del Sistema"));

        // 1. Panel Superior / Izquierdo: Tus etiquetas numéricas
        JPanel panelEtiquetas = new JPanel(new GridLayout(4, 2, 10, 10));
        panelEtiquetas.add(lblThroughput);
        panelEtiquetas.add(lblUtilizacionCpu);
        panelEtiquetas.add(lblTiempoRespuesta);
        panelEtiquetas.add(lblCumplimientoDeadlines);
        panelEtiquetas.add(lblEquidad);
        panelEtiquetas.add(lblProducidosConsumidos);
        panelEtiquetas.add(lblTiempoBloqueado);

        add(panelEtiquetas, BorderLayout.NORTH);

        // 2. Gráfico JFreeChart solicitado por Bettina
        chart = ChartFactory.createLineChart(
                "Uso de CPU vs Ciclo de Reloj",
                "Ciclo",
                "Utilización (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 220));
        add(chartPanel, BorderLayout.CENTER);
    }

    // Tu método original para actualizar textos + actualización del gráfico
    public void actualizarMetricas(double throughput, double utilCpu, double tRespuesta, double pctDeadlines, double equidad, int prod, int cons, double tBloq, int cicloActual) {
        SwingUtilities.invokeLater(() -> {
            lblThroughput.setText(String.format("Throughput: %.2f proc/ciclo", throughput));
            lblUtilizacionCpu.setText(String.format("Utilización CPU Promedio: %.1f%%", utilCpu));
            lblTiempoRespuesta.setText(String.format("Tiempo Respuesta Promedio: %.1f ciclos", tRespuesta));
            lblCumplimientoDeadlines.setText(String.format("Cumplimiento Deadlines: %.1f%%", pctDeadlines));
            lblEquidad.setText(String.format("Índice de Equidad: %.2f", equidad));
            lblProducidosConsumidos.setText("Producidos: " + prod + " | Consumidos: " + cons);
            lblTiempoBloqueado.setText(String.format("Tiempo Prom. Bloqueado: %.1f ciclos", tBloq));

            // Actualizar punto en el gráfico
            dataset.addValue(utilCpu, "Uso CPU (%)", "C" + cicloActual);
        });
    }
}