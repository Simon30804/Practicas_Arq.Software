/*
 * Sujeto.java
 * Contrato del patrón Observer para el foro.
 * Define suscripciones con intereses por tipo de evento y acceso mínimo para pull.
 */
package Pr1;

import java.util.Set;

// Interfaz Sujeto para el patrón Observer. Un sujeto puede suscribir por intereses específicos,
// permitiendo a los observadores recibir solo las notificaciones de eventos que les interesan. 
public interface Sujeto {
    void suscribir(Observer o);
    void suscribir(Observer o, Set<TipoEvento> intereses); //  El interés incorpora la noción de aspecto
    void registrarInteres(Observer o, TipoEvento interes);
    void borrar(Observer o);
    void notificar(EventoForo evento);  // Ahora el evento incluye el tipo de evento, lo que permite a los observadores filtrar las notificaciones
    Mensaje getMensaje(String idMensaje);
}
