package edu.unimet.avilaos.gui;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class VentanaPrincipal extends JFrame {

    private final VistaConfiguracion vistaConfiguracion = new VistaConfiguracion();
    private final VistaCrearProceso vistaCrearProceso = new VistaCrearProceso();
    private final VistaSimulacion vistaSimulacion = new VistaSimulacion();
    private final PanelControles panelControles = new PanelControles();

    private JDialog dialogoConfig;
    private JDialog dialogoCrear;

    public VentanaPrincipal() {
        setTitle("ÁvilaOS - Simulador de Sistema Operativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        add(panelControles, BorderLayout.NORTH);
        add(vistaSimulacion, BorderLayout.CENTER);

        panelControles.getBotonConfiguracion().addActionListener(e -> mostrarDialogoConfiguracion());
        panelControles.getBotonCrearProceso().addActionListener(e -> mostrarDialogoCrearProceso());
    }

    public void mostrarDialogoConfiguracion() {
        if (dialogoConfig == null) {
            dialogoConfig = new JDialog(this, "Configuración inicial (Cargar los 2 JSON obligatorios)", true);
            dialogoConfig.setContentPane(vistaConfiguracion);
            dialogoConfig.pack();
            dialogoConfig.setMinimumSize(new java.awt.Dimension(560, 280));
            dialogoConfig.setLocationRelativeTo(this);
        }
        dialogoConfig.setVisible(true);
    }

    public void cerrarDialogoConfiguracion() {
        if (dialogoConfig != null && dialogoConfig.isVisible()) {
            dialogoConfig.setVisible(false);
        }
    }

    public void mostrarDialogoCrearProceso() {
        if (dialogoCrear == null) {
            dialogoCrear = new JDialog(this, "Crear nuevo proceso", true);
            dialogoCrear.setContentPane(vistaCrearProceso);
            dialogoCrear.pack();
            dialogoCrear.setLocationRelativeTo(this);
        }
        dialogoCrear.setVisible(true);
    }

    public void setCreador(CreadorProcesos creador) {
        if (vistaCrearProceso != null) {
            vistaCrearProceso.setCreador(creador);
        }
    }

    public VistaConfiguracion getVistaConfiguracion() { return vistaConfiguracion; }
    public VistaCrearProceso getVistaCrearProceso() { return vistaCrearProceso; }
    public VistaSimulacion getVistaSimulacion() { return vistaSimulacion; }
    public PanelControles getPanelControles() { return panelControles; }
}