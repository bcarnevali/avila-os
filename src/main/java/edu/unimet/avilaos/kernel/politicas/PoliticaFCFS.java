package edu.unimet.avilaos.kernel.politicas;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/** First Come First Served: se atiende en orden de llegada a listos, sin expropiación. */
public class PoliticaFCFS implements PoliticaPlanificacion {

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.FCFS;
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
        return 0;
    }
}
