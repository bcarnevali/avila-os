package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * Lo que el panel del disco le pide al sistema cuando el usuario cambia algo.
 * Lo implementa el controlador, así la vista no toca el disco directamente.
 */
public interface ControlDisco {

    void cambiarPoliticaDisco(TipoPoliticaDisco politica);

    void cambiarVelocidadCabezal(int posicionesPorCiclo);
}
