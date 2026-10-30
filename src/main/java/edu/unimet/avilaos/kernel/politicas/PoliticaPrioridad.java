package edu.unimet.avilaos.kernel.politicas;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/**
 * Prioridades apropiativas: va primero el de mayor prioridad.
 * Convención: número menor = prioridad más alta (1 es la máxima).
 */
public class PoliticaPrioridad implements PoliticaPlanificacion {

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.PRIORIDAD_APROPIATIVA;
    }

    @Override
    public int comparar(PCB a, PCB b) {
        return Integer.compare(a.getPrioridad(), b.getPrioridad());
    }

    @Override
    public boolean debeExpropiar(PCB enEjecucion, PCB recienLlegado) {
        return recienLlegado.getPrioridad() < enEjecucion.getPrioridad();
    }

    @Override
    public int getQuantum() {
        return 0;
    }
}
