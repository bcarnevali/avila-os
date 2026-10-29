/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.sincronizacion;

import edu.unimet.avilaos.estructuras.Cola;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;

public class SemaforoSO {

    private final String nombre;
    private int valor;
    private final Cola<PCB> bloqueados;

    public SemaforoSO(String nombre, int valorInicial) {
        this.nombre = nombre;
        this.valor = valorInicial;
        this.bloqueados = new Cola<>();
    }

    public synchronized boolean semWait(PCB pcb) {
        if (valor > 0) {
            valor--;
            return true;
        } else {
            bloqueados.encolar(pcb);
            return false;
        }
    }

    public synchronized PCB semSignal() {
        valor++;
        while (!bloqueados.estaVacia()) {
            PCB pcb = bloqueados.desencolar();
            if (pcb != null && pcb.getEstado() != EstadoProceso.TERMINADO) {
                return pcb;
            }
        }
        return null;
    }

    public synchronized int getValor() {
        return valor;
    }

    public String getNombre() {
        return nombre;
    }

    public synchronized PCB[] getBloqueados() {
        int n = bloqueados.tamano();
        PCB[] copia = new PCB[n];
        for (int i = 0; i < n; i++) {
            PCB p = bloqueados.desencolar();
            copia[i] = p;
            bloqueados.encolar(p);
        }
        return copia;
    }
}