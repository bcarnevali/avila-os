package edu.unimet.avilaos.kernel.politicas;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/**
 * Round Robin: orden de llegada, pero cada proceso usa la CPU como máximo
 * un quantum. El planificador lo devuelve al final de listos al agotarlo.
 */
public class PoliticaRoundRobin implements PoliticaPlanificacion {

    private final int quantum;

    public PoliticaRoundRobin(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("El quantum debe ser mayor que 0");
        }
        this.quantum = quantum;
    }

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.ROUND_ROBIN;
    }

    @Override
    public int comparar(PCB a, PCB b) {
        return 0;
    }

    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB recienLlegado) {
        return false;
    }

    @Override
    public int getQuantum() {
        return quantum;
    }
}
