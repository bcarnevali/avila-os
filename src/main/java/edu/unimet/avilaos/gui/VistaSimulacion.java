package edu.unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class VistaSimulacion extends JPanel {

    private final PanelLog panelLog = new PanelLog();

    public VistaSimulacion() {
        setLayout(new BorderLayout());
        add(new JLabel("Vista 2: Simulación", SwingConstants.CENTER), BorderLayout.CENTER);

        panelLog.setPreferredSize(new Dimension(0, 150));
        add(panelLog, BorderLayout.SOUTH);
    }

    /** Se le pasa al Cluster como registro de eventos. */
    public PanelLog getPanelLog() {
        return panelLog;
    }
}