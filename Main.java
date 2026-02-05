package Pr1;

import Pr1.publisher.TemaForo;
import Pr1.suscriber.Usuario;

public class Main {
    public static void main(String[] args) {
        TemaForo javaAvanzado = new TemaForo("Programación en Java");

        Usuario user1 = new Usuario("Alba");
        Usuario user2 = new Usuario("Beto");
        Usuario user3 = new Usuario("Manuel");

        javaAvanzado.suscribir(user1);
        javaAvanzado.suscribir(user2);
        javaAvanzado.suscribir(user3);

        System.out.println(("=== INICIO DEL FORO ==="));

        // Alba publica un mensaje en el foro
        Mensaje m = new Mensaje(user1, "¡Hola! ¿Cómo implemento el patrón Observer?");
        javaAvanzado.publicarMensaje(m);
        System.out.println();

        // Beto publica otro mensaje, en respuesta al mensaje de Alba
        Mensaje m2 = new Mensaje(user2, "Puedes crear una interfaz Observer y que los usuarios la implementen.");
        javaAvanzado.publicarMensaje(m2);
        System.out.println();

        // Manuel publica un mensaje
        Mensaje m3 = new Mensaje(user3, "A mí también me vendría bien sberlo");
        javaAvanzado.publicarMensaje(m3);
        System.out.println();

        // Mostramos los mensaje actuales
        javaAvanzado.mostrarMensajes();

        // Manuel se da cuenta que lo ha escrito mal, y lo edita
        System.out.println("\n=== MANUEL EDITA SU MENSAJE ===");
        javaAvanzado.editarMensaje(m3.getId(), "A mí también me vendría bien saberlo", user3); 
        System.out.println();

        // Mostramos los mensaje actuales
        javaAvanzado.mostrarMensajes();

        // Manuel decide eliminar su mensaje
        System.out.println("\n=== MANUEL ELIMINA SU MENSAJE ===");
        javaAvanzado.eliminarMensaje(m3.getId(), user3);
        System.out.println();

        // Mostramos los mensaje actuales
        javaAvanzado.mostrarMensajes();

        // Beto intenta editar el mensaje de Alba, pero no puede porque no es el autor del mensaje
        System.out.println("\n=== BETO INTENTA EDITAR EL MENSAJE DE ALBA ===");
        boolean editado = javaAvanzado.editarMensaje(m.getId(), "¡Hola! ¿Cómo implemento el patrón Observer? (editado por Beto)", user2);
        if (!editado) {
            System.out.println("Beto no pudo editar el mensaje de Alba porque no es el autor del mensaje.");
        }
    }
}