package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.metricas.Metricas;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/** Metricas de rendimiento de un computador, con el grafico de uso de CPU en el tiempo. */
public class PanelMetricas extends JPanel {

    /** Cuantos ciclos se ven a la vez en el grafico. */
    private static final int MAX_PUNTOS = 40;

    private final JLabel lblThroughput = new JLabel("Throughput: 0.00 proc/ciclo");
    private final JLabel lblUtilizacionCpu = new JLabel("Utilización CPU: 0.0%");
    private final JLabel lblTiempoRespuesta = new JLabel("Respuesta promedio: 0.0 ciclos");
    private final JLabel lblCumplimientoDeadlines = new JLabel("Deadlines cumplidos: 100.0%");
    private final JLabel lblEquidad = new JLabel("Equidad (Jain): 1.00");
    private final JLabel lblProducidosConsumidos = new JLabel("Producidos: 0 | Consumidos: 0");
    private final JLabel lblTiempoBloqueado = new JLabel("Bloqueo prom. en semáforos: 0.0 ciclos");

    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private long ultimoCiclo = -1;

    public PanelMetricas(String titulo) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder(titulo));

        JPanel panelEtiquetas = new JPanel(new GridLayout(4, 2, 10, 4));
        panelEtiquetas.add(lblThroughput);
        panelEtiquetas.add(lblUtilizacionCpu);
        panelEtiquetas.add(lblTiempoRespuesta);
        panelEtiquetas.add(lblCumplimientoDeadlines);
        panelEtiquetas.add(lblEquidad);
        panelEtiquetas.add(lblProducidosConsumidos);
        panelEtiquetas.add(lblTiempoBloqueado);
        add(panelEtiquetas, BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createLineChart(
                "Uso de CPU por ciclo",
                "Ciclo",
                "Utilización (%)",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        add(chartPanel, BorderLayout.CENTER);
    }

    /** Se llama desde el hilo de Swing (Timer del controlador) en cada refresco. */
    public void actualizar(Metricas m, long cicloActual) {
        lblThroughput.setText(String.format("Throughput: %.2f proc/ciclo", m.getThroughput()));
        lblUtilizacionCpu.setText(String.format("Utilización CPU: %.1f%%", m.getUtilizacionCpu()));
        lblTiempoRespuesta.setText(String.format("Respuesta promedio: %.1f ciclos", m.getTiempoRespuestaPromedio()));
        lblCumplimientoDeadlines.setText(String.format("Deadlines cumplidos: %.1f%%", m.getPorcentajeDeadlinesCumplidos()));
        lblEquidad.setText(String.format("Equidad (Jain): %.2f", m.getIndiceEquidad()));
        lblProducidosConsumidos.setText("Producidos: " + m.getElementosProducidos()
                + " | Consumidos: " + m.getElementosConsumidos());
        lblTiempoBloqueado.setText(String.format("Bloqueo prom. en semáforos: %.1f ciclos", m.getBloqueoPromedioSemaforo()));

        // Un punto por ciclo nuevo; se descartan los mas viejos para que el grafico no crezca sin limite
        if (cicloActual > 0 && cicloActual != ultimoCiclo) {
            ultimoCiclo = cicloActual;
            dataset.addValue(m.getUtilizacionCpu(), "Uso CPU (%)", Long.valueOf(cicloActual));
            while (dataset.getColumnCount() > MAX_PUNTOS) {
                dataset.removeColumn(0);
            }
        }
    }
}
