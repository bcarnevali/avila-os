package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.modelo.enums.TipoPermiso;

public class Archivo extends EntradaSistemaArchivos {

    private final int primerBloque;
    private final int bloques;
    private final String procesoCreador;

    public Archivo(String nombre, String dueno, TipoPermiso permiso, Directorio padre,
                   int primerBloque, int bloques, String procesoCreador) {
        super(nombre, dueno, permiso, padre);
        this.primerBloque = primerBloque;
        this.bloques = bloques;
        this.procesoCreador = procesoCreador;
    }

    @Override
    public int getTamanoBloques() {
        return bloques;
    }

    public int getPrimerBloque() { return primerBloque; }
    public int getBloques() { return bloques; }
    public String getProcesoCreador() { return procesoCreador; }
}