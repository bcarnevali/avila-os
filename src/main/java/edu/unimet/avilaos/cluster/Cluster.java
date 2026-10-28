/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.cluster;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.hardware.RelojGlobal;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/**
 *
 * @author mfbal
 */
public class Cluster {
    private final ListaEnlazada<Computador> computadores = new ListaEnlazada<>();
    private final RelojGlobal reloj;

    public Cluster(int[] ramPorComputador, TipoPolitica politica, int quantum, long duracionCicloMs, RegistroEventos log) {
        if (ramPorComputador == null || ramPorComputador.length < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 computadores");
        }
        this.reloj = new RelojGlobal(duracionCicloMs);
        for (int i = 0; i < ramPorComputador.length; i++) {
            Computador c = new Computador(i + 1, ramPorComputador[i], politica, quantum, log);
            computadores.agregar(c);
            reloj.registrar(c);
        }
    }

    public Computador getComputador(int id) {
        return computadores.obtener(id - 1);
    }

    public int getCantidad() {
        return computadores.tamano();
    }

    public RelojGlobal getReloj() {
        return reloj;
    }

    public void iniciar() {
        reloj.start();
        reloj.reanudar();
    }
    
}
