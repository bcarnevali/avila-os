package edu.unimet.avilaos;

import edu.unimet.avilaos.gui.VentanaPrincipal;
import edu.unimet.avilaos.hardware.MemoriaPrincipal;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.kernel.Planificador;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.metricas.Metricas;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class AvilaOs {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MemoriaPrincipal memoria = new MemoriaPrincipal(1024);
            Metricas metricas = new Metricas();
            RegistroEventos log = (ciclo, mensaje) -> System.out.println("Ciclo " + ciclo + ": " + mensaje);

            Planificador p1 = new Planificador(1, memoria, metricas, TipoPolitica.FCFS, 5, log);
            Planificador p2 = new Planificador(2, memoria, metricas, TipoPolitica.FCFS, 5, log);

            VentanaPrincipal ventana = new VentanaPrincipal();

            ventana.setCreador(solicitud -> {
                long cicloActual = 0;
                PCB pcb = solicitud.aPCB(cicloActual);

                if (pcb.getIdComputador() == 2) {
                    p2.crearProceso(pcb, cicloActual);
                } else {
                    p1.crearProceso(pcb, cicloActual);
                }

                JOptionPane.showMessageDialog(ventana, "¡Proceso " + pcb.getNombre() + " creado exitosamente!");
            });

            ventana.setVisible(true);
        });
    }
}