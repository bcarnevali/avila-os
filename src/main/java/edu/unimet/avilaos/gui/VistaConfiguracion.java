package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.configuracion.ReceptorConfiguracion;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Selecciona los JSON; el controlador valida y construye el sistema. */
public class VistaConfiguracion extends JPanel {
    private File procesos, archivos;
    private ReceptorConfiguracion receptor;
    private final JLabel lblProcesos = new JLabel("Sin seleccionar");
    private final JLabel lblArchivos = new JLabel("Sin seleccionar");
    private final JButton aplicar = new JButton("Aplicar y reiniciar");
    private final JButton guardar = new JButton("Guardar sistema de archivos...");

    public VistaConfiguracion() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel carga = new JPanel(new GridLayout(2, 1, 10, 10));
        carga.add(selector("JSON de procesos", lblProcesos, true));
        carga.add(selector("JSON de archivos", lblArchivos, false));
        add(carga, BorderLayout.NORTH);
        JPanel acciones = new JPanel();
        acciones.add(aplicar); acciones.add(guardar);
        add(acciones, BorderLayout.SOUTH);
        aplicar.setEnabled(false); guardar.setEnabled(false);
        aplicar.addActionListener(e -> {
            if (receptor == null) return;
            try {
                receptor.aplicarConfiguracion(procesos, archivos);
                guardar.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Configuración aplicada. Ve a Simulación e inicia el reloj.");
            } catch (Exception ex) { error(ex); }
        });
        guardar.addActionListener(e -> {
            JFileChooser selector = crearSelector();
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File destino = selector.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".json")) destino = new File(destino.getPath() + ".json");
            try { receptor.guardarSistemaArchivos(destino); }
            catch (Exception ex) { error(ex); }
        });
    }
    public void setReceptor(ReceptorConfiguracion receptor) { this.receptor = receptor; }
    private JPanel selector(String titulo, JLabel etiqueta, boolean esProcesos) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        JButton boton = new JButton("Seleccionar JSON...");
        boton.addActionListener(e -> {
            JFileChooser selector = crearSelector();
            if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File archivo = selector.getSelectedFile();
            if (esProcesos) procesos = archivo; else archivos = archivo;
            etiqueta.setText(archivo.getName());
            aplicar.setEnabled(procesos != null && archivos != null);
        });
        panel.add(etiqueta, BorderLayout.CENTER); panel.add(boton, BorderLayout.EAST);
        return panel;
    }
    private JFileChooser crearSelector() {
        JFileChooser selector = new JFileChooser(new File(System.getProperty("user.dir"), "ejemplos"));
        selector.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
        return selector;
    }
    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
