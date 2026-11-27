package edu.unimet.avilaos.kernel;

import edu.unimet.avilaos.disco.SolicitudES;

/**
 * Lo que el núcleo necesita del sistema de archivos para que un proceso de E/S
 * haga su operación. El kernel no conoce directorios ni permisos: solo pide la
 * solicitud de disco y, cuando el disco termina, que se aplique el efecto.
 */
public interface ServicioArchivos {

    /**
     * Revisa que la operación del proceso se pueda hacer y arma la solicitud para el
     * disco (el bloque al que va el cabezal y cuántos bloques se transfieren).
     *
     * @throws IllegalStateException con el motivo si la operación no se puede hacer
     *         (el archivo no existe, no hay espacio, no tiene permiso, etc.)
     */
    SolicitudES prepararSolicitud(PCB proceso, long ciclo);

    /**
     * El disco terminó la solicitud: aplica el efecto en el sistema de archivos
     * (por ejemplo, al eliminar se liberan los bloques).
     *
     * @throws IllegalStateException con el motivo si ya no se puede aplicar
     */
    void completarOperacion(PCB proceso, SolicitudES solicitud, long ciclo);
}
