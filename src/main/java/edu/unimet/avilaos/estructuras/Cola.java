/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.unimet.avilaos.estructuras;

/**
 *
 * @author mfbal
 */
public class Cola<T> {
    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int tamano;

    public Cola() {
        this.primero = null;
        this.ultimo = null;
        this.tamano = 0;
    }

    public void encolar(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (estaVacia()) {
            primero = nuevo;
        } else {
            ultimo.setSiguiente(nuevo);
        }
        ultimo = nuevo;
        tamano++;
    }

    public T desencolar() {
        if (estaVacia()) return null;
        T dato = primero.getDato();
        primero = primero.getSiguiente();
        if (primero == null) {
            ultimo = null;
        }
        tamano--;
        return dato;
    }

    public T verPrimero() {
        return estaVacia() ? null : primero.getDato();
    }

    public boolean eliminar(T elemento) {
        if (estaVacia()) return false;

        if (primero.getDato().equals(elemento)) {
            desencolar();
            return true;
        }

        Nodo<T> actual = primero;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getDato().equals(elemento)) {
                if (actual.getSiguiente() == ultimo) {
                    ultimo = actual;
                }
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamano--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Inserta respetando el orden. 
     * Si comparador.comparar(nuevo, existente) <= 0, avanza para ubicarse DETRÁS de los iguales.
     */
    public void insertarOrdenado(T elemento, Comparador<T> comparador) {
        Nodo<T> nuevo = new Nodo<>(elemento);

        if (estaVacia()) {
            primero = nuevo;
            ultimo = nuevo;
            tamano++;
            return;
        }

        // Si debe ir antes del primer elemento
        if (comparador.comparar(elemento, primero.getDato()) < 0) {
            nuevo.setSiguiente(primero);
            primero = nuevo;
            tamano++;
            return;
        }

        // Buscar posición adecuada (quedando detrás de los elementos con valor igual)
        Nodo<T> actual = primero;
        while (actual.getSiguiente() != null && comparador.comparar(elemento, actual.getSiguiente().getDato()) >= 0) {
            actual = actual.getSiguiente();
        }

        nuevo.setSiguiente(actual.getSiguiente());
        actual.setSiguiente(nuevo);

        if (nuevo.getSiguiente() == null) {
            ultimo = nuevo;
        }

        tamano++;
    }

    public int tamano() { return tamano; }
    public boolean estaVacia() { return tamano == 0; }
}
