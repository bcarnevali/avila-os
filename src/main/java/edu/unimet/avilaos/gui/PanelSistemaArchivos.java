package edu.unimet.avilaos.gui;

import edu.unimet.avilaos.archivos.Archivo;
import edu.unimet.avilaos.archivos.Directorio;
import edu.unimet.avilaos.archivos.EntradaSistemaArchivos;
import edu.unimet.avilaos.archivos.Permisos;
import edu.unimet.avilaos.archivos.SistemaArchivos;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.ModoUsuario;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class PanelSistemaArchivos extends JPanel {

    public interface GuardadorJson {
        void guardar(File destino) throws Exception;
    }

    private SistemaArchivos sistema;
    private SolicitanteOperaciones solicitante;
    private GuardadorJson guardadorJson;

    private ModoUsuario modoActual = ModoUsuario.ADMINISTRADOR;
    private String usuarioActual = "admin";

    private final JButton btnCrearArchivo = new JButton("Crear archivo");
    private final JButton btnCrearDirectorio = new JButton("Crear directorio");
    private final JButton btnLeer = new JButton("Leer");
    private final JButton btnRenombrar = new JButton("Renombrar");
    private final JButton btnEliminar = new JButton("Eliminar");
    private final JButton btnGuardarJson = new JButton("Guardar JSON...");

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

        JPanel barraAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barraAcciones.setBorder(BorderFactory.createTitledBorder("Operaciones de E/S y persistencia"));
        barraAcciones.add(btnCrearArchivo);
        barraAcciones.add(btnCrearDirectorio);
        barraAcciones.add(btnLeer);
        barraAcciones.add(btnRenombrar);
        barraAcciones.add(btnEliminar);
        barraAcciones.add(btnGuardarJson);

        configurarBotones();
        add(barraAcciones, BorderLayout.NORTH);

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

        sincronizarModoGlobal(true, "admin");
    }

    public void sincronizarModoGlobal(boolean esAdmin, String usuario) {
        this.modoActual = esAdmin ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
        this.usuarioActual = esAdmin ? "admin" : usuario;
        btnCrearDirectorio.setEnabled(esAdmin);
        btnGuardarJson.setEnabled(esAdmin && sistema != null);
        ultimaVista = null;
        refrescar();
        actualizarDetalleSeleccion();
    }

    public void setGuardadorJson(GuardadorJson guardadorJson) {
        this.guardadorJson = guardadorJson;
    }

    public void asociar(SistemaArchivos sistema) {
        this.sistema = sistema;
        ultimaVista = null;
        btnGuardarJson.setEnabled(modoActual == ModoUsuario.ADMINISTRADOR && sistema != null);
        refrescar();
    }

    public void setSolicitanteOperaciones(SolicitanteOperaciones solicitante) {
        this.solicitante = solicitante;
    }

    private void configurarBotones() {
        btnCrearArchivo.addActionListener(e -> accionCrear(false));
        btnCrearDirectorio.addActionListener(e -> accionCrear(true));
        btnLeer.addActionListener(e -> accionLeer());
        btnRenombrar.addActionListener(e -> accionRenombrar());
        btnEliminar.addActionListener(e -> accionEliminar());
        btnGuardarJson.addActionListener(e -> accionGuardarJson());
    }

    private void accionGuardarJson() {
        if (!verificarSistemaListo()) return;
        if (guardadorJson == null) {
            mostrarError("El guardado de JSON no está conectado.");
            return;
        }
        JFileChooser selector = new JFileChooser(new File(System.getProperty("user.dir"), "ejemplos"));
        selector.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".json")) {
            destino = new File(destino.getPath() + ".json");
        }
        try {
            guardadorJson.guardar(destino);
            JOptionPane.showMessageDialog(this, "Sistema de archivos guardado en:\n" + destino.getName(),
                    "Guardado exitoso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void accionCrear(boolean esDirectorio) {
        if (!verificarSistemaListo()) return;

        if (esDirectorio && getModoActual() != ModoUsuario.ADMINISTRADOR) {
            mostrarError("Solo el administrador puede crear directorios.");
            return;
        }
        Directorio padre = obtenerDirectorioDestino();
        String motivo = Permisos.motivoParaCrear(getModoActual(), getUsuarioActual(), padre);
        if (motivo != null) {
            mostrarError(motivo);
            return;
        }

        String tipoTexto = esDirectorio ? "directorio" : "archivo";
        String nombre = JOptionPane.showInputDialog(this,
                "Nombre del nuevo " + tipoTexto + " dentro de '" + padre.getRuta() + "':",
                "Crear " + tipoTexto,
                JOptionPane.QUESTION_MESSAGE);
        if (nombre == null) return;
        nombre = nombre.trim();
        if (nombre.isEmpty() || nombre.contains("/")) {
            mostrarError("El nombre no puede estar vacío ni contener '/'.");
            return;
        }

        String rutaNueva = construirRutaHijo(padre, nombre);
        if (sistema.buscar(rutaNueva) != null) {
            mostrarError("Ya existe un elemento en '" + rutaNueva + "'.");
            return;
        }

        int bloques = 0;
        if (!esDirectorio) {
            String bloquesTxt = JOptionPane.showInputDialog(this,
                    "Tamaño en bloques para '" + rutaNueva + "':",
                    "Crear archivo",
                    JOptionPane.QUESTION_MESSAGE);
            if (bloquesTxt == null) return;
            try {
                bloques = Integer.parseInt(bloquesTxt.trim());
                if (bloques < 1) {
                    mostrarError("El tamaño en bloques debe ser al menos 1.");
                    return;
                }
            } catch (NumberFormatException ex) {
                mostrarError("Debes ingresar un número entero válido de bloques.");
                return;
            }
        }

        enviarSolicitud(OperacionES.CREAR, rutaNueva, bloques, null);
    }

    private void accionLeer() {
        if (!verificarSistemaListo()) return;

        EntradaSistemaArchivos sel = obtenerSeleccion();
        if (sel == null) {
            mostrarError("Selecciona un archivo o directorio para leer.");
            return;
        }
        String motivo = Permisos.motivoParaLeer(getModoActual(), getUsuarioActual(), sel);
        if (motivo != null) {
            mostrarError(motivo);
            return;
        }

        enviarSolicitud(OperacionES.LEER, sel.getRuta(), 0, null);
    }

    private void accionRenombrar() {
        if (!verificarSistemaListo()) return;

        EntradaSistemaArchivos sel = obtenerSeleccion();
        if (sel == null) {
            mostrarError("Selecciona un elemento para renombrar.");
            return;
        }
        String motivo = Permisos.motivoParaRenombrar(getModoActual(), getUsuarioActual(), sel);
        if (motivo != null) {
            mostrarError(motivo);
            return;
        }

        String nuevoNombre = JOptionPane.showInputDialog(this,
                "Nuevo nombre para '" + sel.getNombre() + "':",
                "Renombrar",
                JOptionPane.QUESTION_MESSAGE);
        if (nuevoNombre == null) return;
        nuevoNombre = nuevoNombre.trim();
        if (nuevoNombre.isEmpty() || nuevoNombre.contains("/")) {
            mostrarError("El nuevo nombre no puede estar vacío ni contener '/'.");
            return;
        }
        if (sel.getPadre() != null && sel.getPadre().buscarHijo(nuevoNombre) != null) {
            mostrarError("Ya existe '" + nuevoNombre + "' en '" + sel.getPadre().getRuta() + "'.");
            return;
        }

        enviarSolicitud(OperacionES.ACTUALIZAR, sel.getRuta(), 0, nuevoNombre);
    }

    private void accionEliminar() {
        if (!verificarSistemaListo()) return;

        EntradaSistemaArchivos sel = obtenerSeleccion();
        if (sel == null) {
            mostrarError("Selecciona un elemento para eliminar.");
            return;
        }
        String motivo = Permisos.motivoParaEliminar(getModoActual(), getUsuarioActual(), sel);
        if (motivo != null) {
            mostrarError(motivo);
            return;
        }

        int conf = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas eliminar '" + sel.getRuta() + "'?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        enviarSolicitud(OperacionES.ELIMINAR, sel.getRuta(), 0, null);
    }

    private boolean verificarSistemaListo() {
        if (sistema == null) {
            mostrarError("No hay ningún sistema de archivos cargado.");
            return false;
        }
        return true;
    }

    private Directorio obtenerDirectorioDestino() {
        EntradaSistemaArchivos sel = obtenerSeleccion();
        if (sel instanceof Directorio) {
            return (Directorio) sel;
        }
        if (sel != null && sel.getPadre() != null) {
            return sel.getPadre();
        }
        return sistema.getRaiz();
    }

    private String construirRutaHijo(Directorio padre, String nombre) {
        String base = padre.getRuta();
        return "/".equals(base) ? "/" + nombre : base + "/" + nombre;
    }

    private void enviarSolicitud(OperacionES op, String ruta, int bloques, String nuevoNombre) {
        if (solicitante != null) {
            solicitante.solicitarOperacion(op, ruta, bloques, nuevoNombre);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Solicitud lista para enviar al kernel:\nOperación: " + op + "\nRuta: " + ruta,
                    "Solicitud de E/S",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Operación no permitida", JOptionPane.WARNING_MESSAGE);
    }

    private String ultimaVista;

    public void refrescar() {
        if (sistema == null) {
            return;
        }
        EntradaSistemaArchivos seleccion = obtenerSeleccion();
        String rutaSeleccion = seleccion == null ? null : seleccion.getRuta();
        DefaultMutableTreeNode temporal = new DefaultMutableTreeNode("/");
        construirNodos(sistema.getRaiz(), temporal);
        StringBuilder firma = new StringBuilder(getModoActual().name()).append(getUsuarioActual());
        firmar(temporal, firma);
        String actual = firma.toString();
        if (actual.equals(ultimaVista)) return;
        ultimaVista = actual;
        nodoRaizVisual.removeAllChildren();
        construirNodos(sistema.getRaiz(), nodoRaizVisual);
        modeloArbol.reload();
        for (int i = 0; i < arbol.getRowCount(); i++) {
            arbol.expandRow(i);
        }
        modeloTabla.actualizar(filtrarArchivosVisibles(sistema.getArchivos()));
        if (rutaSeleccion != null) {
            for (int i = 0; i < arbol.getRowCount(); i++) {
                TreePath path = arbol.getPathForRow(i);
                Object item = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                if (item instanceof ItemArbol nodo && rutaSeleccion.equals(nodo.entrada.getRuta())) {
                    arbol.setSelectionPath(path); break;
                }
            }
        }
        actualizarDetalleSeleccion();
    }

    private void firmar(DefaultMutableTreeNode nodo, StringBuilder firma) {
        Object item = nodo.getUserObject();
        if (item instanceof ItemArbol n) {
            EntradaSistemaArchivos e = n.entrada;
            firma.append('|').append(e.getRuta()).append('|').append(e.getDueno()).append('|').append(e.getPermiso());
            if (e instanceof Archivo a) firma.append('|').append(a.getTamanoBloques()).append('|').append(a.getPrimerBloque());
        }
        for (int i = 0; i < nodo.getChildCount(); i++) firmar((DefaultMutableTreeNode) nodo.getChildAt(i), firma);
    }

    private void construirNodos(Directorio dir, DefaultMutableTreeNode nodoPadre) {
        ListaEnlazada<EntradaSistemaArchivos> hijos = dir.getHijos();

        for (int i = 0; i < hijos.tamano(); i++) {
            EntradaSistemaArchivos hijo = hijos.obtener(i);
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
        return modoActual;
    }

    public String getUsuarioActual() {
        return usuarioActual;
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