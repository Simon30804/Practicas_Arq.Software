package Pr1;

import Pr1.publisher.TemaForo;
import Pr1.suscriber.Usuario;
import java.util.EnumSet;

public class Main {
    public static void main(String[] args) {
        TemaForo javaAvanzado = new TemaForo("Programación en Java");
        TemaForo patrones = new TemaForo("Patrones de Diseño");

        // Preferencias de pull por usuario (híbrido push/pull)
        Usuario user1 = new Usuario("Alba"); // pull completo (contenido + autor)
        Usuario user2 = new Usuario("Beto", false, false); // solo push mínimo
        Usuario user3 = new Usuario("Manuel", true, false); // solo pull de contenido

        System.out.println(("=== SUSCRIPCIONES ==="));
        // Preferencias de notificación (aspectos por tipo de evento)
        javaAvanzado.suscribir(
            user1,
            EnumSet.of(TipoEvento.NUEVO_MENSAJE, TipoEvento.MENSAJE_EDITADO, TipoEvento.NUEVA_SUSCRIPCION)
        );
        javaAvanzado.suscribir(
            user2,
            EnumSet.of(TipoEvento.NUEVA_SUSCRIPCION, TipoEvento.MENSAJE_ELIMINADO)
        );
        javaAvanzado.suscribir(user3); // Sin preferencias explícitas: recibe todos los eventos

        System.out.println(("=== INICIO DEL FORO ==="));

        // Alba publica un mensaje en el foro
        Mensaje m = new Mensaje(user1, "¡Hola! ¿Cómo implemento el patrón Observer?");
        System.out.println("[ESTADO] Se publica un nuevo mensaje en " + javaAvanzado.getTitulo() + ".");
        javaAvanzado.publicarMensaje(m);
        System.out.println();

        System.out.println("=== BETO ACTIVA INTERES EN NUEVOS MENSAJES ===");
        javaAvanzado.registrarInteres(user2, TipoEvento.NUEVO_MENSAJE);
        System.out.println();

        // Beto publica otro mensaje, en respuesta al mensaje de Alba
        Mensaje m2 = new Mensaje(user2, "Puedes crear una interfaz Observer y que los usuarios la implementen.");
        System.out.println("[ESTADO] Se publica un nuevo mensaje en " + javaAvanzado.getTitulo() + ".");
        javaAvanzado.publicarMensaje(m2);
        System.out.println();

        // Manuel publica un mensaje
        Mensaje m3 = new Mensaje(user3, "A mí también me vendría bien sberlo");
        System.out.println("[ESTADO] Se publica un nuevo mensaje en " + javaAvanzado.getTitulo() + ".");
        javaAvanzado.publicarMensaje(m3);
        System.out.println();

        // Mostramos los mensaje actuales
        javaAvanzado.mostrarMensajes();

        // Manuel se da cuenta que lo ha escrito mal, y lo edita
        System.out.println("\n=== MANUEL EDITA SU MENSAJE ===");
        System.out.println("[ESTADO] Se edita un mensaje en " + javaAvanzado.getTitulo() + ".");
        javaAvanzado.editarMensaje(m3.getId(), "A mí también me vendría bien saberlo", user3); 
        System.out.println();

        // Mostramos los mensaje actuales
        javaAvanzado.mostrarMensajes();

        // Manuel decide eliminar su mensaje
        System.out.println("\n=== MANUEL ELIMINA SU MENSAJE ===");
        System.out.println("[ESTADO] Se elimina un mensaje en " + javaAvanzado.getTitulo() + ".");
        javaAvanzado.eliminarMensaje(m3.getId(), user3);
        System.out.println();

        // Mostramos los mensaje actuales
        javaAvanzado.mostrarMensajes();

        // Beto silencia el foro de Java, pero sigue atento a otros foros
        System.out.println("\n=== BETO SILENCIA EL FORO DE JAVA ===");
        user2.silenciarForo(javaAvanzado);
        System.out.println("Beto ha silenciado el foro: " + javaAvanzado.getTitulo());

        // Mensaje posterior al silencio (Beto no debería recibir nada)
        Mensaje m4 = new Mensaje(user1, "Mensaje después del silencio de Beto.");
        System.out.println("[ESTADO] Se publica un nuevo mensaje en " + javaAvanzado.getTitulo() + ".");
        javaAvanzado.publicarMensaje(m4);
        System.out.println();

        // Mensaje en otro foro para mostrar la diferenciación por sujeto
        Mensaje p1 = new Mensaje(user2, "Recomiendo empezar por Observer y Strategy.");
        patrones.suscribir(user2, EnumSet.of(TipoEvento.NUEVO_MENSAJE));
        System.out.println("[ESTADO] Se publica un nuevo mensaje en " + patrones.getTitulo() + ".");
        patrones.publicarMensaje(p1);
        System.out.println();

        // Beto intenta editar el mensaje de Alba, pero no puede porque no es el autor del mensaje
        System.out.println("\n=== BETO INTENTA EDITAR EL MENSAJE DE ALBA ===");
        System.out.println("[ESTADO] Se intenta editar un mensaje en " + javaAvanzado.getTitulo() + ".");
        boolean editado = javaAvanzado.editarMensaje(m.getId(), "¡Hola! ¿Cómo implemento el patrón Observer? (editado por Beto)", user2);
        if (!editado) {
            System.out.println("Beto no pudo editar el mensaje de Alba porque no es el autor del mensaje.");
        }
    }
}
