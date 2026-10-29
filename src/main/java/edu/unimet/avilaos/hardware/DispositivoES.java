/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.hardware;

import edu.unimet.avilaos.estructuras.Cola;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;

public class DispositivoES implements ComponenteReloj {
    private final Cola<PCB> enEspera = new Cola<>();
    private final Cola<PCB> terminados = new Cola<>();
    private PCB enAtencion;
    private int restante;

    public synchronized void solicitarES(PCB pcb) {
        enEspera.encolar(pcb);
    }

    @Override
    public synchronized void tick(long ciclo) {
        if (enAtencion == null && !enEspera.estaVacia()) {
            enAtencion = enEspera.desencolar();
            restante = enAtencion.getCiclosParaSatisfacerES();
        }

        if (enAtencion == null) {
            return;
        }

        // Se le venció el deadline mientras esperaba/atendía E/S
        if (enAtencion.getEstado() == EstadoProceso.TERMINADO) {
            enAtencion = null;
            return;
        }

        restante--;

        if (restante <= 0) {
            terminados.encolar(enAtencion);
            enAtencion = null;
        }
    }

    /** Devuelve un proceso que terminó su E/S, o null si no hay. */
    public synchronized PCB retirarTerminado() {
        return terminados.desencolar();
    }

    public synchronized PCB getEnAtencion() {
        return enAtencion;
    }

    public synchronized Cola<PCB> getEnEspera() {
        return enEspera;
    }
}