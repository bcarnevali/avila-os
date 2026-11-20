package edu.unimet.avilaos.kernel;
import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.hardware.MemoriaPrincipal;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import java.util.concurrent.Semaphore;

/**
 * Planificador a mediano plazo de un computador (gestión de swap).
 * Decide cuándo suspender procesos hacia el disco (SWAP_OUT) si la RAM está
 * saturada o hay procesos nuevos esperando entrar, y cuándo reingresarlos
 * (SWAP_IN) cuando vuelven a estar listos y hay memoria disponible.
 */
public class PlanificadorMedianoPlazo implements Disco.ReceptorDisco {

    /** Porcentaje de uso de RAM a partir del cual se activa la suspensión. */
    public static final double UMBRAL_SUSPENSION_DEFAULT = 80.0;
    /** Porcentaje máximo de uso de RAM permitido para reingresar procesos desde swap. */
    public static final double UMBRAL_REACTIVACION_DEFAULT = 75.0;

    private final int idComputador;
    private final MemoriaPrincipal memoria;
    private final Planificador planificador;
    private final RegistroEventos log;
    private final Semaphore mutex = new Semaphore(1);

    private Disco disco;
    private double umbralSuspension = UMBRAL_SUSPENSION_DEFAULT;
    private double umbralReactivacion = UMBRAL_REACTIVACION_DEFAULT;

    /** Procesos cuya escritura de SWAP_OUT está en curso en el disco (aún ocupan RAM). */
    private final ListaEnlazada<PCB> saliendoASwap = new ListaEnlazada<>();
    /** Procesos cuya lectura de SWAP_IN está en curso en el disco (ya reservaron RAM). */
    private final ListaEnlazada<PCB> entrandoDeSwap = new ListaEnlazada<>();
    /** RAM comprometida por procesos que están saliendo a swap y aún no terminan su transferencia. */
    private int ramEnSwapOut = 0;

    public PlanificadorMedianoPlazo(int idComputador, MemoriaPrincipal memoria,
                                    Planificador planificador, RegistroEventos log) {
        if (memoria == null || planificador == null) {
            throw new IllegalArgumentException("Memoria y planificador son obligatorios");
        }
        this.idComputador = idComputador;
        this.memoria = memoria;
        this.planificador = planificador;
        this.log = log != null ? log : (ciclo, mensaje) -> { };
    }

    public void setDisco(Disco disco) {
        this.disco = disco;
    }

    public void setUmbrales(double umbralSuspension, double umbralReactivacion) {
        if (umbralSuspension <= 0 || umbralSuspension > 100 || umbralReactivacion <= 0 || umbralReactivacion > 100) {
            throw new IllegalArgumentException("Los umbrales deben estar entre 1 y 100");
        }
        this.umbralSuspension = umbralSuspension;
        this.umbralReactivacion = umbralReactivacion;
    }

    // ===================== Evaluación por ciclo =====================

    /** Evalúa si corresponde suspender o reactivar procesos en el ciclo actual. */
    public void evaluar(long ciclo) {
        if (disco == null) {
            return;
        }
        mutex.acquireUninterruptibly();
        try {
            limpiarBloquesDeTerminados();
            intentarSwapOut(ciclo);
            intentarSwapIn(ciclo);
        } finally {
            mutex.release();
        }
    }

    private void intentarSwapOut(long ciclo) {
        int ramEfectivaUsada = Math.max(0, memoria.getUsada() - ramEnSwapOut);
        double porcentajeEfectivo = (ramEfectivaUsada * 100.0) / memoria.getCapacidad();
        boolean hayNuevosEsperando = planificador.getNuevos().length > 0;

        if (porcentajeEfectivo < umbralSuspension && !hayNuevosEsperando) {
            return;
        }

        PCB victima = seleccionarVictimaSwapOut(porcentajeEfectivo >= umbralSuspension && hayNuevosEsperando);
        if (victima == null) {
            return;
        }

        int bloquesNecesarios = victima.getMemoriaRequerida();
        if (!disco.cabeEnSwap(bloquesNecesarios)) {
            return;
        }

        int primerBloque;
        try {
            primerBloque = disco.asignarSwap(victima.toString(), bloquesNecesarios);
        } catch (IllegalStateException e) {
            return;
        }

        boolean movido;
        EstadoProceso estadoPrevio = victima.getEstado();
        if (estadoPrevio == EstadoProceso.BLOQUEADO) {
            movido = planificador.moverABloqueadosSuspendidos(victima);
        } else {
            movido = planificador.moverAListosSuspendidos(victima);
        }

        if (!movido) {
            disco.liberar(primerBloque);
            return;
        }

        victima.setBloqueSwap(primerBloque);
        saliendoASwap.agregar(victima);
        ramEnSwapOut += bloquesNecesarios;

        SolicitudES solicitud = new SolicitudES(
                victima, OperacionES.SWAP_OUT, primerBloque, bloquesNecesarios, ciclo);
        disco.getPlanificador().encolar(solicitud, ciclo);
        registrar(ciclo, "Ordena SWAP_OUT de " + victima + " (" + bloquesNecesarios
                + " bloques desde bloque " + primerBloque + ")");
    }

