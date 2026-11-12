package edu.unimet.avilaos.disco.politicas;

import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/** FIFO: atiende las solicitudes en el orden en que llegaron. */
public class PoliticaFIFO implements PoliticaDisco {

    @Override
    public TipoPoliticaDisco getTipo() { return TipoPoliticaDisco.FIFO; }

    @Override
    public int elegir(ListaEnlazada<SolicitudES> cola, int posicion, DireccionCabezal direccion) {
        return 0;
    }

    @Override
    public boolean saltaAlExtremoOpuesto() { return false; }
}
