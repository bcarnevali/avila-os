package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.PlanificadorDisco;
import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import javax.swing.table.AbstractTableModel;

/**
 * Tabla de la cola de E/S del disco. La primera fila es la solicitud que el disco
 * está atendiendo (si hay); las demás, las que esperan en orden de llegada.
 */
public class ModeloTablaColaES extends AbstractTableModel {

    private static final String[] COLUMNAS = {"", "Proceso", "Operación", "Bloque", "Llegó (ciclo)", "Espera"};

    private SolicitudES actual;
    private SolicitudES[] filas = new SolicitudES[0];
    private long ciclo;

    public void actualizar(PlanificadorDisco plan, long cicloActual) {
        actual = plan.getActual();
        ListaEnlazada<SolicitudES> cola = plan.getCola();
        int n = cola.tamano() + (actual == null ? 0 : 1);
        SolicitudES[] nuevas = new SolicitudES[n];
        int i = 0;
        if (actual != null) {
            nuevas[i++] = actual;
        }
        for (int j = 0; j < cola.tamano(); j++) {
            nuevas[i++] = cola.obtener(j);
        }
        filas = nuevas;
        ciclo = cicloActual;
        fireTableDataChanged();
    }

    public boolean esEnCurso(int fila) {
        return actual != null && fila == 0;
    }

    @Override public int getRowCount() { return filas.length; }
    @Override public int getColumnCount() { return COLUMNAS.length; }
    @Override public String getColumnName(int c) { return COLUMNAS[c]; }

    @Override
    public Object getValueAt(int fila, int col) {
        SolicitudES s = filas[fila];
        switch (col) {
            case 0: return esEnCurso(fila) ? "En curso" : String.valueOf(actual == null ? fila + 1 : fila);
            case 1: return s.getSolicitante();
            case 2: return s.getOperacion();
            case 3: return s.getBloque();
            case 4: return s.getCicloLlegada();
            case 5:
                long inicio = s.getCicloInicio() >= 0 ? s.getCicloInicio() : ciclo;
                return Math.max(0, inicio - s.getCicloLlegada()) + " ciclos";
            default: return "";
        }
    }
}
