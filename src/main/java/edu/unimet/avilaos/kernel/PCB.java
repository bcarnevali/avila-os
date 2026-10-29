package edu.unimet.avilaos.kernel;

import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.MotivoBloqueo;
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bloque de control del proceso (PCB). En ÁvilaOS cada proceso tiene un único
 * hilo, así que el PCB integra también el TCB (relación 1:1).
 * Guarda el contexto del proceso para poder sacarlo de la CPU y reanudarlo.
 */
public class PCB {

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    // ---------- Datos fijos ----------
    private final int id;
    private final String nombre;
    private final TipoProceso tipo;
    private final int prioridad;
    private final int memoriaRequerida;
    private final int instruccionesTotales;
    private final long cicloLlegada;

    // ---------- Contexto de ejecución ----------
    private int idComputador;
    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;
    private int deadlineRestante;
    private int pc;
    private int mar;

    // ---------- CPU bound / I/O bound ----------
    /** Cada cuántas instrucciones pide una operación de E/S (0 = nunca). */
    private int ciclosEntreES;
    /** Cuántos ciclos tarda en satisfacerse cada operación de E/S. */
    private int ciclosParaSatisfacerES;

    // ---------- Productor / consumidor ----------
    private String idBuffer;
    /** Cada cuántos ciclos produce o consume un elemento. */
    private int ciclosPorElemento;
    /** Cuántos elementos debe producir o consumir para terminar. */
    private int elementosRequeridos;
    private int elementosProcesados;

    // ---------- Datos para métricas ----------
    /** Ciclo en que entró a la CPU por primera vez (-1 = todavía no). */
    private long cicloPrimeraEjecucion;
    /** Ciclo en que entró a la cola de listos por última vez. */
    private long cicloEntradaListos;
    /** Ciclos totales que pasó esperando en la cola de listos. */
    private long ciclosEsperaTotal;
    /** Ciclo en que se bloqueó por última vez. */
    private long cicloInicioBloqueo;
    /** Ciclos que lleva en la CPU en el quantum actual (Round Robin). */
    private int ciclosEnQuantum;

