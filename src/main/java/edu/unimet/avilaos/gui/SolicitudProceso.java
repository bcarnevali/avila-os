package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoProceso;

public class SolicitudProceso {

    public static final int AUTOMATICO = -1;

    public final String nombre;
    public final TipoProceso tipo;
    public final int prioridad;
    public final int memoria;
    public final int instrucciones;
    public final int deadline;
    public final int idComputador;
    public final int ciclosEntreES;
    public final int ciclosES;
    public final String buffer;
    public final int ciclosPorElemento;
    public final int elementos;

    public SolicitudProceso(String nombre, TipoProceso tipo, int prioridad, int memoria,
                            int instrucciones, int deadline, int idComputador,
                            int ciclosEntreES, int ciclosES, String buffer,
                            int ciclosPorElemento, int elementos) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instrucciones = instrucciones;
        this.deadline = deadline;
        this.idComputador = idComputador;
        this.ciclosEntreES = ciclosEntreES;
        this.ciclosES = ciclosES;
        this.buffer = buffer;
        this.ciclosPorElemento = ciclosPorElemento;
        this.elementos = elementos;
    }

    public PCB aPCB(long cicloActual) {
        int compId = (this.idComputador == AUTOMATICO || this.idComputador <= 0) ? 1 : this.idComputador;

        PCB pcb = new PCB(
            this.nombre,
            this.tipo,
            this.prioridad,
            this.memoria,
            this.instrucciones,
            this.deadline,
            compId,
            cicloActual
        );

        if (this.ciclosEntreES > 0 || this.ciclosES > 0) {
            pcb.configurarES(this.ciclosEntreES, this.ciclosES);
        }

        return pcb;
    }
}