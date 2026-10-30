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
import edu.unimet.avilaos.sincronizacion.SemaforoSO;

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
        liberarBuffersDeTerminados(ciclo);

        // Un proceso a mitad de una operacion sobre un buffer no se expropia:
        // la seccion critica (semWait ... semSignal) se completa sin interrupciones del planificador
        if (actual == null || actual.getPasoBuffer() == 0) {
            actual = planificador.planificar(actual, ciclo);
            cpu.setEnEjecucion(actual);
        }
        metricas.registrarCiclo(actual != null);

        if (actual == null) return; // CPU ociosa en este ciclo

        // 3a. Operacion del SO sobre un buffer: cada paso cuenta como una instruccion en modo sistema operativo
        if (actual.getPasoBuffer() > 0) {
            cpu.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
            if (ejecutarPasoBuffer(actual, ciclo)) {
                cpu.setEnEjecucion(null); // se bloqueo: libera la CPU
                return;
            }
            if (actual.getPasoBuffer() == 0 && actual.haTerminado()) {
                planificador.terminar(actual, ciclo);
                cpu.setEnEjecucion(null);
            }
            return;
        }

        // 3b. Instruccion del programa (modo usuario)
        cpu.setModo(ModoEjecucion.USUARIO);
        cpu.ejecutarInstruccion();

        if (actual.haTerminado()) {
            planificador.terminar(actual, ciclo);
            cpu.setEnEjecucion(null);
        } else if (actual.leTocaBuffer()) {
            // A partir del proximo ciclo hace sus operaciones sobre el buffer en modo SO
            actual.setPasoBuffer(1);
        } else if (actual.leTocaES() && actual.getCiclosParaSatisfacerES() > 0) {
            // 4. Le toca E/S: se bloquea y libera la CPU
            planificador.bloquear(actual, MotivoBloqueo.ES, ciclo);
            dispositivoES.solicitarES(actual);
            cpu.setEnEjecucion(null);
        }
    }

    /**
     * Ejecuta UN paso de la operacion sobre el buffer (algoritmo del anexo, Stallings cap. 5).
     * Productor: 1 semWait(vacios), 2 semWait(mutex), 3 insertar, 4 semSignal(mutex), 5 semSignal(llenos).
     * Consumidor: 1 semWait(llenos), 2 semWait(mutex), 3 extraer, 4 semSignal(mutex), 5 semSignal(vacios).
     * Devuelve true si el proceso se bloqueo. Al despertar repite el mismo paso.
     */
    private boolean ejecutarPasoBuffer(PCB pcb, long ciclo) {
        Buffer b = cluster == null ? null : cluster.getBuffer(pcb.getIdBuffer());
        if (b == null) {
            registrar(ciclo, "Error: el buffer '" + pcb.getIdBuffer() + "' no existe para el proceso " + pcb);
            pcb.setPasoBuffer(0);
            return false;
        }
        boolean productor = pcb.getTipo() == TipoProceso.PRODUCTOR;
        SemaforoSO espacio = productor ? b.getVacios() : b.getLlenos();
        SemaforoSO aviso = productor ? b.getLlenos() : b.getVacios();
        MotivoBloqueo motivoEspacio = productor ? MotivoBloqueo.SEMAFORO_VACIOS : MotivoBloqueo.SEMAFORO_LLENOS;

        switch (pcb.getPasoBuffer()) {
            case 1:
                // Acceso remoto: espera la latencia de la red una vez por operacion
                if (b.getIdAnfitrion() != id && !pcb.isAccesoRemotoPagado()) {
                    if (!cluster.getRed().yaLlego(pcb)) {
                        planificador.bloquear(pcb, MotivoBloqueo.LATENCIA_RED, ciclo);
                        registrar(ciclo, "Proceso " + pcb + " accede al buffer " + b.getId()
                                + " de forma remota (está en el Computador " + b.getIdAnfitrion() + ")");
                        cluster.getRed().enviar(pcb, b.getIdAnfitrion(), ciclo);
                        return true;
                    }
                    pcb.setAccesoRemotoPagado(true);
                }
                if (!espacio.semWait(pcb)) {
                    bloquearEnSemaforo(pcb, motivoEspacio, espacio, ciclo);
                    return true;
                }
                pcb.setPasoBuffer(2);
                return false;
            case 2:
                if (!b.getMutex().semWait(pcb)) {
                    bloquearEnSemaforo(pcb, MotivoBloqueo.SEMAFORO_MUTEX, b.getMutex(), ciclo);
                    return true;
                }
                pcb.setPasoBuffer(3);
                return false;
            case 3:
                if (productor) {
                    b.insertar(pcb.getId());
                    metricas.registrarProduccion();
                    registrar(ciclo, "Proceso " + pcb + " produce en " + b.getId()
                            + " (" + b.getOcupados() + "/" + b.getCapacidad() + ")");
                } else {
                    b.extraer();
                    metricas.registrarConsumo();
                    registrar(ciclo, "Proceso " + pcb + " consume de " + b.getId()
                            + " (" + b.getOcupados() + "/" + b.getCapacidad() + ")");
                }
                pcb.registrarElementoProcesado();
                pcb.setPasoBuffer(4);
                return false;
            case 4:
                despertar(b.getMutex().semSignal(), ciclo);
                pcb.setPasoBuffer(5);
                return false;
            default:
                despertar(aviso.semSignal(), ciclo);
                pcb.setPasoBuffer(0);
                pcb.setAccesoRemotoPagado(false);
                return false;
        }
    }

    private void bloquearEnSemaforo(PCB pcb, MotivoBloqueo motivo, SemaforoSO semaforo, long ciclo) {
        planificador.bloquear(pcb, motivo, ciclo);
        registrar(ciclo, "Proceso " + pcb + " se bloquea en el semáforo '" + semaforo.getNombre() + "'");
    }

    /** Revisa si algun proceso murio por deadline (en la CPU o en una cola) a mitad de una operacion. */
    private void liberarBuffersDeTerminados(long ciclo) {
        PCB[] terminados = planificador.getTerminados();
        for (PCB p : terminados) {
            if (p.getPasoBuffer() > 0) {
                liberarBuffer(p, p.getPasoBuffer(), ciclo);
                p.setPasoBuffer(0);
            }
        }
    }

    /**
     * El proceso murio por deadline a mitad de una operacion: devuelve los semaforos
     * que ya habia tomado para no dejar bloqueados a los demas para siempre.
     */
    private void liberarBuffer(PCB pcb, int paso, long ciclo) {
        Buffer b = cluster == null ? null : cluster.getBuffer(pcb.getIdBuffer());
        if (b == null) return;
        boolean productor = pcb.getTipo() == TipoProceso.PRODUCTOR;
        SemaforoSO espacio = productor ? b.getVacios() : b.getLlenos();
        SemaforoSO aviso = productor ? b.getLlenos() : b.getVacios();
        if (paso == 3 || paso == 4) {
            despertar(b.getMutex().semSignal(), ciclo);
        }
        if (paso == 2 || paso == 3) {
            despertar(espacio.semSignal(), ciclo); // no llego a usar el espacio que habia reservado
        } else if (paso == 4 || paso == 5) {
            despertar(aviso.semSignal(), ciclo); // el elemento ya esta en el buffer: avisa
        }
        registrar(ciclo, "Proceso " + pcb + " muere a mitad de una operación sobre " + b.getId()
                + ": se liberan sus semáforos");
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

    private void registrar(long ciclo, String mensaje) {
        if (log != null) {
            log.registrar(ciclo, "[Computador " + id + "] " + mensaje);
        }
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