    public PCB(String nombre, TipoProceso tipo, int prioridad, int memoriaRequerida,
               int instruccionesTotales, int deadline, int idComputador, long cicloLlegada) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del proceso no puede estar vacío");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de proceso es obligatorio");
        }
        if (memoriaRequerida <= 0) {
            throw new IllegalArgumentException("La memoria requerida debe ser mayor que 0");
        }
        if (instruccionesTotales <= 0) {
            throw new IllegalArgumentException("La cantidad de instrucciones debe ser mayor que 0");
        }
        if (deadline <= 0) {
            throw new IllegalArgumentException("El deadline debe ser mayor que 0");
        }
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
        this.cicloPrimeraEjecucion = -1;
        this.cicloEntradaListos = -1;
        this.cicloInicioBloqueo = -1;
    }

    // ---------- Configuración según el tipo ----------

    /** Para procesos CPU bound o I/O bound. */
    public void configurarES(int ciclosEntreES, int ciclosParaSatisfacerES) {
        if (tipo != TipoProceso.CPU_BOUND && tipo != TipoProceso.IO_BOUND) {
            throw new IllegalStateException("Solo los procesos CPU bound o I/O bound hacen E/S");
        }
        if (ciclosEntreES < 0 || ciclosParaSatisfacerES < 0) {
            throw new IllegalArgumentException("Los ciclos de E/S no pueden ser negativos");
        }
        this.ciclosEntreES = ciclosEntreES;
        this.ciclosParaSatisfacerES = ciclosParaSatisfacerES;
    }

    /** Para procesos productores o consumidores. */
    public void configurarBuffer(String idBuffer, int ciclosPorElemento, int elementosRequeridos) {
        if (tipo != TipoProceso.PRODUCTOR && tipo != TipoProceso.CONSUMIDOR) {
            throw new IllegalStateException("Solo los productores y consumidores usan un buffer");
        }
        if (idBuffer == null || idBuffer.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el buffer");
        }
        if (ciclosPorElemento <= 0 || elementosRequeridos <= 0) {
            throw new IllegalArgumentException("Los ciclos por elemento y los elementos requeridos deben ser mayores que 0");
        }
        this.idBuffer = idBuffer;
        this.ciclosPorElemento = ciclosPorElemento;
        this.elementosRequeridos = elementosRequeridos;
    }

    // ---------- Consultas ----------

    public boolean esProductorOConsumidor() {
        return tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR;
    }

    public int getInstruccionesRestantes() {
        return Math.max(0, instruccionesTotales - pc);
    }

    public boolean terminoInstrucciones() {
        return pc >= instruccionesTotales;
    }

    /** Un productor o consumidor termina al procesar sus elementos; el resto, al acabar sus instrucciones. */
    public boolean haTerminado() {
        if (esProductorOConsumidor()) {
            return elementosProcesados >= elementosRequeridos;
        }
        return terminoInstrucciones();
    }

    /** true si en la instrucción actual le toca pedir E/S. */
    public boolean leTocaES() {
        return ciclosEntreES > 0 && pc > 0 && pc % ciclosEntreES == 0;
    }

    /** true si en la instrucción actual le toca producir o consumir un elemento. */
    public boolean leTocaBuffer() {
        return esProductorOConsumidor() && pc > 0 && pc % ciclosPorElemento == 0;
    }

    // ---------- Cambios durante la simulación ----------

    /** Avanza una instrucción: el PC y el MAR suben una unidad. */
    public void avanzarInstruccion() {
        pc++;
        mar++;
    }

    public void registrarElementoProcesado() {
        elementosProcesados++;
    }

    /** Resta un ciclo al deadline. Devuelve true si llegó a 0. */
    public boolean descontarDeadline() {
        if (deadlineRestante > 0) {
            deadlineRestante--;
        }
        return deadlineRestante == 0;
    }

    public void marcarEntradaListos(long ciclo) {
        cicloEntradaListos = ciclo;
    }

    /** Se llama cuando sale de listos hacia la CPU: suma la espera y anota la primera ejecución. */
    public void marcarSalidaListos(long ciclo) {
        if (cicloEntradaListos >= 0) {
            ciclosEsperaTotal += ciclo - cicloEntradaListos;
            cicloEntradaListos = -1;
        }
        if (cicloPrimeraEjecucion < 0) {
            cicloPrimeraEjecucion = ciclo;
        }
        ciclosEnQuantum = 0;
    }

    public void marcarInicioBloqueo(long ciclo, MotivoBloqueo motivo) {
        estado = EstadoProceso.BLOQUEADO;
        motivoBloqueo = motivo;
        cicloInicioBloqueo = ciclo;
    }

    /** Se llama al desbloquearse. Devuelve cuántos ciclos estuvo bloqueado. */
    public long marcarFinBloqueo(long ciclo) {
        long duracion = cicloInicioBloqueo >= 0 ? ciclo - cicloInicioBloqueo : 0;
        motivoBloqueo = MotivoBloqueo.NINGUNO;
        cicloInicioBloqueo = -1;
        return duracion;
    }

    public void sumarCicloQuantum() {
        ciclosEnQuantum++;
    }

    // ---------- Getters ----------
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
    public int getCiclosEntreES() { return ciclosEntreES; }
    public int getCiclosParaSatisfacerES() { return ciclosParaSatisfacerES; }
    public String getIdBuffer() { return idBuffer; }
    public int getCiclosPorElemento() { return ciclosPorElemento; }
    public int getElementosRequeridos() { return elementosRequeridos; }
    public int getElementosProcesados() { return elementosProcesados; }
    public long getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public long getCiclosEsperaTotal() { return ciclosEsperaTotal; }
    public int getCiclosEnQuantum() { return ciclosEnQuantum; }

    // ---------- Setters ----------
    public void setIdComputador(int idComputador) { this.idComputador = idComputador; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void setMotivoBloqueo(MotivoBloqueo motivo) { this.motivoBloqueo = motivo; }
    public void setDeadlineRestante(int deadline) { this.deadlineRestante = deadline; }
    public void setPc(int pc) { this.pc = pc; }
    public void setMar(int mar) { this.mar = mar; }

    /** Formato del log del enunciado: P3-H1 = proceso 3, su único hilo. */
    @Override
    public String toString() {
        return "P" + id + "-H1";
    }
    
    public void retrocederInstruccion() {
    this.pc--;
    this.mar--;
    }
}
