/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.hardware;

import edu.unimet.avilaos.estructuras.ListaEnlazada;

/**
 *
 * @author mfbal
 */
public class RelojGlobal extends Thread{
    
    private final ListaEnlazada<ComponenteReloj> componentes = new ListaEnlazada<>();
    private long cicloActual = 0;
    private volatile long duracionCicloMs;
    private volatile boolean corriendo = true;
    private volatile boolean pausado = false;

    public RelojGlobal(long duracionCicloMs) {
        if (duracionCicloMs <= 0) {
            throw new IllegalArgumentException("La duración del ciclo debe ser mayor a 0");
        }
        this.duracionCicloMs = duracionCicloMs;
    }

    public synchronized void registrar(ComponenteReloj componente) {
        if (componente != null) {
            componentes.agregar(componente);
        }
    }

    public synchronized void pausar() {
        this.pausado = true;
    }

    public synchronized void reanudar() {
        this.pausado = false;
        notifyAll();
    }

    public void detener() {
        this.corriendo = false;
        this.pausado = false;
        this.interrupt();
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
                synchronized (this) {
                    while (pausado && corriendo) {
                        wait();
                    }
                }
                if (!corriendo) break;

                cicloActual++;

                synchronized (this) {
                    int n = componentes.tamano();
                    for (int i = 0; i < n; i++) {
                        ComponenteReloj comp = componentes.obtener(i);
                        if (comp != null) {
                            comp.tick(cicloActual);
                        }
                    }
                }

                Thread.sleep(duracionCicloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
}
