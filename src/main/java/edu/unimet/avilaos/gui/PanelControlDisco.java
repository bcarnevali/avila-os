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

public class PanelControlDisco extends JPanel {

    private final JComboBox<TipoPoliticaDisco> comboPolitica = new JComboBox<>(TipoPoliticaDisco.values());
    private final JSpinner spinnerVelocidad = new JSpinner(new SpinnerNumberModel(5, 1, 200, 1));
    private final JButton botonAplicar = new JButton("Aplicar");
    private ControlDisco control;
    private boolean actualizando;
    private PlanificadorDisco planActual;

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
                try {
                    control.cambiarPoliticaDisco((TipoPoliticaDisco) comboPolitica.getSelectedItem());
                } catch (IllegalArgumentException ex) {
                    restaurarValores();
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación no permitida", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        botonAplicar.addActionListener(e -> {
            try {
                int v = ValidadorCampos.leer(spinnerVelocidad, "Velocidad del cabezal (posiciones por ciclo)");
                if (control != null) {
                    control.cambiarVelocidadCabezal(v);
                }
            } catch (IllegalArgumentException ex) {
                restaurarValores();
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación no permitida", JOptionPane.WARNING_MESSAGE);
            }
        });

        add(new JLabel("Política:"));
        add(comboPolitica);
        add(new JLabel("   Velocidad del cabezal (posiciones/ciclo):"));
        add(spinnerVelocidad);
        add(botonAplicar);
    }

    public void setControl(ControlDisco control) {
        this.control = control;
    }

    public void actualizarPermisosVisuales(boolean esAdmin) {
        comboPolitica.setEnabled(esAdmin);
        spinnerVelocidad.setEnabled(esAdmin);
        botonAplicar.setEnabled(esAdmin);
    }

    private void restaurarValores() {
        if (planActual == null) return;
        actualizando = true;
        try {
            spinnerVelocidad.setValue(planActual.getVelocidad());
            comboPolitica.setSelectedItem(planActual.getPolitica());
        } finally {
            actualizando = false;
        }
    }

    public void actualizar(PlanificadorDisco plan) {
        actualizando = true;
        try {
            if (planActual != plan) {
                planActual = plan;
                spinnerVelocidad.setValue(plan.getVelocidad());
            }

            if (comboPolitica.getSelectedItem() != plan.getPolitica()
                    && !comboPolitica.isPopupVisible()) {
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