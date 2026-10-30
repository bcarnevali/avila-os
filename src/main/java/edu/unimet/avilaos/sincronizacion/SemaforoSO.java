package edu.unimet.avilaos.sincronizacion;

import edu.unimet.avilaos.estructuras.Cola;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import java.util.concurrent.Semaphore;

/**
 * Semaforo SIMULADO de AvilaOS (el que usan los procesos productores y consumidores).
 * No confundir con el Semaphore de Java, que aqui solo protege el valor y la cola
 * de bloqueados porque el hilo del reloj los modifica y el hilo de la GUI los lee.
 */
public class SemaforoSO {

    private final String nombre;
    private int valor;
    private final Cola<PCB> bloqueados;
    private final Semaphore mutex = new Semaphore(1);

    public SemaforoSO(String nombre, int valorInicial) {
        this.nombre = nombre;
        this.valor = valorInicial;
        this.bloqueados = new Cola<>();
    }

    /** true si el proceso puede seguir; false si debe bloquearse (queda en la cola del semaforo). */
    public boolean semWait(PCB pcb) {
        mutex.acquireUninterruptibly();
        try {
            if (valor > 0) {
                valor--;
                return true;
            }
            bloqueados.encolar(pcb);
            return false;
        } finally {
            mutex.release();
        }
    }

    /** Suma 1 y devuelve el proceso a despertar (el primero vivo de la cola), o null. */
    public PCB semSignal() {
        mutex.acquireUninterruptibly();
        try {
            valor++;
            while (!bloqueados.estaVacia()) {
                PCB pcb = bloqueados.desencolar();
                if (pcb != null && pcb.getEstado() != EstadoProceso.TERMINADO) {
                    return pcb;
                }
            }
            return null;
        } finally {
            mutex.release();
        }
    }

    public int getValor() {
        mutex.acquireUninterruptibly();
        try {
            return valor;
        } finally {
            mutex.release();
        }
    }

    public String getNombre() {
        return nombre;
    }

    /** Copia de los procesos bloqueados (para la GUI). Descarta los que ya terminaron por deadline. */
    public PCB[] getBloqueados() {
        mutex.acquireUninterruptibly();
        try {
            int n = bloqueados.tamano();
            int vivos = 0;
            PCB[] temporal = new PCB[n];
            for (int i = 0; i < n; i++) {
                PCB p = bloqueados.desencolar();
                if (p.getEstado() != EstadoProceso.TERMINADO) {
                    temporal[vivos++] = p;
                    bloqueados.encolar(p);
                }
            }
            PCB[] copia = new PCB[vivos];
            for (int i = 0; i < vivos; i++) {
                copia[i] = temporal[i];
            }
            return copia;
        } finally {
            mutex.release();
        }
    }
}
