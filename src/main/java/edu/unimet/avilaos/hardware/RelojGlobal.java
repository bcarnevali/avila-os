package edu.unimet.avilaos.hardware;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import java.util.concurrent.Semaphore;

/**
 * Reloj global del sistema distribuido. Es un hilo de Java que en cada ciclo
 * llama tick() de cada componente registrado (la red y cada computador), en orden.
 * La lista de componentes se protege con un Semaphore; la pausa tambien se
 * implementa con un Semaphore: el hilo se queda esperando un permiso hasta que se reanuda.
 */
public class RelojGlobal extends Thread {

    private final ListaEnlazada<ComponenteReloj> componentes = new ListaEnlazada<>();
    private final Semaphore mutexComponentes = new Semaphore(1);
    private final Semaphore permisoReanudar = new Semaphore(0);
    private volatile long cicloActual = 0;
    private volatile long duracionCicloMs;
    private volatile boolean corriendo = true;
    private volatile boolean pausado = false;

    public RelojGlobal(long duracionCicloMs) {
        if (duracionCicloMs <= 0) {
            throw new IllegalArgumentException("La duración del ciclo debe ser mayor a 0");
        }
        this.duracionCicloMs = duracionCicloMs;
        setDaemon(true); // no impide cerrar el programa
    }

    public void registrar(ComponenteReloj componente) {
        if (componente == null) return;
        mutexComponentes.acquireUninterruptibly();
        try {
            componentes.agregar(componente);
        } finally {
            mutexComponentes.release();
        }
    }

    public void pausar() {
        pausado = true;
    }

    public void reanudar() {
        if (pausado) {
            pausado = false;
            permisoReanudar.release();
        }
    }

    public void detener() {
        corriendo = false;
        pausado = false;
        permisoReanudar.release();
        interrupt();
    }

    public void setDuracionCiclo(long duracionCicloMs) {
        if (duracionCicloMs <= 0) {
            throw new IllegalArgumentException("La duración del ciclo debe ser mayor a 0");
        }
        this.duracionCicloMs = duracionCicloMs;
    }

    public long getCicloActual() {
        return cicloActual;
    }

    public boolean isPausado() {
        return pausado;
    }

    @Override
    public void run() {
        while (corriendo) {
            try {
                // Pausado: espera el permiso que deja reanudar()
                while (pausado && corriendo) {
                    permisoReanudar.acquire();
                }
                if (!corriendo) break;

                cicloActual++;

                mutexComponentes.acquireUninterruptibly();
                try {
                    for (int i = 0; i < componentes.tamano(); i++) {
                        componentes.obtener(i).tick(cicloActual);
                    }
                } finally {
                    mutexComponentes.release();
                }

                Thread.sleep(duracionCicloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
