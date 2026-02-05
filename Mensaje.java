package Pr1;

import java.time.LocalDateTime;
import Pr1.suscriber.Usuario;
import java.util.UUID;

public class Mensaje {
    private String id; // Para identificar cada mensaje de forma única,  y así poder editarlo o eliminarlo.
    private Usuario autor;
    private String contenido;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEdicion; // Para saber cuándo se editó el mensaje por última vez.
    private boolean editado; // Para saber si el mensaje ha sido editado o no.

    public Mensaje(Usuario autor, String contenido) {
        this.id = UUID.randomUUID().toString(); // Genera un ID único para cada mensaje.
        this.autor = autor;
        this.contenido = contenido;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaEdicion = null; // Inicialmente no hay fecha de edición.
        this.editado = false; 
    }

    public void editarMensaje(String nuevoMensaje) {
        this.contenido = nuevoMensaje;
        this.fechaEdicion = LocalDateTime.now(); // Actualiza la fecha de edición.
        this.editado = true; // Marcamos el mensaje como editado.
    }

    public String getId() {
        return id;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getContenido() {
        return contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEdicion() {
        return fechaEdicion;
    }

    @Override
    public String toString() {
        String info = "[" + fechaCreacion.getHour() + ":" + String.format("%02d", fechaCreacion.getMinute()) + "] -- " +  "[ ID.Mensaje: " + id.substring(0, 8) + "...] " + autor + ": " + contenido;
        if (editado) {
            info += " (editado)";
        }
        return info;
    }
}