package edu.unimet.avilaos.disco.politicas;

import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * Regla de planificación del disco (patrón Strategy, igual que las políticas de CPU).
 * El planificador del disco le pregunta cuál solicitud atender y qué hacer cuando
 * el cabezal llega a un extremo.
 */
public interface PoliticaDisco {

    TipoPoliticaDisco getTipo();

    /**
     * Elige la próxima solicitud a atender.
     *
     * @param cola      solicitudes pendientes, en orden de llegada (no está vacía)
     * @param posicion  bloque donde está el cabezal
     * @param direccion hacia dónde se mueve el cabezal
     * @return el índice en la cola de la solicitud elegida, o -1 si no hay ninguna
     *         en la dirección actual y el cabezal debe seguir hasta el extremo
     *         (solo SCAN y C-SCAN devuelven -1)
     */
    int elegir(ListaEnlazada<SolicitudES> cola, int posicion, DireccionCabezal direccion);

    /**
     * Qué hace el cabezal al llegar a un extremo sin nada pendiente en su dirección:
     * true si salta al extremo opuesto sin atender nada (C-SCAN), false si invierte la
     * dirección (SCAN).
     */
    boolean saltaAlExtremoOpuesto();
}
