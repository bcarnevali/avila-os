package edu.unimet.avilaos.gui;

/** La GUI solo entrega la solicitud; quien la implemente decide qué hacer con ella. */
public interface CreadorProcesos {

    void crearProceso(SolicitudProceso solicitud);
}