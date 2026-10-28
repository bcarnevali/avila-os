package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import javax.swing.table.AbstractTableModel;

/** Modelo propio de tabla para mostrar PCB (sin DefaultTableModel, que usa Vector). */
public class ModeloTablaPCB extends AbstractTableModel {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Tipo", "Estado",
        "Prioridad", "Deadline", "PC", "Memoria", "Motivo bloqueo"};

    private PCB[] datos = new PCB[0];

    public void setDatos(PCB[] nuevos) {
        datos = (nuevos == null) ? new PCB[0] : nuevos;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return datos.length;
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
        PCB p = datos[fila];
        switch (columna) {
            case 0: return p.toString();
            case 1: return p.getNombre();
            case 2: return p.getTipo();
            case 3: return p.getEstado();
            case 4: return p.getPrioridad();
            case 5: return p.getDeadlineRestante();
            case 6: return p.getPc();
            case 7: return p.getMemoriaRequerida();
            default: return p.getMotivoBloqueo();
        }
    }
}