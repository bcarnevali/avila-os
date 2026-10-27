/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package edu.unimet.avilaos;

import edu.unimet.avilaos.gui.VentanaPrincipal;
import javax.swing.SwingUtilities;

public class AvilaOs {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
