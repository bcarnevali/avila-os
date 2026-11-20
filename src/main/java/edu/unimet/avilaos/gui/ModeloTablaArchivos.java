package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.archivos.Archivo;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import java.awt.Color;
import javax.swing.table.AbstractTableModel;

public class ModeloTablaArchivos extends AbstractTableModel {

    private static final String[] COLUMNAS = {
        "Nombre", "Bloques", "Primer bloque", "Dueño", "Proceso creador", "Color"
    };

    private ListaEnlazada<Archivo> archivos = new ListaEnlazada<>();

    public void actualizar(ListaEnlazada<Archivo> nuevos) {
        this.archivos = (nuevos != null) ? nuevos : new ListaEnlazada<>();
        fireTableDataChanged();
    }

    public Archivo getArchivoEn(int fila) {
        if (fila < 0 || fila >= archivos.tamano()) {
            return null;
        }
        return archivos.obtener(fila);
    }

    @Override
    public int getRowCount() {
        return archivos.tamano();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNAS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Archivo a = archivos.obtener(rowIndex);
        switch (columnIndex) {
            case 0: return a.getNombre();
            case 1: return a.getTamanoBloques();
            case 2: return a.getPrimerBloque();
            case 3: return a.getDueno();
            case 4: return a.getProcesoCreador();
            case 5:
                Color c = ColoresArchivos.colorDe(a.getRuta());
                return ColoresArchivos.hex(c);
            default: return "";
        }
    }
}