package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.archivos.Archivo;
import edu.unimet.avilaos.archivos.Directorio;
import edu.unimet.avilaos.archivos.EntradaSistemaArchivos;
import edu.unimet.avilaos.archivos.Permisos;
import edu.unimet.avilaos.archivos.SistemaArchivos;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.ModoUsuario;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class PanelSistemaArchivos extends JPanel {

    private SistemaArchivos sistema;

    private final JComboBox<ModoUsuario> comboModo = new JComboBox<>(ModoUsuario.values());
    private final JComboBox<String> comboUsuario = new JComboBox<>(new String[]{"user1", "user2"});

    private final DefaultMutableTreeNode nodoRaizVisual = new DefaultMutableTreeNode("/");
    private final DefaultTreeModel modeloArbol = new DefaultTreeModel(nodoRaizVisual);
    private final JTree arbol = new JTree(modeloArbol);

    private final JLabel lblNombre = new JLabel("Nombre: -");
    private final JLabel lblBloques = new JLabel("Tamaño en bloques: -");
    private final JLabel lblDueno = new JLabel("Dueño: -");
    private final JLabel lblPermiso = new JLabel("Permiso: -");
    private final JLabel lblPrimerBloque = new JLabel("Primer bloque: -");

    private final ModeloTablaArchivos modeloTabla = new ModeloTablaArchivos();
    private final JTable tabla = new JTable(modeloTabla);

    public PanelSistemaArchivos() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel barraModo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraModo.setBorder(BorderFactory.createTitledBorder("Modo de acceso"));
        barraModo.add(new JLabel("Modo:"));
        barraModo.add(comboModo);
        barraModo.add(new JLabel("Usuario:"));
        barraModo.add(comboUsuario);

        comboUsuario.setEnabled(false);
        comboModo.addActionListener(e -> {
            boolean esUsuario = getModoActual() == ModoUsuario.USUARIO;
            comboUsuario.setEnabled(esUsuario);
            refrescar();
            actualizarDetalleSeleccion();
        });
        comboUsuario.addActionListener(e -> {
            refrescar();
            actualizarDetalleSeleccion();
        });

        add(barraModo, BorderLayout.NORTH);

        JScrollPane scrollArbol = new JScrollPane(arbol);
        scrollArbol.setBorder(BorderFactory.createTitledBorder("Árbol de Directorios"));
        arbol.addTreeSelectionListener(e -> actualizarDetalleSeleccion());

        JPanel panelDetalle = new JPanel(new GridLayout(5, 1, 2, 2));
        panelDetalle.setBorder(BorderFactory.createTitledBorder("Propiedades de la selección"));
        panelDetalle.add(lblNombre);
        panelDetalle.add(lblBloques);
        panelDetalle.add(lblDueno);
        panelDetalle.add(lblPermiso);
        panelDetalle.add(lblPrimerBloque);

        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        panelIzquierdo.add(scrollArbol, BorderLayout.CENTER);
        panelIzquierdo.add(panelDetalle, BorderLayout.SOUTH);

        configurarRenderizadorColor();
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Tabla de Asignación de Archivos"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, scrollTabla);
        split.setDividerLocation(340);
        add(split, BorderLayout.CENTER);
    }

    public void asociar(SistemaArchivos sistema) {
        this.sistema = sistema;
        refrescar();
    }

    public void refrescar() {
        if (sistema == null) {
            return;
        }
        nodoRaizVisual.removeAllChildren();
        construirNodos(sistema.getRaiz(), nodoRaizVisual);
        modeloArbol.reload();
        for (int i = 0; i < arbol.getRowCount(); i++) {
            arbol.expandRow(i);
        }
        modeloTabla.actualizar(filtrarArchivosVisibles(sistema.getArchivos()));
    }

    private void construirNodos(Directorio dir, DefaultMutableTreeNode nodoPadre) {
        ModoUsuario modo = getModoActual();
        String usuario = getUsuarioActual();
        ListaEnlazada<EntradaSistemaArchivos> hijos = dir.getHijos();

        for (int i = 0; i < hijos.tamano(); i++) {
            EntradaSistemaArchivos hijo = hijos.obtener(i);
            if (Permisos.motivoParaLeer(modo, usuario, hijo) != null) {
                continue;
            }
            DefaultMutableTreeNode nodoHijo = new DefaultMutableTreeNode(new ItemArbol(hijo));
            nodoPadre.add(nodoHijo);
            if (hijo instanceof Directorio) {
                construirNodos((Directorio) hijo, nodoHijo);
            }
        }
    }

    private ListaEnlazada<Archivo> filtrarArchivosVisibles(ListaEnlazada<Archivo> todos) {
        ModoUsuario modo = getModoActual();
        String usuario = getUsuarioActual();
        ListaEnlazada<Archivo> visibles = new ListaEnlazada<>();
        for (int i = 0; i < todos.tamano(); i++) {
            Archivo a = todos.obtener(i);
            if (Permisos.motivoParaLeer(modo, usuario, a) == null) {
                visibles.agregar(a);
            }
        }
        return visibles;
    }

    private void actualizarDetalleSeleccion() {
        EntradaSistemaArchivos sel = obtenerSeleccion();
        if (sel == null) {
            lblNombre.setText("Nombre: -");
            lblBloques.setText("Tamaño en bloques: -");
            lblDueno.setText("Dueño: -");
            lblPermiso.setText("Permiso: -");
            lblPrimerBloque.setText("Primer bloque: -");
            return;
        }
        lblNombre.setText("Nombre: " + sel.getNombre());
        lblBloques.setText("Tamaño en bloques: " + sel.getTamanoBloques());
        lblDueno.setText("Dueño: " + sel.getDueno());
        lblPermiso.setText("Permiso: " + (sel.getPermiso() == TipoPermiso.PRIVADO ? "Privado" : "Público"));
        if (sel instanceof Archivo) {
            lblPrimerBloque.setText("Primer bloque: " + ((Archivo) sel).getPrimerBloque());
        } else {
            lblPrimerBloque.setText("Primer bloque: N/A (Directorio)");
        }
    }

    private EntradaSistemaArchivos obtenerSeleccion() {
        if (sistema == null) return null;
        TreePath path = arbol.getSelectionPath();
        if (path == null) return sistema.getRaiz();
        DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = nodo.getUserObject();
        if (obj instanceof ItemArbol) {
            return ((ItemArbol) obj).entrada;
        }
        return sistema.getRaiz();
    }

    private void configurarRenderizadorColor() {
        tabla.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Archivo arch = modeloTabla.getArchivoEn(row);
                if (arch != null && !isSelected) {
                    c.setBackground(ColoresArchivos.colorDe(arch.getRuta()));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
    }

    public ModoUsuario getModoActual() {
        ModoUsuario m = (ModoUsuario) comboModo.getSelectedItem();
        return m != null ? m : ModoUsuario.ADMINISTRADOR;
    }

    public String getUsuarioActual() {
        if (getModoActual() == ModoUsuario.ADMINISTRADOR) {
            return "admin";
        }
        Object sel = comboUsuario.getSelectedItem();
        return sel != null ? sel.toString() : "user1";
    }

    private static class ItemArbol {
        final EntradaSistemaArchivos entrada;

        ItemArbol(EntradaSistemaArchivos entrada) {
            this.entrada = entrada;
        }

        @Override
        public String toString() {
            if (entrada instanceof Directorio) {
                return "[DIR] " + entrada.getNombre();
            }
            return entrada.getNombre();
        }
    }
}