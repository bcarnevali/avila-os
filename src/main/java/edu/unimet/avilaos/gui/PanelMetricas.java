package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.metricas.FuenteMetricas;
import edu.unimet.avilaos.metricas.Metricas;
import edu.unimet.avilaos.metricas.MetricasSistema;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Metricas de rendimiento: una tabla con cada computador y el sistema completo,
 * y un solo grafico con la utilizacion de la CPU de cada computador en el tiempo.
 */
public class PanelMetricas extends JPanel {

    /** Cuantos ciclos se ven a la vez en el grafico. */
    private static final int MAX_PUNTOS = 40;

    private final ModeloTablaMetricas modeloTabla = new ModeloTablaMetricas();
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private long ultimoCiclo = -1;

    public PanelMetricas() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Métricas de rendimiento"));

        JTable tabla = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(400, 95));
        add(scrollTabla, BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createLineChart(
                "Utilización de la CPU de cada computador",
                "Ciclo",
                "Utilización acumulada (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        add(chartPanel, BorderLayout.CENTER);
    }

    /** Vacia el grafico (al cargar un cluster nuevo). */
    public void reiniciar() {
        dataset.clear();
        ultimoCiclo = -1;
    }

    /** Se llama desde el hilo de Swing (Timer del controlador) en cada refresco. */
    public void actualizar(Cluster cluster) {
        int n = cluster.getCantidad();
        Metricas[] porComputador = new Metricas[n];
        String[] nombres = new String[n + 1];
        FuenteMetricas[] filas = new FuenteMetricas[n + 1];
        for (int i = 0; i < n; i++) {
            porComputador[i] = cluster.getComputador(i + 1).getMetricas();
            nombres[i] = "Computador " + (i + 1);
            filas[i] = porComputador[i];
        }
        nombres[n] = "Sistema completo";
        filas[n] = new MetricasSistema(porComputador);
        modeloTabla.setDatos(nombres, filas);

        // Un punto por ciclo nuevo, una linea por computador; se descartan los ciclos mas viejos
        long ciclo = cluster.getReloj().getCicloActual();
        if (ciclo > 0 && ciclo != ultimoCiclo) {
            ultimoCiclo = ciclo;
            for (int i = 0; i < n; i++) {
                dataset.addValue(porComputador[i].getUtilizacionCpu(), nombres[i], Long.valueOf(ciclo));
            }
            while (dataset.getColumnCount() > MAX_PUNTOS) {
                dataset.removeColumn(0);
            }
        }
    }
}
