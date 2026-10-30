package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.sincronizacion.Buffer;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/** Estado de un buffer: semaforos, ocupacion, contenido y procesos bloqueados en el. */
public class PanelBuffers extends JPanel {

    private final JLabel lblSemaforos = new JLabel("Mutex: 1 | Llenos: 0 | Vacíos: 0");
    private final JLabel lblContenido = new JLabel("Contenido: [ ]");
    private final JLabel lblContadores = new JLabel("Producidos: 0 | Consumidos: 0");
    private final JProgressBar pbCapacidad = new JProgressBar(0, 100);
    private final ModeloTablaPCB modeloBloqueadosBuffer = new ModeloTablaPCB();

    public PanelBuffers(String nombreBuffer) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(nombreBuffer));

        JPanel panelInfo = new JPanel(new GridLayout(4, 1, 5, 5));
        panelInfo.add(lblSemaforos);
        pbCapacidad.setStringPainted(true);
        pbCapacidad.setString("Ocupación: 0 / 0");
        panelInfo.add(pbCapacidad);
        panelInfo.add(lblContenido);
        panelInfo.add(lblContadores);
        add(panelInfo, BorderLayout.NORTH);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("Procesos bloqueados en el buffer"));
        panelTabla.add(new JScrollPane(new JTable(modeloBloqueadosBuffer)), BorderLayout.CENTER);
        add(panelTabla, BorderLayout.CENTER);
    }

    /** Se llama desde el hilo de Swing en cada refresco. */
    public void actualizar(Buffer b) {
        lblSemaforos.setText("Mutex: " + b.getMutex().getValor()
                + " | Llenos: " + b.getLlenos().getValor()
                + " | Vacíos: " + b.getVacios().getValor());

        lblContadores.setText("Producidos: " + b.getProducidos() + " | Consumidos: " + b.getConsumidos());

        int ocupados = b.getOcupados();
        pbCapacidad.setMaximum(b.getCapacidad());
        pbCapacidad.setValue(ocupados);
        pbCapacidad.setString("Ocupación: " + ocupados + " / " + b.getCapacidad());

        // Cada elemento guarda el id del proceso que lo produjo
        int[] elementos = b.getElementos();
        StringBuilder sb = new StringBuilder("Contenido: [ ");
        for (int i = 0; i < elementos.length; i++) {
            sb.append("P").append(elementos[i]).append(' ');
        }
        lblContenido.setText(sb.append(']').toString());

        // Bloqueados en cualquiera de los tres semaforos del buffer
        PCB[] vacios = b.getVacios().getBloqueados();
        PCB[] llenos = b.getLlenos().getBloqueados();
        PCB[] mutex = b.getMutex().getBloqueados();
        PCB[] todos = new PCB[vacios.length + llenos.length + mutex.length];
        int k = 0;
        for (PCB p : vacios) todos[k++] = p;
        for (PCB p : llenos) todos[k++] = p;
        for (PCB p : mutex) todos[k++] = p;
        modeloBloqueadosBuffer.setDatos(todos);
    }
}
