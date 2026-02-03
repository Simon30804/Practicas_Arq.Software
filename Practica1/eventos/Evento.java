package Practica1.eventos;

import java.util.Set;

public abstract class Evento {
    private final Set<String> eventosInteres;

    protected Evento(Set<String> eventosInteres) {
        this.eventosInteres = eventosInteres;
    }

    public Set<String> getEventosInteres() {
        return eventosInteres;
    }
}