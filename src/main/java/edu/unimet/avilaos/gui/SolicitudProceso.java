package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.modelo.enums.TipoProceso;

/** Datos ya validados que entrega el formulario de la Vista 1. */
public record SolicitudProceso(
        String nombre,
        TipoProceso tipo,
        int prioridad,
        int memoria,
        int instrucciones,
        int deadline,
        int idComputador,
        int ciclosEntreES,
        int ciclosParaSatisfacerES,
        String idBuffer,
        int ciclosPorElemento,
        int elementosRequeridos) {

    /** Valor de idComputador cuando el usuario deja que ÁvilaOS elija. */
    public static final int AUTOMATICO = -1;
}
