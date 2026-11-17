package edu.unimet.avilaos.disco.politicas;

import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * SCAN (elevador): el cabezal avanza en una dirección atendiendo lo que encuentra,
 * llega hasta el extremo del disco y regresa en la dirección contraria.
 */
public class PoliticaSCAN implements PoliticaDisco {

    @Override
    public TipoPoliticaDisco getTipo() { return TipoPoliticaDisco.SCAN; }

    @Override
    public int elegir(ListaEnlazada<SolicitudES> cola, int posicion, DireccionCabezal direccion) {
        return masCercanaEnDireccion(cola, posicion, direccion);
    }

    @Override
    public boolean saltaAlExtremoOpuesto() { return false; }

    /**
     * La solicitud más cercana que está en la dirección del cabezal (incluyendo su
     * posición actual), o -1 si no hay ninguna. En empate gana la que llegó primero.
     */
    static int masCercanaEnDireccion(ListaEnlazada<SolicitudES> cola, int posicion, DireccionCabezal direccion) {
        int mejor = -1;
        int menorDistancia = Integer.MAX_VALUE;
        for (int i = 0; i < cola.tamano(); i++) {
            int bloque = cola.obtener(i).getBloque();
            boolean adelante = direccion == DireccionCabezal.ARRIBA ? bloque >= posicion : bloque <= posicion;
            int distancia = Math.abs(bloque - posicion);
            if (adelante && distancia < menorDistancia) {
                menorDistancia = distancia;
                mejor = i;
            }
        }
        return mejor;
    }
}
