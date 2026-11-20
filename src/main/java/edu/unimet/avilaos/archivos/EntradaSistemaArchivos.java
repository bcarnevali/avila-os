package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.modelo.enums.TipoPermiso;

public abstract class EntradaSistemaArchivos {

    protected String nombre;
    protected String dueno;
    protected TipoPermiso permiso;
    protected Directorio padre;

    public EntradaSistemaArchivos(String nombre, String dueno, TipoPermiso permiso, Directorio padre) {
        this.nombre = nombre;
        this.dueno = dueno;
        this.permiso = permiso;
        this.padre = padre;
    }

    public String getRuta() {
        if (padre == null) {
            return "/";
        }
        String rutaPadre = padre.getRuta();
        if ("/".equals(rutaPadre)) {
            return "/" + nombre;
        }
        return rutaPadre + "/" + nombre;
    }

    public abstract int getTamanoBloques();

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDueno() { return dueno; }
    public void setDueno(String dueno) { this.dueno = dueno; }

    public TipoPermiso getPermiso() { return permiso; }
    public void setPermiso(TipoPermiso permiso) { this.permiso = permiso; }

    public Directorio getPadre() { return padre; }
    public void setPadre(Directorio padre) { this.padre = padre; }
}