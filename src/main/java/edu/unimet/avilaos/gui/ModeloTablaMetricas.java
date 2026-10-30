package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.metricas.FuenteMetricas;
import javax.swing.table.AbstractTableModel;

/** Una fila por computador y una ultima fila con el sistema completo. Sin DefaultTableModel (usa Vector). */
public class ModeloTablaMetricas extends AbstractTableModel {

    private static final String[] COLUMNAS = {"", "Throughput (proc/ciclo)", "Utilización CPU (%)",
        "Respuesta prom. (ciclos)", "Deadlines cumplidos (%)", "Equidad (Jain)",
        "Producidos", "Consumidos", "Bloqueo prom. semáforos (ciclos)"};

    private String[] nombres = new String[0];
    private FuenteMetricas[] filas = new FuenteMetricas[0];

    public void setDatos(String[] nombres, FuenteMetricas[] filas) {
        this.nombres = nombres;
        this.filas = filas;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return filas.length;
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        FuenteMetricas m = filas[fila];
        switch (columna) {
            case 0: return nombres[fila];
            case 1: return String.format("%.3f", m.getThroughput());
            case 2: return String.format("%.1f", m.getUtilizacionCpu());
            case 3: return String.format("%.2f", m.getTiempoRespuestaPromedio());
            case 4: return String.format("%.1f", m.getPorcentajeDeadlinesCumplidos());
            case 5: return String.format("%.2f", m.getIndiceEquidad());
            case 6: return m.getElementosProducidos();
            case 7: return m.getElementosConsumidos();
            default: return String.format("%.2f", m.getBloqueoPromedioSemaforo());
        }
    }
}
