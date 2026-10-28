/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.hardware;

import edu.unimet.avilaos.estructuras.Cola;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.MotivoBloqueo;

public class DispositivoES implements ComponenteReloj {

    private final String nombre;
    private final Cola<PCB> colaBloqueados = new Cola<>();
    private PCB procesoEnAtencion = null;
    private int tiempoRestanteES = 0;

    public DispositivoES(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Encola un proceso que requiere hacer una operación de E/S.
     * Cambia el estado del proceso a BLOQUEADO por motivo ES.
     */
    public synchronized void solicitarES(PCB pcb, int duracionCiclos) {
        if (pcb == null) return;

        pcb.setEstado(EstadoProceso.BLOQUEADO);
        pcb.setMotivoBloqueo(MotivoBloqueo.ES);
        
        this.tiempoRestanteES = duracionCiclos;
        colaBloqueados.encolar(pcb);

        // Si el dispositivo está libre, inicia de inmediato
        if (procesoEnAtencion == null) {
            atenderSiguiente();
        }
    }

    private void atenderSiguiente() {
        if (!colaBloqueados.estaVacia()) {
            procesoEnAtencion = colaBloqueados.desencolar();
        }
    }

    /**
     * Avance en cada pulso de reloj: descuenta el tiempo de la E/S en curso.
     */
    @Override
    public synchronized void tick(long ciclo) {
        if (procesoEnAtencion == null) {
            atenderSiguiente();
        }

        if (procesoEnAtencion != null) {
            tiempoRestanteES--;

            // Cuando la E/S finaliza, desbloqueamos el proceso
            if (tiempoRestanteES <= 0) {
                procesoEnAtencion.setMotivoBloqueo(null);
                procesoEnAtencion.setEstado(EstadoProceso.LISTO);
                procesoEnAtencion = null;

                // Prepara el siguiente proceso en cola si lo hay
                atenderSiguiente();
            }
        }
    }

    public String getNombre() {
        return nombre;
    }

    public synchronized PCB getProcesoEnAtencion() {
        return procesoEnAtencion;
    }

    public synchronized Cola<PCB> getColaBloqueados() {
        return colaBloqueados;
    }

    public synchronized boolean estaOcupado() {
        return procesoEnAtencion != null;
    }
}