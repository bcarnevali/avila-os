
package edu.unimet.avilaos.gui;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class VistaConfiguracion extends JPanel {

    public VistaConfiguracion() {
        setLayout(new BorderLayout());
        add(new JLabel("Configuración", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
    
