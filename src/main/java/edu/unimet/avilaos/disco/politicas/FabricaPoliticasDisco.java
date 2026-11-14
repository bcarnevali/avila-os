package edu.unimet.avilaos.disco.politicas;

import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/** Crea la política de disco que corresponde a cada tipo. */
public final class FabricaPoliticasDisco {

    private FabricaPoliticasDisco() {
    }

    public static PoliticaDisco crear(TipoPoliticaDisco tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("La política de disco es obligatoria");
        }
        switch (tipo) {
            case FIFO: return new PoliticaFIFO();
            case SSTF: return new PoliticaSSTF();
            case SCAN: return new PoliticaSCAN();
            case C_SCAN: return new PoliticaCSCAN();
            default: throw new IllegalArgumentException("Política de disco desconocida: " + tipo);
        }
    }
}
