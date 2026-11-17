package edu.unimet.avilaos.disco.politicas;

import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * SSTF (Shortest Seek Time First): atiende la solicitud más cercana al cabezal.
 * Si dos están a la misma distancia, gana la que llegó primero.
 */
public class PoliticaSSTF implements PoliticaDisco {

    @Override
    public TipoPoliticaDisco getTipo() { return TipoPoliticaDisco.SSTF; }

    @Override
    public int elegir(ListaEnlazada<SolicitudES> cola, int posicion, DireccionCabezal direccion) {
        int mejor = 0;
        int menorDistancia = Integer.MAX_VALUE;
        for (int i = 0; i < cola.tamano(); i++) {
            int distancia = Math.abs(cola.obtener(i).getBloque() - posicion);
            if (distancia < menorDistancia) { // estricto: en empate se queda la más antigua
                menorDistancia = distancia;
                mejor = i;
            }
        }
        return mejor;
    }

    @Override
    public boolean saltaAlExtremoOpuesto() { return false; }
}
