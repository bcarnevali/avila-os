package edu.unimet.avilaos.kernel;

import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.MotivoBloqueo;
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import java.util.concurrent.atomic.AtomicInteger;

public class PCB {

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    private final int id;
    private final String nombre;
    private final TipoProceso tipo;
    private final int prioridad;
    private final int memoriaRequerida;
    private final int instruccionesTotales;
    private final long cicloLlegada;

    private int idComputador;
    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;
    private int deadlineRestante;
    private int pc;
    private int mar;

    public PCB(String nombre, TipoProceso tipo, int prioridad, int memoriaRequerida,
               int instruccionesTotales, int deadline, int idComputador, long cicloLlegada) {
        this.id = SIGUIENTE_ID.getAndIncrement();
        this.nombre = nombre;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoriaRequerida = memoriaRequerida;
        this.instruccionesTotales = instruccionesTotales;
        this.deadlineRestante = deadline;
        this.idComputador = idComputador;
        this.cicloLlegada = cicloLlegada;
        this.estado = EstadoProceso.NUEVO;
        this.motivoBloqueo = MotivoBloqueo.NINGUNO;
        this.pc = 0;
        this.mar = 0;
    }

    public int getInstruccionesRestantes() {
        return instruccionesTotales - pc;
    }

    public boolean terminoInstrucciones() {
        return pc >= instruccionesTotales;
    }

    // Getters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoriaRequerida() { return memoriaRequerida; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public long getCicloLlegada() { return cicloLlegada; }
    public int getIdComputador() { return idComputador; }
    public EstadoProceso getEstado() { return estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public int getDeadlineRestante() { return deadlineRestante; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }

    // Setters de lo que cambia durante la simulación
    public void setIdComputador(int idComputador) { this.idComputador = idComputador; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void setMotivoBloqueo(MotivoBloqueo motivo) { this.motivoBloqueo = motivo; }
    public void setDeadlineRestante(int deadline) { this.deadlineRestante = deadline; }
    public void setPc(int pc) { this.pc = pc; }
    public void setMar(int mar) { this.mar = mar; }

    @Override
    public String toString() {
        return "P" + id + " (" + nombre + ")";
    }
}
