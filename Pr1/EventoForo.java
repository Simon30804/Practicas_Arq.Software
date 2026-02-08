package Pr1;

/*
 * EventoForo.java
 * Clase que representa un evento en el foro, incluyendo el tipo de evento y un identificador de mensaje opcional.
 * Este diseño permite a los observadores filtrar eventos por tipo y acceder a detalles específicos del mensaje si es relevante.
*   NOTE: eventoForo supone el payload de la notificación. 
*/
public class EventoForo {
    private String idMensaje; // Puede ser null para eventos sin mensaje asociado.
    private TipoEvento tipo;

    public EventoForo(String idMensaje, TipoEvento tipo) { 
        this.idMensaje = idMensaje;
        this.tipo = tipo;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getIdMensaje() {
        return idMensaje;
    }
}
