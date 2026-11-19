package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;

public class Directorio extends EntradaSistemaArchivos {

    private final ListaEnlazada<EntradaSistemaArchivos> hijos;

    public Directorio(String nombre, String dueno, TipoPermiso permiso, Directorio padre) {
        super(nombre, dueno, permiso, padre);
        this.hijos = new ListaEnlazada<>();
    }

    public void agregarHijo(EntradaSistemaArchivos hijo) {
        hijo.setPadre(this);
        hijos.agregar(hijo);
    }

    public void eliminarHijo(EntradaSistemaArchivos hijo) {
        hijos.eliminar(hijo);
    }

    public EntradaSistemaArchivos buscarHijo(String nombreBuscado) {
        for (int i = 0; i < hijos.tamano(); i++) {
            EntradaSistemaArchivos actual = hijos.obtener(i);
            if (actual.getNombre().equals(nombreBuscado)) {
                return actual;
            }
        }
        return null;
    }

    @Override
    public int getTamanoBloques() {
        int total = 0;
        for (int i = 0; i < hijos.tamano(); i++) {
            total += hijos.obtener(i).getTamanoBloques();
        }
        return total;
    }

    public ListaEnlazada<EntradaSistemaArchivos> getHijos() {
        return hijos;
    }
}