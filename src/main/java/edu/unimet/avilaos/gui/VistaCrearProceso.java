/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.gui;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class VistaCrearProceso extends JPanel {

    public VistaCrearProceso() {
        setLayout(new BorderLayout());
        add(new JLabel("Vista 1: Crear proceso", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}