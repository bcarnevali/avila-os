package edu.unimet.avilaos;

import edu.unimet.avilaos.controlador.ControladorSimulacion;
import edu.unimet.avilaos.gui.VentanaPrincipal;
import javax.swing.SwingUtilities;

public class AvilaOs {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            new ControladorSimulacion(
                ventana.getVistaCrearProceso(),
                ventana.getVistaSimulacion(),
                ventana.getPanelControles()
            );
            ventana.setVisible(true);
        });
    }
}