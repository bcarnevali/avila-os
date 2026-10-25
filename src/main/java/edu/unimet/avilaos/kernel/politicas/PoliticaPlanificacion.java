package edu.unimet.avilaos.kernel.politicas;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/**
 * Regla de planificación. El planificador la usa para ordenar la cola de listos
 * y para decidir si un proceso que llega expropia al que está en CPU.
 */
public interface PoliticaPlanificacion {

    TipoPolitica getTipo();

    /**
     * Negativo si a va antes que b, positivo si b va antes, 0 si da igual.
     * Con 0 el proceso nuevo queda detrás de los que ya estaban (orden de llegada).
     */
    int comparar(PCB a, PCB b);

    /** true si el proceso que acaba de llegar a listos debe sacar al que está en CPU. */
    boolean debeExpropiar(PCB enEjecucion, PCB recienLlegado);

    /** Quantum en ciclos. 0 si la política no usa quantum. */
    int getQuantum();
}
