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
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import edu.unimet.avilaos.sincronizacion.Buffer;

public class Computador implements ComponenteReloj {

    private final int id;
    private final CPU cpu = new CPU();
    private final MemoriaPrincipal memoria;
    private final Metricas metricas = new Metricas();
    private final Planificador planificador;
    private final DispositivoES dispositivoES = new DispositivoES();
    private final RegistroEventos log;
    private Cluster cluster;

    public Computador(int id, int tamanoRam, TipoPolitica politica, int quantum, RegistroEventos log) {
        this.id = id;
        this.memoria = new MemoriaPrincipal(tamanoRam);
        this.planificador = new Planificador(id, memoria, metricas, politica, quantum, log);
        this.log = log;
    }

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
            actual = null;
        }

        actual = planificador.planificar(actual, ciclo);
        cpu.setEnEjecucion(actual);
        metricas.registrarCiclo(actual != null);

        if (actual == null) return;

        // 3. Ejecuta una instruccion (modo usuario)
        cpu.setModo(ModoEjecucion.USUARIO);
        cpu.ejecutarInstruccion();

        if (actual.leTocaBuffer()) {
            if (operarBuffer(actual, ciclo)) {
                cpu.setEnEjecucion(null);
                return;
            }
        }

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

    private boolean operarBuffer(PCB pcb, long ciclo) {
        if (cluster == null) return false;
        Buffer b = cluster.getBuffer(pcb.getIdBuffer());
        if (b == null) {
            if (log != null) {
                log.registrar(ciclo, "[Computador " + id + "] Error: Buffer '" + pcb.getIdBuffer() + "' no existe para el proceso " + pcb.getId());
            }
            return false;
        }

        // TODO Bettina: latencia de red si b.getIdAnfitrion() != id

        if (pcb.getTipo() == TipoProceso.PRODUCTOR) {
            if (!b.getVacios().semWait(pcb)) {
                pcb.retrocederInstruccion();
                planificador.bloquear(pcb, MotivoBloqueo.SEMAFORO_VACIOS, ciclo);
                return true;
            }
            if (!b.getMutex().semWait(pcb)) {
                pcb.retrocederInstruccion();
                planificador.bloquear(pcb, MotivoBloqueo.SEMAFORO_MUTEX, ciclo);
                return true;
            }
            b.insertar(pcb.getId());
            pcb.registrarElementoProcesado();
            metricas.registrarProduccion();
            if (log != null) {
                log.registrar(ciclo, "[Computador " + id + "] Proceso " + pcb.getId() + " produce en " + b.getId() + " (" + b.getOcupados() + "/" + b.getCapacidad() + ")");
            }
            despertar(b.getMutex().semSignal(), ciclo);
            despertar(b.getLlenos().semSignal(), ciclo);
            return false;
        } else if (pcb.getTipo() == TipoProceso.CONSUMIDOR) {
            if (!b.getLlenos().semWait(pcb)) {
                pcb.retrocederInstruccion();
                planificador.bloquear(pcb, MotivoBloqueo.SEMAFORO_LLENOS, ciclo);
                return true;
            }
            if (!b.getMutex().semWait(pcb)) {
                pcb.retrocederInstruccion();
                planificador.bloquear(pcb, MotivoBloqueo.SEMAFORO_MUTEX, ciclo);
                return true;
            }
            b.extraer();
            pcb.registrarElementoProcesado();
            metricas.registrarConsumo();
            if (log != null) {
                log.registrar(ciclo, "[Computador " + id + "] Proceso " + pcb.getId() + " consume de " + b.getId() + " (" + b.getOcupados() + "/" + b.getCapacidad() + ")");
            }
            despertar(b.getMutex().semSignal(), ciclo);
            despertar(b.getVacios().semSignal(), ciclo);
            return false;
        }
        return false;
    }

    private void despertar(PCB p, long ciclo) {
        if (p != null && cluster != null) {
            Computador compDestino = cluster.getComputador(p.getIdComputador());
            if (compDestino != null) {
                compDestino.getPlanificador().desbloquear(p, ciclo);
            }
        }
    }

    public void crearProceso(PCB pcb, long ciclo) {
        planificador.crearProceso(pcb, ciclo);
    }

    public void setCluster(Cluster cluster) { this.cluster = cluster; }
    public Cluster getCluster() { return cluster; }
    public int getId() { return id; }
    public CPU getCpu() { return cpu; }
    public MemoriaPrincipal getMemoria() { return memoria; }
    public Metricas getMetricas() { return metricas; }
    public Planificador getPlanificador() { return planificador; }
    public DispositivoES getDispositivoES() { return dispositivoES; }
}