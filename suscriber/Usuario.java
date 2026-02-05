package Pr1.suscriber;

import Pr1.EventoForo;
import Pr1.Sujeto;
import Pr1.Observer;

public class Usuario implements Observer {
    private String nombre;

    public Usuario(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public void actualizar(Sujeto s, EventoForo evento) {
        System.out.println("Notificación para " + nombre + ": Hay un nuevo evento de tipo " 
                           + evento.getTipo() + " en el foro.");

        // Mostramos info del mensaje si está disponible
        if (evento.getMensaje() != null) {
            System.out.println("Mensaje: " + evento.getMensaje().getContenido());
            System.out.println("Autor: " + evento.getMensaje().getAutor().getNombre());
        }
    }

    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Usuario usuario = (Usuario) obj;
        return nombre.equals(usuario.nombre);
    }
}