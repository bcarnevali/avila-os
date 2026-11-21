package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.modelo.enums.OperacionES;

public interface SolicitanteOperaciones {

    void solicitarOperacion(OperacionES op, String ruta, int bloques, String nuevoNombre);
}