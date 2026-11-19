package edu.unimet.avilaos.configuracion;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * Lo que trae el JSON de procesos: la configuración global, la del computador y
 * la lista de procesos de E/S. Se arma con GestorJsonProcesos.
 */
public class ConfiguracionProcesos {

    private final int duracionCicloMs;
    private final int tamanoSwap;
    private final TipoPoliticaDisco politicaDisco;
    private final int ram;
    private final TipoPolitica politica;
    private final int quantum;
    private final ListaEnlazada<ProcesoJson> procesos;

    public ConfiguracionProcesos(int duracionCicloMs, int tamanoSwap, TipoPoliticaDisco politicaDisco,
                                 int ram, TipoPolitica politica, int quantum,
                                 ListaEnlazada<ProcesoJson> procesos) {
        this.duracionCicloMs = duracionCicloMs;
        this.tamanoSwap = tamanoSwap;
        this.politicaDisco = politicaDisco;
        this.ram = ram;
        this.politica = politica;
        this.quantum = quantum;
        this.procesos = procesos;
    }

    public int getDuracionCicloMs() { return duracionCicloMs; }
    public int getTamanoSwap() { return tamanoSwap; }
    public TipoPoliticaDisco getPoliticaDisco() { return politicaDisco; }
    public int getRam() { return ram; }
    public TipoPolitica getPolitica() { return politica; }
    public int getQuantum() { return quantum; }
    public ListaEnlazada<ProcesoJson> getProcesos() { return procesos; }
}
