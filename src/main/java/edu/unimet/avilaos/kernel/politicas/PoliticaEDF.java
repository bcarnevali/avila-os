package edu.unimet.avilaos.kernel.politicas;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/** Earliest Deadline First: va primero el proceso con el deadline más cercano. Apropiativa. */
public class PoliticaEDF implements PoliticaPlanificacion {

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.EDF;
    }

    @Override
    public int comparar(PCB a, PCB b) {
        return Integer.compare(a.getDeadlineRestante(), b.getDeadlineRestante());
    }

    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB recienLlegado) {
        return recienLlegado.getDeadlineRestante() < enEjecucion.getDeadlineRestante();
    }

    @Override
    public int getQuantum() {
        return 0;
    }
}
