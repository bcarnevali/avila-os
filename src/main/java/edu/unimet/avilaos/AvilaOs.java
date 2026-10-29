package edu.unimet.avilaos;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import edu.unimet.avilaos.gui.VentanaPrincipal;
import edu.unimet.avilaos.gui.VistaSimulacion;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class AvilaOs {

    private static long cicloActual = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            VistaSimulacion vistaSimulacion = ventana.getVistaSimulacion();

            // 1. Crear Cluster pasando el PanelLog para registrar eventos
            int[] memorias = new int[]{1024, 1024};
            Cluster cluster = new Cluster(
                memorias, 
                TipoPolitica.FCFS, 
                5, 
                1000, 
                vistaSimulacion.getPanelLog()
            );

            // 2. Cargar la vista gráfica inicial de los computadores
            vistaSimulacion.cargarCluster(cluster);
            vistaSimulacion.actualizarDatos(cluster);

            // 3. TIMER DE REFRESCO DE LA INTERFAZ (Actualiza tablas y RAM cada 500ms)
            Timer timerUI = new Timer(500, e -> {
                vistaSimulacion.actualizarDatos(cluster);
            });
            timerUI.start();

            // 4. Configurar la creación de procesos
            ventana.setCreador(solicitud -> {
                PCB pcb = solicitud.aPCB(cicloActual);
                
                Computador pcDestino = cluster.getComputador(pcb.getIdComputador());
                if (pcDestino == null) {
                    pcDestino = cluster.getComputador(1);
                }

                if (pcDestino != null) {
                    pcDestino.getPlanificador().crearProceso(pcb, cicloActual);
                }
                
                // Forzar refresco inmediato de la UI al crear el proceso
                vistaSimulacion.actualizarDatos(cluster);
                
                JOptionPane.showMessageDialog(ventana, "¡Proceso " + pcb.getNombre() + " creado exitosamente!");
            });

            ventana.setVisible(true);
        });
    }
}