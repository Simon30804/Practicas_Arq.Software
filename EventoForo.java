package Pr1;

public class EventoForo {
    private Mensaje mensaje;
    private TipoEvento tipo;

    public EventoForo(Mensaje mensaje, TipoEvento tipo) {
        this.mensaje = mensaje;
        this.tipo = tipo;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public Mensaje getMensaje() {
        return mensaje;
    }
}
