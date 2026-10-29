package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoProceso;

public class SolicitudProceso {

    public static final int AUTOMATICO = -1;

    private final String nombre;
    private final TipoProceso tipo;
    private final int prioridad;
    private final int idComputador;
    private final int memoria;
    private final int instrucciones;
    private final int deadline;
    private final String idBuffer;
    private final int ciclosPorElemento;
    private final int elementosRequeridos;
    private final int ciclosEntreES;
    private final int ciclosParaSatisfacerES;

    // Constructor 1: Orden de ControladorSimulacion (idBuffer en posicion 8)
    public SolicitudProceso(String nombre, TipoProceso tipo, int prioridad, int idComputador, 
                            int memoria, int instrucciones, int deadline, String idBuffer, 
                            int ciclosPorElemento, int elementosRequeridos, 
                            int ciclosEntreES, int ciclosParaSatisfacerES) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.idComputador = idComputador;
        this.memoria = memoria;
        this.instrucciones = instrucciones;
        this.deadline = deadline;
        this.idBuffer = idBuffer;
        this.ciclosPorElemento = ciclosPorElemento;
        this.elementosRequeridos = elementosRequeridos;
        this.ciclosEntreES = ciclosEntreES;
        this.ciclosParaSatisfacerES = ciclosParaSatisfacerES;
    }

    // Constructor 2: Orden de VistaCrearProceso (idBuffer en posicion 10)
    public SolicitudProceso(String nombre, TipoProceso tipo, int prioridad, int idComputador, 
                            int memoria, int instrucciones, int deadline, int ciclosPorElemento, 
                            int elementosRequeridos, String idBuffer, 
                            int ciclosEntreES, int ciclosParaSatisfacerES) {
        this(nombre, tipo, prioridad, idComputador, memoria, instrucciones, deadline, 
             idBuffer, ciclosPorElemento, elementosRequeridos, ciclosEntreES, ciclosParaSatisfacerES);
    }

    // Getters tradicionales
    public String getNombre() { return nombre; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getIdComputador() { return idComputador; }
    public int getMemoria() { return memoria; }
    public int getInstrucciones() { return instrucciones; }
    public int getDeadline() { return deadline; }
    public String getIdBuffer() { return idBuffer; }
    public int getCiclosPorElemento() { return ciclosPorElemento; }
    public int getElementosRequeridos() { return elementosRequeridos; }
    public int getCiclosEntreES() { return ciclosEntreES; }
    public int getCiclosParaSatisfacerES() { return ciclosParaSatisfacerES; }

    // Métodos estilo record requeridos por ControladorSimulacion
    public String nombre() { return nombre; }
    public TipoProceso tipo() { return tipo; }
    public int prioridad() { return prioridad; }
    public int idComputador() { return idComputador; }
    public int memoria() { return memoria; }
    public int instrucciones() { return instrucciones; }
    public int deadline() { return deadline; }
    public String idBuffer() { return idBuffer; }
    public int ciclosPorElemento() { return ciclosPorElemento; }
    public int elementosRequeridos() { return elementosRequeridos; }
    public int ciclosEntreES() { return ciclosEntreES; }
    public int ciclosParaSatisfacerES() { return ciclosParaSatisfacerES; }

    public PCB aPCB(long cicloLlegada) {
        return new PCB(
            nombre,
            tipo,
            prioridad,
            idComputador,
            memoria,
            instrucciones,
            deadline,
            cicloLlegada
        );
    }
}