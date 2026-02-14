package Pr1.suscriber;

import Pr1.EventoForo;
import Pr1.Mensaje;
import Pr1.Sujeto;
import Pr1.Observer;
import Pr1.publisher.TemaForo;
import java.util.HashSet;
import java.util.Set;

/*
 * Usuario.java
 * Clase que representa a un usuario del foro que puede suscribirse a eventos y recibir notificaciones.
 * El usuario puede configurar si desea mostrar el contenido del mensaje, el autor, o ambos en las notificaciones.
 * Además, el usuario puede silenciar foros específicos para no recibir notificaciones de esos foros.
 */
public class Usuario extends Observer {
    private String nombre;

    // Mostrar contenido y autor son configurables para permitir al usuario personalizar su experiencia de notificación.
    // Son la parametrización del push and pull.
    private boolean mostrarContenido;
    private boolean mostrarAutor;
    private Set<Sujeto> forosSilenciados = new HashSet<>();

    public Usuario(String nombre) {
        this.nombre = nombre;
        this.mostrarContenido = true;
        this.mostrarAutor = true;
    }

    public Usuario(String nombre, boolean mostrarContenido, boolean mostrarAutor) {
        this.nombre = nombre;
        this.mostrarContenido = mostrarContenido;
        this.mostrarAutor = mostrarAutor;
    }

    @Override
    public void actualizar(Sujeto s, EventoForo evento) {
        if (forosSilenciados.contains(s)) {
            String sujetoInfo = s.getClass().getSimpleName();
            if (s instanceof TemaForo) {
                sujetoInfo = "Foro '" + ((TemaForo) s).getTitulo() + "'";
            }
            System.out.println("[SILENCIO] " + nombre + " ignora notificación en " + sujetoInfo + ".");
            return; // Foro silenciado: no se procesa la notificación
        }
        String sujetoInfo = s.getClass().getSimpleName();
        if (s instanceof TemaForo) {
            sujetoInfo = "Foro '" + ((TemaForo) s).getTitulo() + "'";
        }
        String idMensaje = evento.getIdMensaje();
        String idInfo = (idMensaje != null) ? " (id=" + idMensaje.substring(0, Math.min(8, idMensaje.length())) + "...)" : " (sin mensaje)";
        System.out.println("[PUSH] " + nombre + " recibe " + evento.getTipo() + " en " + sujetoInfo + idInfo + ".");

        // Pull opcional: el usuario decide si pide detalles del mensaje al sujeto
        if (idMensaje != null) {
            if (mostrarContenido || mostrarAutor) {
                System.out.println("[PULL] " + nombre + " consulta detalles del mensaje.");
                Mensaje mensaje = ((TemaForo) s).getMensaje(idMensaje);
                if (mensaje == null) {
                    System.out.println("[PULL] Detalles no disponibles (mensaje ya no existe).");
                    return;
                }
                if (mostrarContenido) {
                    System.out.println("[PULL] Mensaje: " + mensaje.getContenido());
                }
                if (mostrarAutor) {
                    System.out.println("[PULL] Autor: " + mensaje.getAutor().getNombre());
                }
            } else {
                System.out.println("[PULL] " + nombre + " no solicita detalles.");
            }
        }
    }

    public void silenciarForo(Sujeto s) {
        forosSilenciados.add(s);
    }

    public void activarForo(Sujeto s) {
        forosSilenciados.remove(s);
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
