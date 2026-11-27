package edu.unimet.avilaos.configuracion;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoProceso;

/** Un proceso de E/S leído del JSON de procesos, ya validado. */
public class ProcesoJson {

    private final String nombre;
    private final int memoria;
    private final int deadline;
    private final int instrucciones;
    private final int prioridad;
    private final String ruta;
    private final OperacionES operacion;
    private final int bloques;

    public ProcesoJson(String nombre, int memoria, int deadline, int instrucciones, int prioridad,
                       String ruta, OperacionES operacion, int bloques) {
        this.nombre = nombre;
        this.memoria = memoria;
        this.deadline = deadline;
        this.instrucciones = instrucciones;
        this.prioridad = prioridad;
        this.ruta = ruta;
        this.operacion = operacion;
        this.bloques = bloques;
    }

    /** Crea el PCB del proceso de E/S para el computador indicado. */
    public PCB crearPCB(int idComputador, long ciclo) {
        PCB pcb = new PCB(nombre, TipoProceso.ES_ARCHIVO, prioridad, memoria, instrucciones,
                deadline, idComputador, ciclo);
        pcb.configurarArchivo(operacion, ruta, bloques);
        return pcb;
    }

    public String getNombre() { return nombre; }
    public int getMemoria() { return memoria; }
    public int getDeadline() { return deadline; }
    public int getInstrucciones() { return instrucciones; }
    public int getPrioridad() { return prioridad; }
    public String getRuta() { return ruta; }
    public OperacionES getOperacion() { return operacion; }
    public int getBloques() { return bloques; }

    @Override
    public String toString() {
        return nombre + " " + operacion + " " + ruta;
    }
}
