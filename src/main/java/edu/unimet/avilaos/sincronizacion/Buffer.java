/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.sincronizacion;

import java.util.concurrent.Semaphore;
import edu.unimet.avilaos.hardware.MemoriaPrincipal;

public class Buffer {

    private final String id;
    private final int capacidad;
    private final int idAnfitrion;
    private final int[] elementos;
    private int frente;
    private int fin;
    private int ocupados;

    private final SemaforoSO vacios;
    private final SemaforoSO llenos;
    private final SemaforoSO mutex;

    private final Semaphore mutexJava = new Semaphore(1);

    public Buffer(String id, int capacidad, int idAnfitrion, MemoriaPrincipal memoriaAnfitrion) {
        if (!memoriaAnfitrion.reservar(capacidad)) {
            throw new IllegalStateException("No hay suficiente memoria RAM para el buffer " + id);
        }
        this.id = id;
        this.capacidad = capacidad;
        this.idAnfitrion = idAnfitrion;
        this.elementos = new int[capacidad];
        this.frente = 0;
        this.fin = 0;
        this.ocupados = 0;

        this.vacios = new SemaforoSO("vacios_" + id, capacidad);
        this.llenos = new SemaforoSO("llenos_" + id, 0);
        this.mutex = new SemaforoSO("mutex_" + id, 1);
    }

    public void insertar(int idProductor) {
        mutexJava.acquireUninterruptibly();
        try {
            elementos[fin] = idProductor;
            fin = (fin + 1) % capacidad;
            ocupados++;
        } finally {
            mutexJava.release();
        }
    }

    public int extraer() {
        mutexJava.acquireUninterruptibly();
        try {
            int idProductor = elementos[frente];
            frente = (frente + 1) % capacidad;
            ocupados--;
            return idProductor;
        } finally {
            mutexJava.release();
        }
    }

    public String getId() {
        return id;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getIdAnfitrion() {
        return idAnfitrion;
    }

    public int getOcupados() {
        mutexJava.acquireUninterruptibly();
        try {
            return ocupados;
        } finally {
            mutexJava.release();
        }
    }

    public int[] getElementos() {
        mutexJava.acquireUninterruptibly();
        try {
            int[] copia = new int[ocupados];
            for (int i = 0; i < ocupados; i++) {
                copia[i] = elementos[(frente + i) % capacidad];
            }
            return copia;
        } finally {
            mutexJava.release();
        }
    }

    public SemaforoSO getVacios() {
        return vacios;
    }

    public SemaforoSO getLlenos() {
        return llenos;
    }

    public SemaforoSO getMutex() {
        return mutex;
    }
}