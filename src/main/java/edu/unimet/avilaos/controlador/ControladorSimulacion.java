package edu.unimet.avilaos.controlador;

import edu.unimet.avilaos.archivos.*;
import edu.unimet.avilaos.cluster.*;
import edu.unimet.avilaos.configuracion.*;
import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.gui.*;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.*;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/** Conecta las vistas con el computador, sin duplicar la lógica del kernel. */
public class ControladorSimulacion implements CreadorProcesos, ReceptorConfiguracion, ControlDisco, SolicitanteOperaciones {
    private final VistaSimulacion vista;
    private final PanelControles controles;
    private Cluster cluster;
    private SistemaArchivos sistema;
    private boolean iniciado;
    private int duracionCicloActual = 500;

    public ControladorSimulacion(VistaConfiguracion configuracion, VistaCrearProceso crear,
                                 VistaSimulacion vista, PanelControles controles) {
        this.vista = vista; this.controles = controles;
        configuracion.setReceptor(this);
        crear.setCreador(this);
        crear.setCantidadComputadores(1);
        vista.getPanelDisco().setControl(this);
        vista.getPanelArchivos().setSolicitanteOperaciones(this);
        vista.getPanelArchivos().setGuardadorJson(this::guardarSistemaArchivos);
        controles.getBotonIniciar().setEnabled(false);
        controles.getBotonAplicar().setEnabled(false);
        controles.getBotonIniciar().addActionListener(e -> iniciar());
        controles.getBotonPausar().addActionListener(e -> pausarOReanudar());
        controles.getBotonAplicar().addActionListener(e -> aplicarParametros());
        controles.setOnCambioModo(this::sincronizarPermisosVisuales);
        sincronizarPermisosVisuales();
        new Timer(200, e -> refrescar()).start();
    }

    private void sincronizarPermisosVisuales() {
        boolean esAdmin = controles.getModoActual() == ModoUsuario.ADMINISTRADOR;
        vista.actualizarPermisosVisuales(esAdmin, controles.getUsuarioActual());
    }

    @Override
    public void aplicarConfiguracion(File procesos, File archivos) throws IOException {
        exigirAdministrador();
        ConfiguracionProcesos c = GestorJsonProcesos.cargar(procesos);
        GestorJsonSistemaArchivos.ResultadoCarga carga = GestorJsonSistemaArchivos.cargar(
                archivos, c.getTamanoSwap(), c.getPoliticaDisco(), vista.getPanelLog());
        Cluster nuevo = new Cluster(new int[]{c.getRam()}, c.getPolitica(), c.getQuantum(),
                c.getDuracionCicloMs(), vista.getPanelLog());
        nuevo.instalarDisco(1, carga.getDisco(), new OperacionesArchivo(carga.getSistemaArchivos(), carga.getDisco()));
        for (int i = 0; i < c.getProcesos().tamano(); i++) {
            nuevo.getComputador(1).crearProceso(c.getProcesos().obtener(i).crearPCB(1, 0), 0);
        }
        if (cluster != null) cluster.getReloj().detener();
        cluster = nuevo; sistema = carga.getSistemaArchivos(); iniciado = false;
        vista.cargarCluster(cluster);
        for (PanelComputador panel : vista.getPanelesPc()) {
            panel.setControlPolitica(this::cambiarPoliticaCpu);
        }
        vista.getPanelArchivos().asociar(sistema);
        controles.marcarJsonCargados(procesos.getName(), archivos.getName());
        controles.setDuracionCiclo(c.getDuracionCicloMs());
        duracionCicloActual = c.getDuracionCicloMs();
        controles.setQuantum(c.getQuantum());
        controles.setCiclo(0);
        controles.getBotonIniciar().setEnabled(true);
        controles.getBotonPausar().setEnabled(false);
        controles.getBotonPausar().setText("Pausar");
        sincronizarPermisosVisuales();
        refrescar();
    }

