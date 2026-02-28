import java.rmi.Naming;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente del Foro Distribuido.
 * Interfaz interactiva que permite a los usuarios:
 * - Registrarse en el foro
 * - Enviar mensajes
 * - Ver mensajes recientes
 * - Ver lista de usuarios
 */
public class Cliente {

    // IP y puerto del Broker (único dato que necesita conocer el cliente)
    private static final String IP_BROKER  = "155.210.154.196";  // Reemplazar con la IP del broker
    private static final int PUERTO_BROKER = 32000;

    private static Broker broker; 
    private static String usuarioActual = null;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            // Obtener referencia al Broker
            broker = (Broker) Naming.lookup(
                    "//" + IP_BROKER + ":" + PUERTO_BROKER + "/Broker800");
            if (broker == null) {
                System.out.println("[Cliente] El borker es null después del lookup, no se pudo conectar al Broker.");
                System.exit(1);
            }

            System.out.println("================================");
            System.out.println("  Foro Distribuido - Cliente");
            System.out.println("================================");
            System.out.println("[Cliente] Conectado al Broker correctamente.\n");

            // Flujo principal del cliente
            registrarUsuario();
            menuPrincipal();
        }
        catch (Exception ex){
            System.out.println("[Cliente] Error: " + ex);
            System.out.println(ex);
            ex.printStackTrace();  // Para ver el error completo
            System.exit(1); 
        }
    }

    /*
    *
    */
   private static void registrarUsuario() {
        System.out.println("=================================");
        System.out.println("  REGISTRO DE USUARIO");
        System.out.println("=================================");
        
        while (usuarioActual == null) {
            System.out.print("Introduce tu nombre de usuario: ");
            String nombre = scanner.nextLine().trim();
            
            if (nombre.isEmpty()) {
                System.out.println("[!] El nombre no puede estar vacío\n");
                continue;
            }
            
            try {
                Respuesta r = broker.ejecutar_servicio(
                        "registrar_usuario", Arrays.asList(nombre));
                
                if (r.isExito()) {
                    usuarioActual = nombre;
                    System.out.println("[OK] " + r.getMensaje());
                    System.out.println("¡Bienvenido/a al foro, " + usuarioActual + "!\n");
                } else {
                    System.out.println("[!] " + r.getMensaje());
                    System.out.print("¿Quieres intentar con otro nombre? (s/n): ");
                    String respuesta = scanner.nextLine().trim().toLowerCase();
                    if (!respuesta.equals("s")) {
                        System.out.println("Saliendo...");
                        System.exit(0);
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    /**
     * Menú principal del foro
     */
    private static void menuPrincipal() {
        while (true) {
            System.out.println("\n=================================");
            System.out.println("  MENÚ PRINCIPAL");
            System.out.println("=================================");
            System.out.println("1. Ver servicios disponibles");
            System.out.println("2. Enviar mensaje");
            System.out.println("3. Ver mensajes recientes");
            System.out.println("4. Ver lista de usuarios");
            System.out.println("5. Estadísticas del foro");
            System.out.println("6. Salir");
            System.out.print("Selecciona una opción: ");

            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    verServiciosDisponibles();
                    break;
                case "2":
                    enviarMensaje();
                    break;
                case "3":
                    verMensajesRecientes();
                    break;
                case "4":
                    verListaUsuarios();
                    break;
                case "5":
                    verEstadisticas();
                    break;
                case "6":
                    System.out.println("\n¡Hasta luego!" + usuarioActual + "!");
                    System.exit(0);
                default:
                    System.out.println("[!] Opción no válida. Inténtalo de nuevo.");
            }
        }
    }

    /*
    * Opción 1: Ver servicios disponibles
    * Muestra la lista de servicios que el Broker tiene registrados, incluyendo el nombre del servicio
    */
   private static void verServiciosDisponibles() {
        System.out.println("\n=================================");
        System.out.println("  SERVICIOS DISPONIBLES");
        System.out.println("=================================");
        try {
            Servicios servicios = broker.lista_servicios();
            if (servicios.getServicios().isEmpty()) {
                System.out.println("No hay servicios disponibles en este momento.");
            } else {
                servicios.getServicios().forEach(s -> 
                    System.out.println("- " + s.getNombreServicio() + " (Servidor: " + s.getNombreServidor() + ")")
                );
                System.out.println("\nTotal: " + servicios.getNumeroServicios() + " servicios");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /*
    * Opción 2: Enviar un mensaje al foro
    * Permite al usuario escribir un mensaje que se enviará al foro. El mensaje se asocia con el nombre del usuario que lo envía.
    */
    private static void enviarMensaje() {
        System.out.println("\n=================================");
        System.out.println("  ENVIAR MENSAJE");
        System.out.println("=================================");

        System.out.print("Escribe tu mensaje: ");
        String textoMensaje = scanner.nextLine().trim();
        if (textoMensaje.isEmpty()) {
            System.out.println("[!] El mensaje no puede estar vacío.");
            return;
        }

        try {
            Respuesta r =  broker.ejecutar_servicio("enviar_mensaje", Arrays.asList(usuarioActual, textoMensaje));

            if (r.isExito()) {
                System.out.println("[OK] " + r.getMensaje());
            } else {
                System.out.println("[!] No se pudo enviar el mensaje: " + r.getMensaje());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /*
    * Opción 3: Ver mensajes recientes del foro
    * Muestra los últimos mensajes enviados al foro, incluyendo el nombre del remitente y el contenido del mensaje
    */
    private static void verMensajesRecientes() {
        System.out.println("\n=================================");
        System.out.println("  MENSAJES RECIENTES");
        System.out.println("=================================");
        try {
            Respuesta r = broker.ejecutar_servicio("obtener_mensajes", Arrays.asList(10)); // Obtenemos los últimos 10 mensajes
            if (r.isExito()) {
                @SuppressWarnings("unchecked")
                List<Mensaje> mensajes = (List<Mensaje>) r.getResultado();
                if (mensajes.isEmpty()) {
                    System.out.println("No hay mensajes en el foro.");
                } else {
                    mensajes.forEach(m -> System.out.println("- [" + m.getUsuario() + "]: " + m.getTexto()));
                }
            } else {
                System.out.println("[!] No se pudieron obtener los mensajes: " + r.getMensaje());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /*
    * Opción 4: Ver lista de usuarios registrados en el foro
    * Muestra la lista de nombres de usuario registrados en el sistema  
    */
    private static void verListaUsuarios() {
        System.out.println("\n=================================");
        System.out.println("  LISTA DE USUARIOS REGISTRADOS");
        System.out.println("=================================");
        try {
            Respuesta r = broker.ejecutar_servicio("obtener_usuarios", Arrays.asList());
            if (r.isExito()) {
                @SuppressWarnings("unchecked")
                List<String> usuarios = (List<String>) r.getResultado();
                if (usuarios.isEmpty()) {
                    System.out.println("No hay usuarios registrados.");
                } else {
                    usuarios.forEach(u -> System.out.println("- " + u + (u.equals(usuarioActual) ? " (tú)" : "")));
                }
            } else {
                System.out.println("[!] No se pudieron obtener los usuarios: " + r.getMensaje());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /*
    * Opción 5: Ver estadísticas del foro
    * Muestra el número total de usuarios registrados y el número total de mensajes enviados en el
    */
    private static void verEstadisticas() {
        try {
            Respuesta rUsuarios = broker.ejecutar_servicio("contar_usuarios", Arrays.asList());
            Respuesta rMensajes = broker.ejecutar_servicio("contar_mensajes", Arrays.asList());

            if (rUsuarios.isExito() && rMensajes.isExito()) {
                int totalUsuarios = (int) rUsuarios.getResultado();
                int totalMensajes = (int) rMensajes.getResultado();
                System.out.println("\n=================================");
                System.out.println("  ESTADISTICAS DEL FORO");
                System.out.println("=================================");
                System.out.println("Total de usuarios registrados: " + totalUsuarios);
                System.out.println("Total de mensajes enviados: " + totalMensajes);
            } else {
                System.out.println("[!] No se pudieron obtener las estadísticas.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }
}