    /**
     * Elige primero entre los procesos BLOQUEADOS (el que lleva más tiempo bloqueado).
     * Solo si no hay bloqueados y hay procesos NUEVOS esperando con RAM saturada,
     * toma el menos favorable de LISTOS.
     */
    private PCB seleccionarVictimaSwapOut(boolean permitirListos) {
        PCB[] bloqueados = planificador.getBloqueados();
        PCB mejorBloqueado = null;
        long menorCicloBloqueo = Long.MAX_VALUE;

        for (PCB p : bloqueados) {
            if (contiene(saliendoASwap, p) || contiene(entrandoDeSwap, p)) {
                continue;
            }
            if (!disco.cabeEnSwap(p.getMemoriaRequerida())) {
                continue;
            }
            long cicloBloq = p.getCicloBloqueo() >= 0 ? p.getCicloBloqueo() : 0;
            if (mejorBloqueado == null || cicloBloq < menorCicloBloqueo) {
                mejorBloqueado = p;
                menorCicloBloqueo = cicloBloq;
            }
        }
        if (mejorBloqueado != null) {
            return mejorBloqueado;
        }

        if (permitirListos) {
            PCB[] listos = planificador.getListos();
            for (int i = listos.length - 1; i >= 0; i--) {
                PCB p = listos[i];
                if (!contiene(saliendoASwap, p) && !contiene(entrandoDeSwap, p)
                        && disco.cabeEnSwap(p.getMemoriaRequerida())) {
                    return p;
                }
            }
        }
        return null;
    }

    private void intentarSwapIn(long ciclo) {
        PCB[] listosSusp = planificador.getListosSuspendidos();
        if (listosSusp.length == 0) {
            return;
        }

        boolean cpuSinListos = planificador.getListos().length == 0;
        for (PCB candidato : listosSusp) {
            if (contiene(saliendoASwap, candidato) || contiene(entrandoDeSwap, candidato)) {
                continue;
            }
            int req = candidato.getMemoriaRequerida();
            if (!memoria.cabe(req)) {
                continue;
            }
            double porcentajeTrasEntrar = ((memoria.getUsada() + req) * 100.0) / memoria.getCapacidad();
            if (!cpuSinListos && porcentajeTrasEntrar > umbralReactivacion) {
                continue;
            }
            if (!memoria.reservar(req)) {
                continue;
            }

            entrandoDeSwap.agregar(candidato);
            int bloqueOrigen = Math.max(0, candidato.getBloqueSwap());
            SolicitudES solicitud = new SolicitudES(
                    candidato, OperacionES.SWAP_IN, bloqueOrigen, req, ciclo);
            disco.getPlanificador().encolar(solicitud, ciclo);
            registrar(ciclo, "Ordena SWAP_IN de " + candidato + " (" + req
                    + " bloques desde bloque " + bloqueOrigen + ")");
            break; // un SWAP_IN por ciclo para evitar contención excesiva
        }
    }

    // ===================== Callback del Disco =====================

    @Override
    public void solicitudTerminada(SolicitudES solicitud, long ciclo) {
        PCB pcb = solicitud.getProceso();
        if (pcb == null) {
            return;
        }
        mutex.acquireUninterruptibly();
        try {
            if (solicitud.getOperacion() == OperacionES.SWAP_OUT) {
                completarSwapOut(pcb, ciclo);
            } else if (solicitud.getOperacion() == OperacionES.SWAP_IN) {
                completarSwapIn(pcb, ciclo);
            }
        } finally {
            mutex.release();
        }
    }

    private void completarSwapOut(PCB pcb, long ciclo) {
        if (saliendoASwap.eliminar(pcb)) {
            ramEnSwapOut = Math.max(0, ramEnSwapOut - pcb.getMemoriaRequerida());
        }
        memoria.liberar(pcb.getMemoriaRequerida());

        if (pcb.getEstado() == EstadoProceso.TERMINADO) {
            liberarSwapDeProceso(pcb);
            registrar(ciclo, "Completa SWAP_OUT de " + pcb + " (ya había terminado): libera RAM y swap");
            return;
        }

        registrar(ciclo, "Completa SWAP_OUT de " + pcb + ": libera "
                + pcb.getMemoriaRequerida() + " de RAM (estado: " + pcb.getEstado() + ")");
        planificador.intentarAdmitirNuevos(ciclo);
    }

    private void completarSwapIn(PCB pcb, long ciclo) {
        entrandoDeSwap.eliminar(pcb);
        liberarSwapDeProceso(pcb);

        if (pcb.getEstado() == EstadoProceso.TERMINADO) {
            memoria.liberar(pcb.getMemoriaRequerida());
            registrar(ciclo, "Completa SWAP_IN de " + pcb + " (ya había terminado): libera RAM reservada");
            return;
        }

        if (planificador.reingresarDesdeSwap(pcb, ciclo)) {
            registrar(ciclo, "Completa SWAP_IN de " + pcb + ": reingresa a cola de LISTOS");
        } else {
            memoria.liberar(pcb.getMemoriaRequerida());
        }
    }

    // ===================== Auxiliares =====================

    private void limpiarBloquesDeTerminados() {
        PCB[] terminados = planificador.getTerminados();
        for (PCB p : terminados) {
            if (p.getBloqueSwap() >= 0 && !contiene(saliendoASwap, p) && !contiene(entrandoDeSwap, p)) {
                liberarSwapDeProceso(p);
            }
        }
    }

    private void liberarSwapDeProceso(PCB pcb) {
        int bloque = pcb.getBloqueSwap();
        if (bloque >= 0 && disco != null) {
            try {
                disco.liberar(bloque);
            } catch (IllegalArgumentException ignored) {
                // Ya había sido liberado
            }
            pcb.setBloqueSwap(-1);
        }
    }

    private boolean contiene(ListaEnlazada<PCB> lista, PCB objetivo) {
        for (int i = 0; i < lista.tamano(); i++) {
            if (lista.obtener(i) == objetivo) {
                return true;
            }
        }
        return false;
    }

    private void registrar(long ciclo, String mensaje) {
        log.registrar(ciclo, "[Computador " + idComputador + " - Mediano Plazo] " + mensaje);
    }
}