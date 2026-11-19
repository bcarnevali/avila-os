package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Demo de la pestaña del disco sin el resto del sistema: carga los archivos del
 * JSON del enunciado, ocupa algo de swap y va mandando solicitudes al disco.
 * En NetBeans: clic derecho sobre este archivo → Run File. Se puede cambiar la
 * política mientras corre y ver cómo cambia la pendiente del gráfico.
 */
public class DemoPanelDisco {

    private static final int[] BLOQUES = {95, 180, 34, 119, 11, 123, 62, 64, 150, 20, 77, 140, 5, 199, 101};
    private static long ciclo = 0;

    public static void main(String[] args) {
        Disco disco = new Disco(200, 100, 50, DireccionCabezal.ARRIBA, 5, TipoPoliticaDisco.FIFO, null);
        disco.asignarArchivoEn(11, "/sistema/boot.bin", 1);
        disco.asignarArchivoEn(34, "/usuarios/notas.txt", 3);
        disco.asignarArchivoEn(62, "/sistema/script.py", 1);
        disco.asignarArchivoEn(64, "/sistema/core.dll", 2);
        disco.asignarArchivoEn(95, "/sistema/config.sys", 1);
        disco.asignarArchivoEn(119, "/usuarios/img1.png", 1);
        disco.asignarArchivoEn(123, "/usuarios/data.csv", 2);
        disco.asignarArchivoEn(180, "/usuarios/video.mp4", 4);
        disco.asignarSwap("P7 (suspendido)", 16);

        PanelDisco panel = new PanelDisco();
        panel.setControl(new ControlDisco() {
            @Override
            public void cambiarPoliticaDisco(TipoPoliticaDisco p) {
                disco.getPlanificador().cambiarPolitica(p, ciclo);
            }

            @Override
            public void cambiarVelocidadCabezal(int v) {
                disco.getPlanificador().setVelocidad(v);
            }
        });

        String captura = args.length > 0 ? args[0] : null;
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Demo del disco");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(panel);
            f.setSize(1400, 820);
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            Timer reloj = new Timer(captura == null ? 300 : 1, null);
            reloj.addActionListener(e -> {
                ciclo++;
                // cada 3 ciclos llega una solicitud nueva
                if (ciclo % 3 == 1) {
                    int b = BLOQUES[(int) ((ciclo / 3) % BLOQUES.length)];
                    disco.getPlanificador().encolar(new SolicitudES("P" + (ciclo / 3 + 1),
                            ciclo % 2 == 0 ? OperacionES.LEER : OperacionES.ACTUALIZAR, b, 1, ciclo), ciclo);
                }
                if (captura != null && ciclo == 60) {
                    disco.getPlanificador().cambiarPolitica(TipoPoliticaDisco.SSTF, ciclo);
                }
                if (captura != null && ciclo == 120) {
                    disco.getPlanificador().cambiarPolitica(TipoPoliticaDisco.C_SCAN, ciclo);
                }
                disco.tick(ciclo);
                panel.actualizar(disco, ciclo);
                if (captura != null && ciclo == 170) {
                    reloj.stop();
                    guardar(f, captura);
                    System.exit(0);
                }
            });
            reloj.start();
        });
    }

    private static void guardar(JFrame f, String archivo) {
        try {
            BufferedImage img = new BufferedImage(f.getContentPane().getWidth(), f.getContentPane().getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            f.getContentPane().paint(img.getGraphics());
            ImageIO.write(img, "png", new File(archivo));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
