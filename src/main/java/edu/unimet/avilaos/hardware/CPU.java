/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.hardware;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.ModoEjecucion;
/**
 *
 * @author mfbal
 */
public class CPU implements ComponenteReloj{
    
    private volatile PCB enEjecucion;
    private ModoEjecucion modo;

    public CPU() {
        this.modo = ModoEjecucion.SISTEMA_OPERATIVO;
    }

    @Override
    public void tick(long ciclo) {
        // La CPU no decide; el pulso es coordinado a nivel de Computador
    }

    public void ejecutarInstruccion() {
        if (enEjecucion != null) {
            enEjecucion.avanzarInstruccion();
        }
    }

    public PCB getEnEjecucion() {
        return enEjecucion;
    }

    public void setEnEjecucion(PCB enEjecucion) {
        this.enEjecucion = enEjecucion;
    }

    public ModoEjecucion getModo() {
        return modo;
    }

    public void setModo(ModoEjecucion modo) {
        this.modo = modo;
    }

    public int getPc() {
        return enEjecucion != null ? enEjecucion.getPc() : 0;
    }

    public int getMar() {
        return enEjecucion != null ? enEjecucion.getMar() : 0;
    }
    
}
