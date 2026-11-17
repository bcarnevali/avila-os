package edu.unimet.avilaos.disco.politicas;

import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * C-SCAN (SCAN circular): el cabezal atiende solo en una dirección. Al llegar al
 * extremo salta al extremo opuesto sin atender nada (el salto no cuenta como
 * desplazamiento) y sigue en la misma dirección.
 */
public class PoliticaCSCAN implements PoliticaDisco {

    @Override
    public TipoPoliticaDisco getTipo() { return TipoPoliticaDisco.C_SCAN; }

    @Override
    public int elegir(ListaEnlazada<SolicitudES> cola, int posicion, DireccionCabezal direccion) {
        return PoliticaSCAN.masCercanaEnDireccion(cola, posicion, direccion);
    }

    @Override
    public boolean saltaAlExtremoOpuesto() { return true; }
}
