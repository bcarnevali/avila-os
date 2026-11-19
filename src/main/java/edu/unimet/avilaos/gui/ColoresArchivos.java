package edu.unimet.avilaos.gui;

import java.awt.Color;

/**
 * Colores del mapa del disco y de la tabla de asignación. Cada archivo tiene
 * siempre el mismo color, calculado a partir de su ruta, para que el mapa y la
 * tabla coincidan.
 */
public final class ColoresArchivos {

    public static final Color LIBRE = new Color(236, 238, 241);
    public static final Color BORDE_LIBRE = new Color(205, 209, 215);
    public static final Color SWAP = new Color(84, 92, 110);
    public static final Color CABEZAL = new Color(214, 40, 40);
    public static final Color DESTINO = new Color(255, 170, 0);

    /** Paleta de tonos medios que se leen bien con texto negro encima. */
    private static final Color[] PALETA = {
        new Color(120, 180, 240), new Color(130, 205, 150), new Color(245, 180, 110),
        new Color(200, 160, 230), new Color(240, 140, 160), new Color(110, 200, 200),
        new Color(220, 200, 100), new Color(170, 190, 120), new Color(150, 170, 230),
        new Color(235, 150, 110), new Color(180, 220, 170), new Color(210, 170, 190)
    };

    private ColoresArchivos() {
    }

    /** El color de un archivo según su ruta (siempre el mismo para la misma ruta). */
    public static Color colorDe(String ruta) {
        if (ruta == null) {
            return LIBRE;
        }
        int h = 0;
        for (int i = 0; i < ruta.length(); i++) {
            h = 31 * h + ruta.charAt(i);
        }
        return PALETA[Math.floorMod(h, PALETA.length)];
    }

    /** "#RRGGBB" para mostrar el color como texto en la tabla. */
    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
