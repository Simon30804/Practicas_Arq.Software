package Pr1;

import java.util.HashSet;
import Pr1.Sujeto;
/*
 * Observer.java
 * Contrato del patrón Observer para el foro.
 * Define la interfaz que los observadores deben implementar para recibir notificaciones de eventos.
 */
public abstract class Observer implements AutoCloseable {
    HashSet<Sujeto> sujetosSuscritos = new HashSet<>(); // Para que el observador pueda gestionar sus suscripciones

    // Método que se llama para notificar al observador de un evento.
    // El observador puede acceder al sujeto y al evento para decidir cómo manejar la notificación.
    public abstract void actualizar(Sujeto sujeto, EventoForo evento);

    void avisarBorradoSujeto(Sujeto s){
        sujetosSuscritos.remove(s); // El observador borra al sujeto de su lista de suscripciones
    }

    @Override
    public void close() {
        for (Sujeto s : sujetosSuscritos){
            s.borrar(this) ; // El observador se desuscribe de todos los sujetos a los que está suscrito
        }
    }
}