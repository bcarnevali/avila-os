package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class PanelBuffers extends JPanel {

    private final JLabel lblSemaforos = new JLabel("Mutex: 1 | Llenos: 0 | Vacíos: 0");
    private final JProgressBar pbCapacidad = new JProgressBar(0, 100);
    private final ModeloTablaPCB modeloBloqueadosBuffer = new ModeloTablaPCB();

    public PanelBuffers(String nombreBuffer) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(nombreBuffer));

        JPanel panelInfo = new JPanel(new GridLayout(2, 1, 5, 5));
        panelInfo.add(lblSemaforos);
        
        pbCapacidad.setStringPainted(true);
        pbCapacidad.setString("Ocupación: 0 / 0");
        panelInfo.add(pbCapacidad);

        add(panelInfo, BorderLayout.NORTH);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("Procesos Bloqueados en Buffer"));
        panelTabla.add(new JScrollPane(new JTable(modeloBloqueadosBuffer)), BorderLayout.CENTER);

        add(panelTabla, BorderLayout.CENTER);
    }

    public void actualizarEstado(int mutex, int llenos, int vacios, int ocupados, int capacidad, PCB[] bloqueados) {
        SwingUtilities.invokeLater(() -> {
            lblSemaforos.setText("Mutex: " + mutex + " | Llenos: " + llenos + " | Vacíos: " + vacios);
            pbCapacidad.setMaximum(capacidad);
            pbCapacidad.setValue(ocupados);
            pbCapacidad.setString("Ocupación: " + ocupados + " / " + capacidad);
            modeloBloqueadosBuffer.setDatos(bloqueados);
        });
    }
}