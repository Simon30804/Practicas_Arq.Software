package Pr1;

/*
 * Observer.java
 * Contrato del patrón Observer para el foro.
 * Define la interfaz que los observadores deben implementar para recibir notificaciones de eventos.
 */
public interface Observer {
    // Método que se llama para notificar al observador de un evento.
    // El observador puede acceder al sujeto y al evento para decidir cómo manejar la notificación.
    void actualizar(Sujeto sujeto, EventoForo evento);
}