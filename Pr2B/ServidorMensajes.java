import java.rmi.Remote; 
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject; 
import java.util.List;
import java.util.ArrayList;
import java.rmi.Naming;
import java.net.MalformedURLException;

/*
* Este servidor se va a encargar de la gestion de mensajes.
* Sus servicios disponibles son los siguientes:
* - enviarMensaje(String remitente, String mensaje): Permite añadir un mensaje al foro
* - obtenerMensajes(int N): Permite obtener la lista de los últimos N mensajes 
*/
public class ServidorMensajes extends UnicastRemoteObject
implements Servidor {

    // IP y puerto del Broker para el registro del servidor
    private static final String IP_BROKER = "localhost"; // Lo tengo que cambiar
    private static final int PUERTO_BROKER = 32000;

    // IP y puerto donde el servidor está escuchando en RMI
    private static final String IP_SERVIDOR = "localhost"; // Lo tengo que cambiar
    private static final int PUERTO = 32001;

    // Nombre único del server para su registro en el Broker
    private static final String NOMBRE_SERVIDOR = "ServidorMensajes8698";

    // Almacenamiento de los mensajes
    private List<Mensaje> mensajes = new ArrayList<>();
    // Contador para asignar IDs únicos a los mensajes
    private int contadorId = 1;

    // Constructor del servidor
    public ServidorMensajes() throws RemoteException {
        super();
        // Añadir algunos mensajes de bienvenida
        mensajes.add(new Mensaje(contadorId++, "Sistema", "Bienvenido al foro basado en RMI y patrón Broker"));
        mensajes.add(new Mensaje(contadorId++, "Sistema", "Puedes enviar mensajes y consultar los últimos mensajes del foro"));
    }

    @Override
    public Respuesta ejecutarServicio(String nom_servicio, List<Object> parametros) throws RemoteException {
        System.out.println("[ServidorMensajes] Ejecutando servicio: " + nom_servicio
                + " con parámetros: " + parametros);
        try {
            switch (nom_servicio) {
                case "enviar_mensaje":
                    return enviarMensaje(parametros);
                case "obtener_mensajes":
                    return obtenerMensajes(parametros);
                case "contar_mensajes":
                    return contarMensajes(parametros);
                case "eliminar_mensaje":
                    return eliminarMensaje(parametros);
                default:
                    return new Respuesta("Servicio no reconocido en ServidorMensajes: " + nom_servicio);
            }
        } catch (Exception e) {
            return new Respuesta("Error al ejecutar el servicio: " + e.getMessage());
        }
    }

    /*
    * Función para enviar un mensaje al foro
    * El parámetro "parametros" debe contener dos elementos: el nombre del remitente (String) y el contenido del mensaje (String)
    * Devuelve una Respuesta indicando que el mensaje se ha enviado correctamente, incluyendo el ID
    * El mensaje se almacena en una lista interna del servidor, junto con un ID único generado automáticamente y el nombre del remitente
    */
    private Respuesta enviarMensaje(List<Object> parametros) {
        String usuario = (String) parametros.get(0);
        String mensaje = (String) parametros.get(1);

        Mensaje nuevoMensaje = new Mensaje(contadorId++, usuario, mensaje);
        mensajes.add(nuevoMensaje);

        System.out.println("[ServidorMensajes] Mensaje enviado: " + nuevoMensaje);
        int idMensaje = nuevoMensaje.getId();
        String m = "Mensaje enviado correctamente (ID: " + idMensaje + ")";
        return new Respuesta((Object) m);
    } 
    
    /*
    * Función para eliminar un mensaje del foro
    * El parámetro "parametros" debe contener un único elemento que es el ID del mensaje a eliminar (int)
    * Devuelve una Respuesta indicando si el mensaje se ha eliminado correctamente o no
    */
    private Respuesta eliminarMensaje(List<Object> parametros) {
        int idMensaje = ((Number) parametros.get(0)).intValue();
        
        boolean eliminado = mensajes.removeIf(m -> m.getId() == idMensaje);
        
        if (eliminado) {
            System.out.println("[ServidorMensajes] Mensaje " + idMensaje + " eliminado");
            return new Respuesta((Object) "Mensaje eliminado correctamente");
        } else {
            return new Respuesta("No se encontró ningún mensaje con ID: " + idMensaje);
        }
    }

    /*
    * Función para obtener los últimos N mensajes del foro
    * El parámetro "parametros" debe contener un único elemento que es el número de mensajes a obtener (N)
    * Devuelve una Respuesta con una lista de los últimos N mensajes
    * Si el número de mensajes solicitados es mayor que el total de mensajes disponibles, se devuelven todos los mensajes almacenados
    * Cada mensaje se representa como un objeto de la clase Mensaje, que contiene un ID, el remitente y el contenido del mensaje
    */
    private Respuesta obtenerMensajes(List<Object> parametros) {
        int limite = ((Number) parametros.get(0)).intValue(); // Extraemos el número de mensajes a obtener

        int inicio = Math.max(0, mensajes.size() - limite); // Calculamos el índice de inicio para obtener los últimos N mensajes
        List<Mensaje> ultimosMensajes = new ArrayList<>(mensajes.subList(inicio, mensajes.size())); // Obtenemos los últimos N mensajes

        System.out.println("[ServidorMensajes] Devolviendo " + ultimosMensajes.size() + " mensajes");
        return new Respuesta(ultimosMensajes);
    }

    /*
    * Contar el número total de mensajes almacenados en el servidor
    */
    private Respuesta contarMensajes(List<Object> parametros) {
        int numeroMensajes = mensajes.size();
        System.out.println("[ServidorMensajes] Número total de mensajes: " + numeroMensajes);
        return new Respuesta(numeroMensajes);
    }


    public static void main(String[] args) {
        try {
            // Creamos y registramos el Objeto en el rmiregistry para que el Broker pueda localizarlo
            ServidorMensajes servidor = new ServidorMensajes();
            String direccionRMI = IP_SERVIDOR + ":" + PUERTO;
            Naming.rebind("//" + direccionRMI + "/" + NOMBRE_SERVIDOR, servidor);
            System.out.println("[ServidorMensajes] Servidor registrado en RMI con nombre: " + NOMBRE_SERVIDOR);

            // Nos conectamos al Broker para registrar el servicio que ofrecemos
            Broker broker = (Broker) Naming.lookup("//" + IP_BROKER + ":" + PUERTO_BROKER + "/Broker800");
            broker.registrar_servidor(NOMBRE_SERVIDOR, direccionRMI);
            System.out.println("[ServidorMensajes] Servicio registrado en el Broker: " + NOMBRE_SERVIDOR);

        } catch (Exception e) {
            System.err.println("[ServidorMensajes] Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
