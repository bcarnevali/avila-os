/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package Estructuras;

/**
 *
 * @author mfbal
 */
@FunctionalInterface
public interface Comparador<T> {
    /**
     * Devuelve:
     * < 0 si 'a' tiene mayor prioridad o va antes que 'b'
     * 0 si son iguales
     * > 0 si 'a' va después que 'b'
     */
    int comparar(T a, T b);
}
    