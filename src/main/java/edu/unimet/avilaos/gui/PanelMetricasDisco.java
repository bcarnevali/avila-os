package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.PlanificadorDisco;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Métricas del disco y de la suspensión, y el gráfico del desplazamiento acumulado
 * del cabezal en el tiempo. Cada tramo del gráfico tiene el color de la política
 * que estaba activa, para poder comparar las pendientes de las políticas.
 */
public class PanelMetricasDisco extends JPanel {

    private static final Color[] COLORES = {
        new Color(31, 119, 180), new Color(44, 160, 44), new Color(255, 127, 14), new Color(214, 39, 40)
    };

    private final JLabel lblDesplazamiento = new JLabel();
    private final JLabel lblEspera = new JLabel();
    private final JLabel lblAtendidas = new JLabel();
    private final JLabel lblPorPolitica = new JLabel();
    private final JLabel lblSuspension = new JLabel();
    private final XYSeries[] series = new XYSeries[TipoPoliticaDisco.values().length];
    private long ultimoCiclo = -1;
    private TipoPoliticaDisco ultimaPolitica;

    public PanelMetricasDisco() {
        super(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Métricas del disco"));

        JPanel numeros = new JPanel(new GridLayout(0, 1, 0, 2));
        numeros.add(lblDesplazamiento);
        numeros.add(lblEspera);
        numeros.add(lblAtendidas);
        numeros.add(lblPorPolitica);
        numeros.add(lblSuspension);
        add(numeros, BorderLayout.NORTH);

        XYSeriesCollection datos = new XYSeriesCollection();
        for (TipoPoliticaDisco p : TipoPoliticaDisco.values()) {
            series[p.ordinal()] = new XYSeries(PanelControlDisco.nombre(p), false, true);
            datos.addSeries(series[p.ordinal()]);
        }
        JFreeChart grafico = ChartFactory.createXYLineChart("Desplazamiento acumulado del cabezal",
                "Ciclo", "Bloques recorridos", datos, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = grafico.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(225, 225, 225));
        plot.setRangeGridlinePaint(new Color(225, 225, 225));
        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < COLORES.length; i++) {
            r.setSeriesPaint(i, COLORES[i]);
            r.setSeriesStroke(i, new BasicStroke(2.2f));
        }
        plot.setRenderer(r);
        ChartPanel panelGrafico = new ChartPanel(grafico);
        panelGrafico.setPreferredSize(new Dimension(420, 260));
        add(panelGrafico, BorderLayout.CENTER);

        setSuspensiones(0, 0);
    }

    public void actualizar(PlanificadorDisco plan, long ciclo) {
        long desplazamiento = plan.getDesplazamientoTotal();
        lblDesplazamiento.setText("Desplazamiento total del cabezal: " + desplazamiento + " bloques");
        lblEspera.setText(String.format("Espera promedio en la cola de E/S: %.2f ciclos", plan.getEsperaPromedio()));
        lblAtendidas.setText("Solicitudes atendidas: " + plan.getAtendidas());
        StringBuilder sb = new StringBuilder("Atendidas por política: ");
        for (TipoPoliticaDisco p : TipoPoliticaDisco.values()) {
            sb.append(PanelControlDisco.nombre(p)).append(' ').append(plan.getAtendidasCon(p))
                    .append(p.ordinal() < TipoPoliticaDisco.values().length - 1 ? "  ·  " : "");
        }
        lblPorPolitica.setText(sb.toString());

        if (ciclo != ultimoCiclo) {
            TipoPoliticaDisco politica = plan.getPolitica();
            if (ultimaPolitica != null && politica != ultimaPolitica && ultimoCiclo >= 0) {
                // une los tramos: el nuevo empieza donde terminó el anterior
                XYSeries anterior = series[ultimaPolitica.ordinal()];
                if (anterior.getItemCount() > 0) {
                    series[politica.ordinal()].addOrUpdate(anterior.getX(anterior.getItemCount() - 1),
                            anterior.getY(anterior.getItemCount() - 1));
                }
            }
            series[politica.ordinal()].addOrUpdate(ciclo, desplazamiento);
            ultimoCiclo = ciclo;
            ultimaPolitica = politica;
        }
    }

    /** Lo llena el planificador a mediano plazo. */
    public void setSuspensiones(int suspensiones, int reactivaciones) {
        lblSuspension.setText("Suspensiones: " + suspensiones + "  ·  Reactivaciones: " + reactivaciones);
    }

    /** Para una simulación nueva. */
    public void reiniciar() {
        for (XYSeries s : series) {
            s.clear();
        }
        ultimoCiclo = -1;
        ultimaPolitica = null;
    }
}
