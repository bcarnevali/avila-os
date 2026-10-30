package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.kernel.RegistroEventos;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class PanelLog extends JPanel implements RegistroEventos {

    private final JTextArea area = new JTextArea();

    public PanelLog() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Log de eventos"));
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    /** Lo llama el sistema operativo desde el hilo del reloj. */
    @Override
    public void registrar(long ciclo, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            area.append("[Ciclo " + ciclo + "] " + mensaje + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
    }
}