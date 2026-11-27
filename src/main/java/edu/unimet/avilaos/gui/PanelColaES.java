package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.PlanificadorDisco;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/** Cola única de E/S del disco, con la operación de cada solicitud. */
public class PanelColaES extends JPanel {

    private final ModeloTablaColaES modelo = new ModeloTablaColaES();
    private final JLabel lblResumen = new JLabel(" ");

    public PanelColaES() {
        super(new BorderLayout(0, 4));
        setBorder(BorderFactory.createTitledBorder("Cola de E/S del disco"));
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
                boolean enCurso = modelo.esEnCurso(fila);
                if (!sel) {
                    c.setBackground(enCurso ? new Color(255, 236, 200) : Color.WHITE);
                }
                c.setFont(c.getFont().deriveFont(enCurso ? Font.BOLD : Font.PLAIN));
                return c;
            }
        });
        tabla.getColumnModel().getColumn(3).setCellRenderer(tabla.getDefaultRenderer(Object.class));
        tabla.getColumnModel().getColumn(4).setCellRenderer(tabla.getDefaultRenderer(Object.class));
        add(lblResumen, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public void actualizar(PlanificadorDisco plan, long ciclo) {
        modelo.actualizar(plan, ciclo);
        int esperando = plan.getCola().tamano();
        lblResumen.setText(plan.getActual() == null && esperando == 0
                ? "El disco está libre."
                : esperando + (esperando == 1 ? " solicitud espera" : " solicitudes esperan")
                        + "; la de color naranja es la que se está atendiendo.");
    }
}
