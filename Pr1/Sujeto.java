/*
 * Sujeto.java
 * Contrato del patrón Observer para el foro.
 * Define suscripciones con intereses por tipo de evento y acceso mínimo para pull.
 */
package Pr1;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
// Interfaz Sujeto para el patrón Observer. Un sujeto puede suscribir por intereses específicos,
// permitiendo a los observadores recibir solo las notificaciones de eventos que les interesan. 
public abstract class Sujeto implements AutoCloseable {
        
    protected List<Observer> observadores = new ArrayList<>();
    protected Map<Observer, Set<TipoEvento>> interesesPorObserver = new HashMap<>();
    
    protected void suscribir(Observer o){
        suscribir(o, null); // Sin intereses específicos: recibe todos los eventos.
    }

    protected void suscribir(Observer o, Set<TipoEvento> intereses){  //  El interés incorpora la noción de aspecto
        if (!observadores.contains(o)) {
            observadores.add(o);
        }

        if (intereses == null || intereses.isEmpty()) {
            interesesPorObserver.remove(o); // Sin preferencias explícitas: recibe todos los eventos.
        } else {
            interesesPorObserver.put(o, intereses);
        }
    }

    protected void registrarInteres(Observer o, TipoEvento interes){
        if (!observadores.contains(o)) {
            observadores.add(o);
        }
        Set<TipoEvento> intereses = interesesPorObserver.computeIfAbsent(
            o, k -> new java.util.HashSet<>()
        );
        intereses.add(interes);
    }
    protected void borrar(Observer o){
        observadores.remove(o);
        interesesPorObserver.remove(o);
    }

    protected abstract void notificar(EventoForo evento);  // Ahora el evento incluye el tipo de evento, lo que permite a los observadores filtrar las notificaciones


    @Override
    public void close() {
        for (Observer o : observadores){
            o.avisarBorradoSujeto(this) ; // El sujeto borra a todos los observadores al cerrarse
        }
    }
}
