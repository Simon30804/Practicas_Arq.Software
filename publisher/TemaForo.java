package Pr1.publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Pr1.EventoForo;
import Pr1.Mensaje;
import Pr1.TipoEvento;
import Pr1.Observer;
import Pr1.Sujeto;
import Pr1.suscriber.Usuario;

public class TemaForo implements Sujeto {
    private String titulo;
    private List<Observer> observadores = new ArrayList<>();
    private Map<String, Mensaje> mensajes = new HashMap<>(); 
    private List<String> ordenMensajes = new ArrayList<>(); // Para mantener el orden de los mensajes, ya que el HashMap no lo garantiza.

    public TemaForo(String titulo) {
        this.titulo = titulo;
    }

    @Override
    public void suscribir(Observer o) {
        observadores.add(o);
    }

    @Override
    public void borrar(Observer o) {
        observadores.remove(o);
    }

    @Override
    public void notificar(EventoForo evento) {
        for (Observer o : observadores) {
            o.actualizar(this, evento);
        }
    }

    public void publicarMensaje(Mensaje mensaje) {
        mensajes.put(mensaje.getId(), mensaje);
        ordenMensajes.add(mensaje.getId());
        notificar(new EventoForo(mensaje, TipoEvento.NUEVO_MENSAJE));
    }

    public boolean editarMensaje(String idMensaje, String nuevoContenido, Usuario autorEditor) {
        Mensaje mensaje = mensajes.get(idMensaje);

        if(mensaje == null) {
            System.out.println("Error: No se encontró el mensaje con ID: " + idMensaje);
            return false;
        }

        // Comprobamos que el autor del mensaje sea el mismo que el usuario que intenta editarlo.
        if (!mensaje.getAutor().equals(autorEditor)) {
            System.out.println("Error: Solo el autor del mensaje puede editarlo.");
            return false;
        }

        // Editamos el mensaje y notificamos a los observadores.
        mensaje.editarMensaje(nuevoContenido);
        notificar(new EventoForo(mensaje, TipoEvento.MENSAJE_EDITADO));

        return true;
    }

    public boolean eliminarMensaje(String idMensaje, Usuario autorElimina) {
        Mensaje mensaje = mensajes.get(idMensaje);
        
        if (mensaje == null) {
            System.out.println("Error: No se encontró el mensaje con ID: " + idMensaje);
            return false;
        }

        // Comprobamos que el autor del mensaje sea el mismo que el usuario que intenta eliminarlo.
        if (!mensaje.getAutor().equals(autorElimina)) {
            System.out.println("Error: Solo el autor del mensaje puede eliminarlo.");
            return false;
        }

        mensajes.remove(idMensaje);
        ordenMensajes.remove(idMensaje);
        notificar(new EventoForo(mensaje, TipoEvento.MENSAJE_ELIMINADO));
        return true;
    }

    public Mensaje getMensaje(String idMensaje) {
        return mensajes.get(idMensaje);
    }

    public Mensaje getUltimoMensaje() {
        if (mensajes.isEmpty()) {
            return null;
        }
        String idUltimoMensaje = ordenMensajes.get(ordenMensajes.size() - 1);
        return mensajes.get(idUltimoMensaje);
    }

    public List<Mensaje> getTodosLosMensajes() {
        List<Mensaje> listaMensajes = new ArrayList<>();
        for (String id : ordenMensajes) {
            listaMensajes.add(mensajes.get(id));
        }
        return listaMensajes;
    }

    public String getTitulo() {
        return titulo;
    }

    /**
     * Mostramos todos los mensajes del foro
     */
    public void mostrarMensajes() {
        System.out.println("\n=== Mensajes en: " + titulo + " ===");
        if (ordenMensajes.isEmpty()) {
            System.out.println("No hay mensajes todavía.");
            return;
        }
        
        for (String id : ordenMensajes) {
            Mensaje m = mensajes.get(id);
            System.out.println(m);
        }
        System.out.println("===============================\n");
    }
}