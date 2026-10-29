package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.Planificador;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class VentanaPrincipal extends JFrame {

    private static final String CONFIGURACION = "configuracion";
    private static final String CREAR_PROCESO = "crearProceso";
    private static final String SIMULACION = "simulacion";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contenedor = new JPanel(cardLayout);

    private final VistaConfiguracion vistaConfiguracion;
    private final VistaCrearProceso vistaCrearProceso;
    private final VistaSimulacion vistaSimulacion;

    public VentanaPrincipal() {
        setTitle("ÁvilaOS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        vistaConfiguracion = new VistaConfiguracion();
        vistaCrearProceso = new VistaCrearProceso();
        vistaSimulacion = new VistaSimulacion();

        contenedor.add(vistaConfiguracion, CONFIGURACION);
        contenedor.add(vistaCrearProceso, CREAR_PROCESO);
        contenedor.add(vistaSimulacion, SIMULACION);

        JPanel barra = new JPanel();
        barra.add(crearBoton("Configuración", CONFIGURACION));
        barra.add(crearBoton("Crear proceso", CREAR_PROCESO));
        barra.add(crearBoton("Simulación", SIMULACION));

        add(barra, BorderLayout.NORTH);
        add(contenedor, BorderLayout.CENTER);
    }

    /**
     * Conecta el creador de procesos con la vista correspondiente.
     */
    public void setCreador(CreadorProcesos creador) {
        if (vistaCrearProceso != null) {
            vistaCrearProceso.setCreador(creador);
        }
    }

    private JButton crearBoton(String texto, String vista) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> cardLayout.show(contenedor, vista));
        return boton;
    }
}