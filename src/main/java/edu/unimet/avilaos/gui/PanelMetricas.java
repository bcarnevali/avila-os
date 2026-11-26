package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.metricas.FuenteMetricas;
import edu.unimet.avilaos.metricas.Metricas;
import edu.unimet.avilaos.metricas.MetricasSistema;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class PanelMetricas extends JPanel {

    private static final int MAX_PUNTOS = 40;

    private final ModeloTablaMetricas modeloTabla = new ModeloTablaMetricas();
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private final JLabel lblSuspensiones = new JLabel("Planificador de mediano plazo — Suspensiones: 0  |  Reactivaciones: 0");
    private long ultimoCiclo = -1;

    public PanelMetricas() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Métricas de rendimiento de CPU y memoria"));

        JPanel panelTop = new JPanel(new BorderLayout(5, 5));

        JTable tabla = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(400, 95));
        panelTop.add(scrollTabla, BorderLayout.CENTER);

        JPanel barraSusp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraSusp.setBorder(BorderFactory.createEtchedBorder());
        lblSuspensiones.setFont(lblSuspensiones.getFont().deriveFont(Font.BOLD));
        barraSusp.add(lblSuspensiones);
        panelTop.add(barraSusp, BorderLayout.SOUTH);

        add(panelTop, BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createLineChart(
                "Utilización de la CPU en el tiempo",
                "Ciclo",
                "Utilización acumulada (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 220));
        add(chartPanel, BorderLayout.CENTER);
    }

    public void setSuspensiones(int suspensiones, int reactivaciones) {
        lblSuspensiones.setText("Planificador de mediano plazo — Suspensiones: " + suspensiones
                + "  |  Reactivaciones: " + reactivaciones);
    }

    public void reiniciar() {
        dataset.clear();
        ultimoCiclo = -1;
        setSuspensiones(0, 0);
    }

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