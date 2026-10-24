package edu.unimet.avilaos.kernel.politicas;

import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/** Crea la política correspondiente a un tipo. La usa la GUI al cambiar de política. */
public final class FabricaPoliticas {

    private FabricaPoliticas() {
    }

    public static PoliticaPlanificacion crear(TipoPolitica tipo, int quantum) {
        switch (tipo) {
            case FCFS:
                return new PoliticaFCFS();
            case ROUND_ROBIN:
                return new PoliticaRoundRobin(quantum);
            case EDF:
                return new PoliticaEDF();
            case PRIORIDAD_APROPIATIVA:
                return new PoliticaPrioridad();
            default:
                throw new IllegalArgumentException("Política no soportada: " + tipo);
        }
    }
}
