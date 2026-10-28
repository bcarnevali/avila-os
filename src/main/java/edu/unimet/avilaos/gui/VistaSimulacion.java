package edu.unimet.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class VistaSimulacion extends JPanel {

    private final JPanel panelComputadores = new JPanel(new GridLayout(1, 2, 10, 10));
    private final PanelLog panelLog = new PanelLog();

    public VistaSimulacion() {
        setLayout(new BorderLayout(5, 5));

        // Área central con scroll para soportar 2 o más computadores
        JScrollPane scrollComputadores = new JScrollPane(panelComputadores);
        add(scrollComputadores, BorderLayout.CENTER);

        // Panel del log en la parte inferior
        panelLog.setPreferredSize(new Dimension(0, 180));
        add(panelLog, BorderLayout.SOUTH);
    }

    public void agregarPanelComputador(JPanel pcPanel) {
        panelComputadores.add(pcPanel);
        panelComputadores.revalidate();
        panelComputadores.repaint();
    }

    public PanelLog getPanelLog() {
        return panelLog;
    }
}