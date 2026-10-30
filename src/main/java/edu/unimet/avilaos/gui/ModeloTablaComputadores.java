package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import javax.swing.table.AbstractTableModel;

/** Tabla editable de la configuracion: RAM y politica inicial de cada computador. */
public class ModeloTablaComputadores extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Computador", "RAM", "Política inicial"};

    private int[] ram = {256, 256};
    private TipoPolitica[] politicas = {TipoPolitica.FCFS, TipoPolitica.FCFS};

    public void setDatos(int[] ram, TipoPolitica[] politicas) {
        this.ram = ram.clone();
        this.politicas = politicas.clone();
        fireTableDataChanged();
    }

    /** Cambia la cantidad de filas; los computadores nuevos arrancan con 256 y FCFS. */
    public void setCantidad(int cantidad) {
        int[] nuevaRam = new int[cantidad];
        TipoPolitica[] nuevasPoliticas = new TipoPolitica[cantidad];
        for (int i = 0; i < cantidad; i++) {
            nuevaRam[i] = i < ram.length ? ram[i] : 256;
            nuevasPoliticas[i] = i < politicas.length ? politicas[i] : TipoPolitica.FCFS;
        }
        ram = nuevaRam;
        politicas = nuevasPoliticas;
        fireTableDataChanged();
    }

    public int[] getRam() { return ram.clone(); }
    public TipoPolitica[] getPoliticas() { return politicas.clone(); }

    @Override
    public int getRowCount() { return ram.length; }

    @Override
    public int getColumnCount() { return COLUMNAS.length; }

    @Override
    public String getColumnName(int columna) { return COLUMNAS[columna]; }

    @Override
    public Class<?> getColumnClass(int columna) {
        return columna == 2 ? TipoPolitica.class : Integer.class;
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return columna > 0;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        switch (columna) {
            case 0: return fila + 1;
            case 1: return ram[fila];
            default: return politicas[fila];
        }
    }

    /** JTable ya rechaza lo que no es entero; aqui se revisa el rango. */
    @Override
    public void setValueAt(Object valor, int fila, int columna) {
        if (columna == 1 && valor instanceof Integer) {
            int v = (Integer) valor;
            if (v >= 16 && v <= 100000) {
                ram[fila] = v;
            }
        } else if (columna == 2 && valor instanceof TipoPolitica) {
            politicas[fila] = (TipoPolitica) valor;
        }
        fireTableCellUpdated(fila, columna);
    }
}