    @Override
    public void guardarSistemaArchivos(File destino) throws IOException {
        exigirAdministrador();
        exigirConfiguracion();
        if (iniciado && !cluster.getReloj().isPausado()) throw new IllegalStateException("Pausa la simulación antes de guardar.");
        GestorJsonSistemaArchivos.guardar(destino, sistema, disco());
    }

    private void exigirConfiguracion() {
        if (cluster == null) throw new IllegalStateException("Carga y aplica ambos JSON primero.");
    }

    private void exigirAdministrador() {
        if (controles.getModoActual() != ModoUsuario.ADMINISTRADOR)
            throw new IllegalArgumentException("Operación reservada al administrador.");
    }

    private Disco disco() { return cluster.getComputador(1).getDisco(); }
    private long ciclo() { return cluster.getReloj().getCicloActual(); }

    @Override
    public void crearProceso(SolicitudProceso s) {
        exigirAdministrador();
        exigirConfiguracion();
        Computador pc = cluster.getComputador(1);
        if (s.memoria() > pc.getMemoria().getCapacidad()) throw new IllegalArgumentException("La memoria requerida supera la RAM.");
        PCB pcb = new PCB(s.nombre(), s.tipo(), s.prioridad(), s.memoria(), s.instrucciones(), s.deadline(), 1, ciclo());
        pcb.configurarES(s.ciclosEntreES(), s.ciclosParaSatisfacerES());
        pc.crearProceso(pcb, ciclo());
    }

    @Override
    public void solicitarOperacion(OperacionES op, String ruta, int bloques, String nuevoNombre) {
        exigirConfiguracion();
        PCB pcb = new PCB(op + " " + ruta, TipoProceso.ES_ARCHIVO, 5, 1, 1, Integer.MAX_VALUE, 1, ciclo());
        pcb.configurarArchivo(op, ruta, bloques);
        pcb.setUsuario(controles.getUsuarioActual(), controles.getModoActual() == ModoUsuario.ADMINISTRADOR);
        pcb.setNuevoNombre(nuevoNombre);
        cluster.getComputador(1).crearProceso(pcb, ciclo());
    }

    private void cambiarPoliticaCpu(TipoPolitica politica) {
        exigirAdministrador();
        exigirConfiguracion();
        cluster.getComputador(1).getPlanificador().cambiarPolitica(politica, ciclo());
    }

    @Override
    public void cambiarPoliticaDisco(TipoPoliticaDisco politica) {
        exigirAdministrador();
        if (cluster != null) disco().getPlanificador().cambiarPolitica(politica, ciclo());
    }

    @Override
    public void cambiarVelocidadCabezal(int velocidad) {
        exigirAdministrador();
        if (cluster != null) disco().getPlanificador().setVelocidad(velocidad);
    }

    private void iniciar() {
        if (cluster == null || iniciado) return;
        cluster.iniciar(); iniciado = true;
        controles.getBotonIniciar().setEnabled(false);
        controles.getBotonPausar().setEnabled(true);
    }

    private void pausarOReanudar() {
        if (!iniciado) return;
        if (cluster.getReloj().isPausado()) {
            cluster.getReloj().reanudar(); controles.getBotonPausar().setText("Pausar");
        } else {
            cluster.getReloj().pausar(); controles.getBotonPausar().setText("Reanudar");
        }
    }

    private void aplicarParametros() {
        if (cluster == null) return;
        try {
            exigirAdministrador();
            int duracion = controles.getDuracionCiclo();
            int quantum = controles.getQuantum();
            cluster.getReloj().setDuracionCiclo(duracion);
            cluster.getComputador(1).getPlanificador().cambiarQuantum(quantum, ciclo());
            duracionCicloActual = duracion;
        } catch (IllegalArgumentException ex) {
            controles.setDuracionCiclo(duracionCicloActual);
            controles.setQuantum(cluster.getComputador(1).getPlanificador().getQuantum());
            JOptionPane.showMessageDialog(controles, ex.getMessage(), "Operación no permitida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refrescar() {
        if (cluster == null) return;
        vista.actualizarDatos(cluster);
        controles.setCiclo(ciclo());
    }

    public Cluster getCluster() { return cluster; }
}