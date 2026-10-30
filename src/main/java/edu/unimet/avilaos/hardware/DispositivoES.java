package edu.unimet.avilaos.hardware;

import edu.unimet.avilaos.estructuras.Cola;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import java.util.concurrent.Semaphore;

/**
 * Dispositivo de E/S de un computador. Atiende un proceso a la vez en orden FIFO.
 * Sus colas se protegen con un Semaphore de Java porque la GUI puede leerlas.
 */
public class DispositivoES implements ComponenteReloj {

    private final Cola<PCB> enEspera = new Cola<>();
    private final Cola<PCB> terminados = new Cola<>();
    private final Semaphore mutex = new Semaphore(1);
    private PCB enAtencion;
    private int restante;

    public void solicitarES(PCB pcb) {
        mutex.acquireUninterruptibly();
        try {
            enEspera.encolar(pcb);
        } finally {
            mutex.release();
        }
    }

    @Override
    public void tick(long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (enAtencion == null && !enEspera.estaVacia()) {
                enAtencion = enEspera.desencolar();
                restante = enAtencion.getCiclosParaSatisfacerES();
            }
            if (enAtencion == null) {
                return;
            }
            // Se le vencio el deadline mientras esperaba o hacia su E/S
            if (enAtencion.getEstado() == EstadoProceso.TERMINADO) {
                enAtencion = null;
                return;
            }
            restante--;
            if (restante <= 0) {
                terminados.encolar(enAtencion);
                enAtencion = null;
            }
        } finally {
            mutex.release();
        }
    }

    /** Devuelve un proceso que termino su E/S, o null si no hay. */
    public PCB retirarTerminado() {
        mutex.acquireUninterruptibly();
        try {
            return terminados.desencolar();
        } finally {
            mutex.release();
        }
    }

    public PCB getEnAtencion() {
        mutex.acquireUninterruptibly();
        try {
            return enAtencion;
        } finally {
            mutex.release();
        }
    }

    /** Cuantos procesos esperan su turno en el dispositivo. */
    public int getCantidadEnEspera() {
        mutex.acquireUninterruptibly();
        try {
            return enEspera.tamano();
        } finally {
            mutex.release();
        }
    }
}
