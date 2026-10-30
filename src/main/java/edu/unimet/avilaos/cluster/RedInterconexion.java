package edu.unimet.avilaos.cluster;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.hardware.ComponenteReloj;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import java.util.concurrent.Semaphore;

/**
 * Red que conecta los computadores del cluster.
 * Cuando un proceso accede a un buffer que vive en otro computador, queda
 * bloqueado (LATENCIA_RED) durante "latencia" ciclos. Al cumplirse, la red lo
 * despierta en el planificador de su propio computador y le da permiso para
 * hacer esa operacion remota sin volver a esperar.
 */
public class RedInterconexion implements ComponenteReloj {

    /** Un proceso esperando la respuesta de la red. */
    private static class Envio {
        final PCB pcb;
        final int idDestino;
        int restante;

        Envio(PCB pcb, int idDestino, int restante) {
            this.pcb = pcb;
            this.idDestino = idDestino;
            this.restante = restante;
        }
    }

    private final Cluster cluster;
    private final RegistroEventos log;
    private final Semaphore mutex = new Semaphore(1);
    private final ListaEnlazada<Envio> enTransito = new ListaEnlazada<>();
    /** Procesos que ya pagaron la latencia y pueden hacer su operacion remota. */
    private final ListaEnlazada<PCB> autorizados = new ListaEnlazada<>();
    private int latencia;

    public RedInterconexion(Cluster cluster, int latencia, RegistroEventos log) {
        this.cluster = cluster;
        this.log = log;
        setLatencia(latencia);
    }

    /** El proceso empieza a esperar la red para llegar al computador idDestino. */
    public void enviar(PCB pcb, int idDestino, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            enTransito.agregar(new Envio(pcb, idDestino, latencia));
            registrar(ciclo, "Proceso " + pcb + " espera " + latencia
                    + " ciclos de latencia para llegar al Computador " + idDestino);
        } finally {
            mutex.release();
        }
    }

    /**
     * true si el proceso ya pago la latencia (y consume ese permiso).
     * Si devuelve false, hay que bloquearlo y llamar a enviar().
     */
    public boolean yaLlego(PCB pcb) {
        mutex.acquireUninterruptibly();
        try {
            return autorizados.eliminar(pcb);
        } finally {
            mutex.release();
        }
    }

    @Override
    public void tick(long ciclo) {
        ListaEnlazada<Envio> llegaron = new ListaEnlazada<>();
        mutex.acquireUninterruptibly();
        try {
            for (int i = 0; i < enTransito.tamano(); i++) {
                Envio e = enTransito.obtener(i);
                e.restante--;
                if (e.restante <= 0 || e.pcb.getEstado() == EstadoProceso.TERMINADO) {
                    llegaron.agregar(e);
                }
            }
            for (int i = 0; i < llegaron.tamano(); i++) {
                Envio e = llegaron.obtener(i);
                enTransito.eliminar(e);
                if (e.pcb.getEstado() != EstadoProceso.TERMINADO) {
                    autorizados.agregar(e.pcb);
                }
            }
        } finally {
            mutex.release();
        }
        // Se despierta fuera del mutex: el planificador tiene su propio Semaphore
        for (int i = 0; i < llegaron.tamano(); i++) {
            Envio e = llegaron.obtener(i);
            if (e.pcb.getEstado() == EstadoProceso.TERMINADO) {
                continue; // se le vencio el deadline mientras esperaba la red
            }
            registrar(ciclo, "Proceso " + e.pcb + " llega al Computador " + e.idDestino);
            cluster.getComputador(e.pcb.getIdComputador()).getPlanificador().desbloquear(e.pcb, ciclo);
        }
    }

    public int getLatencia() {
        return latencia;
    }

    public void setLatencia(int latencia) {
        if (latencia < 0) {
            throw new IllegalArgumentException("La latencia no puede ser negativa");
        }
        this.latencia = latencia;
    }

    /** Cuantos procesos estan esperando la red (para la interfaz). */
    public int getEnTransito() {
        mutex.acquireUninterruptibly();
        try {
            return enTransito.tamano();
        } finally {
            mutex.release();
        }
    }

    private void registrar(long ciclo, String mensaje) {
        if (log != null) {
            log.registrar(ciclo, "[Red] " + mensaje);
        }
    }
}
