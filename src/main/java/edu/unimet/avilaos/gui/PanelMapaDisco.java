package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.disco.PlanificadorDisco;
import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Mapa del disco: un cuadro por bloque, coloreado según el archivo que lo ocupa
 * (gris claro si está libre, oscuro con "S" si es del área de intercambio). El
 * bloque del cabezal se marca en rojo y el bloque al que va, en naranja. Al pasar
 * el mouse sobre un bloque se ve quién lo ocupa y cuál es el siguiente de su cadena.
 */
public class PanelMapaDisco extends JPanel {

    private static final int COLUMNAS = 20;

    private final JLabel lblCabezal = new JLabel(" ");
    private final JLabel lblEspacio = new JLabel(" ");
    private final Cuadricula cuadricula = new Cuadricula();

    // Última foto del disco (se lee en actualizar, se dibuja en paint)
    private String[] ocupantes = new String[0];
    private int[] siguientes = new int[0];
    private boolean[] swap = new boolean[0];
    private int cabezal = -1;
    private int destino = -1;

    public PanelMapaDisco() {
        super(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Disco (SD)"));

        JPanel encabezado = new JPanel(new BorderLayout());
        lblCabezal.setFont(lblCabezal.getFont().deriveFont(Font.BOLD, 13f));
        encabezado.add(lblCabezal, BorderLayout.NORTH);
        encabezado.add(lblEspacio, BorderLayout.SOUTH);

        add(encabezado, BorderLayout.NORTH);
        add(cuadricula, BorderLayout.CENTER);
        add(crearLeyenda(), BorderLayout.SOUTH);
    }

    /** Lee el estado del disco. Se llama desde el refresco de la interfaz. */
    public void actualizar(Disco disco) {
        if (disco == null) {
            return;
        }
        PlanificadorDisco plan = disco.getPlanificador();
        ocupantes = disco.getOcupantes();
        siguientes = disco.getSiguientes();
        swap = disco.getMarcasSwap();
        cabezal = plan.getPosicion();
        SolicitudES actual = plan.getActual();
        destino = actual == null ? -1 : actual.getBloque();

        String flecha = plan.getDireccion() == DireccionCabezal.ARRIBA
                ? "↑ hacia los bloques mayores" : "↓ hacia los bloques menores";
        lblCabezal.setText("Cabezal en el bloque " + cabezal + "  " + flecha
                + "   ·   " + plan.getVelocidad() + " posiciones por ciclo"
                + (actual == null
                        ? (plan.getCola().estaVacia() ? "   ·   disco libre" : "   ·   elige la siguiente solicitud")
                        : "   ·   atendiendo " + actual.getOperacion() + " de " + actual.getSolicitante()
                        + " (bloque " + actual.getBloque() + ")"));
        lblEspacio.setText("Bloques libres: " + disco.getBloquesLibres() + " de " + disco.getTotalBloques()
                + "   ·   Disponibles para archivos: " + disco.getBloquesDisponiblesParaArchivos()
                + "   ·   Swap usado: " + disco.getBloquesSwapUsados() + " de " + disco.getTamanoSwap());
        cuadricula.repaint();
    }

    private JPanel crearLeyenda() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.add(muestra(ColoresArchivos.LIBRE, ColoresArchivos.BORDE_LIBRE, "Libre"));
        p.add(muestra(ColoresArchivos.colorDe("/ejemplo"), Color.GRAY, "Archivo (un color por archivo)"));
        p.add(muestra(ColoresArchivos.SWAP, ColoresArchivos.SWAP, "Proceso suspendido (swap)"));
        p.add(muestra(Color.WHITE, ColoresArchivos.CABEZAL, "Cabezal"));
        p.add(muestra(Color.WHITE, ColoresArchivos.DESTINO, "Bloque destino"));
        return p;
    }

    private JLabel muestra(Color relleno, Color borde, String texto) {
        JLabel l = new JLabel(texto);
        l.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                g.setColor(relleno);
                g.fillRect(x, y, 14, 14);
                ((Graphics2D) g).setStroke(new BasicStroke(2f));
                g.setColor(borde);
                g.drawRect(x, y, 14, 14);
            }
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        });
        return l;
    }

    /** La cuadrícula de bloques. */
    private class Cuadricula extends JComponent {

        Cuadricula() {
            setPreferredSize(new Dimension(COLUMNAS * 34, 10 * 26));
            setToolTipText(""); // activa los tooltips por bloque
        }

        private int lado() {
            return Math.max(10, (getWidth() - 2) / COLUMNAS);
        }

        private int altoCelda(int lado) {
            int filas = Math.max(1, (ocupantes.length + COLUMNAS - 1) / COLUMNAS);
            return Math.max(10, Math.min(28, (getHeight() - 2) / filas));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int ancho = lado();
            int alto = altoCelda(ancho);
            Font fuente = getFont().deriveFont(Math.max(8f, Math.min(11f, alto * 0.45f)));
            g.setFont(fuente);
            FontMetrics fm = g.getFontMetrics();

            for (int b = 0; b < ocupantes.length; b++) {
                int x = 1 + (b % COLUMNAS) * ancho;
                int y = 1 + (b / COLUMNAS) * alto;
                boolean libre = ocupantes[b] == null;
                Color relleno = libre ? ColoresArchivos.LIBRE
                        : swap[b] ? ColoresArchivos.SWAP : ColoresArchivos.colorDe(ocupantes[b]);
                g.setColor(relleno);
                g.fillRect(x + 1, y + 1, ancho - 2, alto - 2);
                g.setStroke(new BasicStroke(1f));
                g.setColor(libre ? ColoresArchivos.BORDE_LIBRE : relleno.darker());
                g.drawRect(x + 1, y + 1, ancho - 2, alto - 2);

                String texto = swap[b] ? "S" + b : String.valueOf(b);
                g.setColor(swap[b] ? Color.WHITE : libre ? new Color(140, 145, 155) : Color.BLACK);
                g.drawString(texto, x + (ancho - fm.stringWidth(texto)) / 2,
                        y + (alto + fm.getAscent() - fm.getDescent()) / 2);

                if (b == destino && b != cabezal) {
                    g.setStroke(new BasicStroke(2.5f));
                    g.setColor(ColoresArchivos.DESTINO);
                    g.drawRect(x + 1, y + 1, ancho - 2, alto - 2);
                }
                if (b == cabezal) {
                    g.setStroke(new BasicStroke(3.5f));
                    g.setColor(ColoresArchivos.CABEZAL);
                    g.drawRect(x + 2, y + 2, ancho - 4, alto - 4);
                }
            }
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int ancho = lado();
            int alto = altoCelda(ancho);
            int col = (e.getX() - 1) / ancho;
            int fila = (e.getY() - 1) / alto;
            if (col < 0 || col >= COLUMNAS) {
                return null;
            }
            int b = fila * COLUMNAS + col;
            if (b < 0 || b >= ocupantes.length) {
                return null;
            }
            if (ocupantes[b] == null) {
                return "Bloque " + b + ": libre";
            }
            String sig = siguientes[b] == Disco.SIN_SIGUIENTE ? "fin de la cadena" : "siguiente → " + siguientes[b];
            return "Bloque " + b + ": " + ocupantes[b] + (swap[b] ? " (swap)" : "") + "  ·  " + sig;
        }
    }
}
