/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.cluster;

import edu.unimet.avilaos.hardware.ComponenteReloj;
import edu.unimet.avilaos.hardware.CPU;
import edu.unimet.avilaos.hardware.DispositivoES;
import edu.unimet.avilaos.hardware.MemoriaPrincipal;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.kernel.Planificador;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.metricas.Metricas;
import edu.unimet.avilaos.modelo.enums.ModoEjecucion;
import edu.unimet.avilaos.modelo.enums.MotivoBloqueo;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

public class Computador implements ComponenteReloj {

    private final int id;
    private final CPU cpu = new CPU();
    private final MemoriaPrincipal memoria;
    private final Metricas metricas = new Metricas();
    private final Planificador planificador;
    private final DispositivoES dispositivoES = new DispositivoES();

    public Computador(int id, int tamanoRam, TipoPolitica politica, int quantum, RegistroEventos log) {
        this.id = id;
        this.memoria = new MemoriaPrincipal(tamanoRam);
        this.planificador = new Planificador(id, memoria, metricas, politica, quantum, log);
    }

    /** Un ciclo de reloj: coordinamos la CPU y el Planificador */
    @Override
    public void tick(long ciclo) {
    // 1. AL PRINCIPIO DEL TICK: Avanza E/S y desbloquea procesos que finalizaron E/S
        dispositivoES.tick(ciclo);
        PCB terminoES;
        while ((terminoES = dispositivoES.retirarTerminado()) != null) {
            planificador.desbloquear(terminoES, ciclo);
    }

    // 2. EJECUCIÓN Y PLANIFICACIÓN (Lógica original de tu clase)
        PCB actual = cpu.getEnEjecucion();

        if (actual != null) {
            if (actual.haTerminado()) {
            // Liberación del proceso que terminó su ejecución
                cpu.setEnEjecucion(null);
                return; // Detiene el flujo para este proceso
        }   

        // 3. AL FINAL DEL TICK: Solicitar E/S si le corresponde
            if (actual.leTocaES() && actual.getCiclosParaSatisfacerES() > 0) {
                planificador.bloquear(actual, MotivoBloqueo.ES, ciclo);
                dispositivoES.solicitarES(actual);
                cpu.setEnEjecucion(null);
        }
    }
}

    public void crearProceso(PCB pcb, long ciclo) {
        planificador.crearProceso(pcb, ciclo);
    }

    public int getId() { return id; }
    public CPU getCpu() { return cpu; }
    public MemoriaPrincipal getMemoria() { return memoria; }
    public Metricas getMetricas() { return metricas; }
    public Planificador getPlanificador() { return planificador; }
    public DispositivoES getDispositivoES() { return dispositivoES; }
}