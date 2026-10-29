/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.cluster;

import edu.unimet.avilaos.hardware.CPU;
import edu.unimet.avilaos.hardware.ComponenteReloj;
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

    /** Un ciclo de reloj: coordina la E/S, la CPU y el Planificador. */
    @Override
    public void tick(long ciclo) {
        // 1. Avanza la E/S y devuelve a listos los procesos que ya terminaron su E/S
        dispositivoES.tick(ciclo);
        PCB terminoES;
        while ((terminoES = dispositivoES.retirarTerminado()) != null) {
            planificador.desbloquear(terminoES, ciclo);
        }

        // 2. Deadlines y planificacion (modo sistema operativo)
        PCB actual = cpu.getEnEjecucion();
        cpu.setModo(ModoEjecucion.SISTEMA_OPERATIVO);

        if (planificador.descontarDeadlines(actual, ciclo)) {
            actual = null; // Se vencio el deadline del proceso en la CPU
        }

        actual = planificador.planificar(actual, ciclo);
        cpu.setEnEjecucion(actual);
        metricas.registrarCiclo(actual != null);

        if (actual == null) return; // CPU ociosa en este ciclo

        // 3. Ejecuta una instruccion (modo usuario)
        cpu.setModo(ModoEjecucion.USUARIO);
        cpu.ejecutarInstruccion();

        if (actual.haTerminado()) {
            planificador.terminar(actual, ciclo);
            cpu.setEnEjecucion(null);
        } else if (actual.leTocaES() && actual.getCiclosParaSatisfacerES() > 0) {
            // 4. Le toca E/S: se bloquea y libera la CPU
            planificador.bloquear(actual, MotivoBloqueo.ES, ciclo);
            dispositivoES.solicitarES(actual);
            cpu.setEnEjecucion(null);
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