package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.PlanificadorDisco;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Selector de la política de disco (se puede cambiar mientras corre la simulación)
 * y velocidad del cabezal. Solo avisa al controlador; no toca el disco.
 */
public class PanelControlDisco extends JPanel {

    private final JComboBox<TipoPoliticaDisco> comboPolitica = new JComboBox<>(TipoPoliticaDisco.values());
    private final JSpinner spinnerVelocidad = new JSpinner(new SpinnerNumberModel(5, 1, 200, 1));
    private ControlDisco control;
    private boolean actualizando;

    public PanelControlDisco() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setBorder(BorderFactory.createTitledBorder("Planificación del disco"));
        ValidadorCampos.soloEnteros(spinnerVelocidad);
        comboPolitica.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l, Object v, int i,
                    boolean sel, boolean foco) {
                return super.getListCellRendererComponent(l, nombre((TipoPoliticaDisco) v), i, sel, foco);
            }
        });
        comboPolitica.setToolTipText("Se aplica de inmediato, aunque la simulación esté corriendo");
        comboPolitica.addActionListener(e -> {
            if (!actualizando && control != null) {
                control.cambiarPoliticaDisco((TipoPoliticaDisco) comboPolitica.getSelectedItem());
            }
        });

        JButton aplicar = new JButton("Aplicar");
        aplicar.addActionListener(e -> {
            try {
                int v = ValidadorCampos.leer(spinnerVelocidad, "Velocidad del cabezal (posiciones por ciclo)");
                if (control != null) {
                    control.cambiarVelocidadCabezal(v);
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Dato inválido", JOptionPane.WARNING_MESSAGE);
            }
        });

        add(new JLabel("Política:"));
        add(comboPolitica);
        add(new JLabel("   Velocidad del cabezal (posiciones por ciclo):"));
        add(spinnerVelocidad);
        add(aplicar);
    }

    public void setControl(ControlDisco control) {
        this.control = control;
    }

    /** Refleja la política activa sin volver a disparar el cambio. */
    public void actualizar(PlanificadorDisco plan) {
        actualizando = true;
        try {
            if (comboPolitica.getSelectedItem() != plan.getPolitica() && !comboPolitica.isPopupVisible()) {
                comboPolitica.setSelectedItem(plan.getPolitica());
            }
        } finally {
            actualizando = false;
        }
    }

    public static String nombre(TipoPoliticaDisco p) {
        return p == TipoPoliticaDisco.C_SCAN ? "C-SCAN" : p.name();
    }
}
