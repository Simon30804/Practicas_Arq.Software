import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Cliente del Foro Distribuido.
 * Interfaz interactiva que permite a los usuarios:
 * - Registrarse en el foro
 * - Enviar mensajes
 * - Ver mensajes recientes
 * - Ver lista de usuarios
 * - Ver estadísticas del foro
 * - Ver la lista de servicios disponibles en el Broker
 * - Seleccionar un servicio de la lista y ejecutarlo (de manera dinámica)
 * - Ejecutar un servicio de forma asíncrona y recoger su respuesta posteriormente
 */
public class Cliente {

    // IP y puerto del Broker (único dato que necesita conocer el cliente)
    private static final String IP_BROKER  = "155.210.154.196";  // Reemplazar con la IP del broker
    private static final int PUERTO_BROKER = 32000;

    private static Broker broker; 
    private static String usuarioActual = null;
    private static Scanner scanner = new Scanner(System.in);

    /**
     * Servicio asíncrono pendiente de recoger (solo registramos el nombre del servicio).
     * Permite al cliente saber qué petición asíncrona tiene 'activa'.
     */
    private static String servicioAsincPendiente = null;

    public static void main(String[] args) {
        try {
            // Obtenemos la referencia al Broker
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
            if (servicioAsincPendiente != null) {
                System.out.println("  [ASÍNCRONO PENDIENTE: " + servicioAsincPendiente + "]");
            }
            System.out.println("=================================");
            System.out.println("1. Ver servicios disponibles");
            System.out.println("2. Enviar mensaje (síncrono)");
            System.out.println("3. Ver mensajes recientes (síncrono)");
            System.out.println("4. Ver lista de usuarios (síncrono)");
            System.out.println("5. Estadísticas del foro (síncrono)");
            System.out.println("6. Seleccionar servicio de la lista y ejecutar");
            System.out.println("7. Ejecutar servicio ASÍNCRONO" + (servicioAsincPendiente != null ? "  [bloqueado — recoge la respuesta pendiente primero]" : ""));
            System.out.println("8. Obtener respuesta ASÍNCRONA"+ (servicioAsincPendiente == null ? "  [no hay petición pendiente]" : " → " + servicioAsincPendiente));
            System.out.println("9. Salir");
            System.out.print("Selecciona una opción: ");
 
            switch (scanner.nextLine().trim()) {
                case "1": verServiciosDisponibles();       break;
                case "2": enviarMensaje();                 break;
                case "3": verMensajesRecientes();          break;
                case "4": verListaUsuarios();              break;
                case "5": verEstadisticas();               break;
                case "6": seleccionarYEjecutarServicio();  break;
                case "7": ejecutarServicioAsincrono();     break;
                case "8": obtenerRespuestaAsincrona();     break;
                case "9":
                    System.out.println("\n¡Hasta luego, " + usuarioActual + "!");
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

    /**
     * Opción 6: Seleccionar un servicio de la lista y ejecutarlo
     * Muestra la lista de sericios registrados en el Broker, permite al usuario seleccionar uno, introducir los parámetros necesarios y ejecutarlo.
     */
    private static void seleccionarYEjecutarServicio(){
        System.out.println("\n=================================");
        System.out.println("  SELECCIONAR Y EJECUTAR SERVICIO");
        System.out.println("=================================");
 
        try {
            Servicios servicios = broker.lista_servicios();
            List<Servicios.Servicio> lista = servicios.getServicios();
 
            if (lista.isEmpty()) {
                System.out.println("No hay servicios disponibles en este momento.");
                return;
            }
 
            // Mostramos la lista de servicios numerada
            System.out.println("Servicios disponibles:");
            for (int i = 0; i < lista.size(); i++) {
                Servicios.Servicio s = lista.get(i);
                System.out.printf("  %2d. %-25s  parámetros: %s  retorno: %s%n",
                        i + 1,
                        s.getNombreServicio(),
                        s.getListaParametros().isEmpty() ? "(ninguno)" : s.getListaParametros(),
                        s.getTipoRetorno());
            }
 
            // El usuario selecciona un servicio
            System.out.print("\nSelecciona el número del servicio (0 para cancelar): ");
            int eleccion;
            try {
                eleccion = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[!] Entrada no válida.");
                return;
            }
            if (eleccion == 0) return;
            if (eleccion < 1 || eleccion > lista.size()) {
                System.out.println("[!] Número fuera de rango.");
                return;
            }
 
            Servicios.Servicio servicioElegido = lista.get(eleccion - 1);
            System.out.println("\n→ Servicio seleccionado: " + servicioElegido.getNombreServicio());
 
            // Solicitamos los parámetros al usuario
            List<Object> parametros = pedirParametros(servicioElegido.getListaParametros());
            if (parametros == null) return; // el usuario canceló
 
            // Ejecutamos el servicio de forma síncrona
            Respuesta r = broker.ejecutar_servicio(
                    servicioElegido.getNombreServicio(), parametros);
 
            System.out.println("\n── Resultado ──────────────────");
            if (r.isExito()) {
                System.out.println("[OK] " + r.getMensaje());
                if (r.getResultado() != null) {
                    System.out.println("     Resultado: " + r.getResultado());
                }
            } else {
                System.out.println("[!] Error: " + r.getMensaje());
            }
 
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /**
     * Opción 7: Ejecutar un servicio de forma asíncrona
     */
    private static void ejecutarServicioAsincrono() {
        System.out.println("\n=================================");
        System.out.println("  EJECUTAR SERVICIO ASÍNCRONO");
        System.out.println("=================================");

        // RESTRICCIÓN: solo una petición asíncrona pendiente a la vez, hasta que el cliente recoja su respuesta (opción 8), no puede lanzar otra petición asíncrona diferente.
        if (servicioAsincPendiente != null) {
            System.out.println("[!] Ya tienes una petición asíncrona pendiente: '"
                    + servicioAsincPendiente + "'");
            System.out.println("    Recoge su respuesta (opción 8) antes de lanzar otra.");
            return;
        }

        try {
            Servicios servicios = broker.lista_servicios();
            List<Servicios.Servicio> lista = servicios.getServicios();
 
            if (lista.isEmpty()) {
                System.out.println("No hay servicios disponibles.");
                return;
            }
 
            // Mostramos los servicios disponibles
            System.out.println("Servicios disponibles:");
            for (int i = 0; i < lista.size(); i++) {
                Servicios.Servicio s = lista.get(i);
                String estado = servicioAsincPendiente != null && servicioAsincPendiente.equals(s.getNombreServicio())
                        ? "  [PENDIENTE]" : "";
                System.out.printf("  %2d. %-25s%s%n", i + 1, s.getNombreServicio(), estado);
            }
 
            System.out.print("\nSelecciona el número del servicio (0 para cancelar): ");
            int eleccion;
            try {
                eleccion = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[!] Entrada no válida.");
                return;
            }
            if (eleccion == 0) return;
            if (eleccion < 1 || eleccion > lista.size()) {
                System.out.println("[!] Número fuera de rango.");
                return;
            }
 
            Servicios.Servicio servicioElegido = lista.get(eleccion - 1);
 
            // Solicitamos los parámetros
            List<Object> parametros = pedirParametros(servicioElegido.getListaParametros());
            if (parametros == null) return;
 
            // Enviamos la petición asíncrona
            broker.ejecutar_servicio_asinc(servicioElegido.getNombreServicio(), parametros);
            servicioAsincPendiente = servicioElegido.getNombreServicio();
            System.out.println("[OK] Petición asíncrona de '" + servicioAsincPendiente
                    + "' registrada. El cliente puede seguir trabajando.");
 
        } catch (Exception e) {
            // Si el broker lanza RemoteException por petición duplicada, lo mostramos
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /**
     * Opción 8: Obtener la respuesta de un servicio asíncrono
     */
    private static void obtenerRespuestaAsincrona() {
         System.out.println("\n=================================");
        System.out.println("  OBTENER RESPUESTA ASÍNCRONA");
        System.out.println("=================================");
 
        if (servicioAsincPendiente == null ) {
            System.out.println("No tienes peticiones asíncronas pendientes.");
            return;
        }
 
        try {
            Respuesta r = broker.obtener_respuesta_asinc(servicioAsincPendiente);
 
            System.out.println("\n── Resultado de '" + servicioAsincPendiente + "' ──");
            if (r.isExito()) {
                System.out.println("[OK] " + r.getMensaje());
                if (r.getResultado() != null) {
                    System.out.println("     Resultado: " + r.getResultado());
                }
                // La respuesta ha sido entregada: lo eliminamos como pendiente
                servicioAsincPendiente = null;
            } else if (r.getMensaje().contains("aún no está lista")) {
                System.out.println("[!] La respuesta aún no está lista. Intenta de nuevo más tarde.");
            }
            else {
                System.out.println("[!] Error al obtener la respuesta: " + r.getMensaje());
                servicioAsincPendiente = null;
            }
 
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para pedir al usuario los parámetros necesarios para ejecutar un servicio.
     * Además le proporcionamos una descripción de cada parámetro (tipo y nombre) para que sepa qué introducir.
     */
    private static List<Object> pedirParametros(List<Object> descripcionParametros) {
        List<Object> parametros = new ArrayList<>();
 
        if (descripcionParametros == null || descripcionParametros.isEmpty()) {
            System.out.println("  (este servicio no requiere parámetros)");
            return parametros;
        }
 
        System.out.println("  Introduce los parámetros (deja en blanco y pulsa Enter para cancelar):");
 
        for (Object desc : descripcionParametros) {
            String descripcion = desc.toString().trim();
            // Extraemos el tipo (primera palabra) y el nombre (resto)
            String[] partes = descripcion.split("\\s+", 2);
            String tipo   = partes[0].toLowerCase();
            String nombre = partes.length > 1 ? partes[1] : descripcion;
 
            System.out.printf("    %-15s (%s): ", nombre, tipo);
            String entrada = scanner.nextLine().trim();
 
            if (entrada.isEmpty()) {
                System.out.println("  Cancelado.");
                return null;
            }
 
            try {
                switch (tipo) {
                    case "int":
                    case "integer":
                        parametros.add(Integer.parseInt(entrada));
                        break;
                    case "long":
                        parametros.add(Long.parseLong(entrada));
                        break;
                    case "double":
                        parametros.add(Double.parseDouble(entrada));
                        break;
                    case "boolean":
                        parametros.add(Boolean.parseBoolean(entrada));
                        break;
                    default: // String y cualquier otro tipo lo tratamos como String
                        parametros.add(entrada);
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Valor no válido para el tipo '" + tipo + "'. Cancelando.");
                return null;
            }
        }
 
        return parametros;
    }
}
