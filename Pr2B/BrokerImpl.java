import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Implementación del Broker.
 *
 * En el nivel básico, el mapeo entre servicios y servidores está definido
 * en el código fuente (método obtenerServidorDeServicio). Los servidores
 * registran dinámicamente su ubicación (IP:puerto), pero añadir nuevos
 * servicios requeriría recompilar el Broker.
 */

public class BrokerImpl extends UnicastRemoteObject 
implements Broker {
    // Mapa para almacenar los servidores registrados (nombre_servidor -> host_remoto_IP_puerto)
    private Map<String, String> servidoresRegistrados; // Mapa de servicios a servidores

    // IP del host donde reside el Broker
    private static final String hostBroker = "localhost"; // Reemplazar con la IP del broker
    
    // Puerto del rmiregistry del Broker
    private static final int puertoBroker = 32000; // Reemplazar con el puerto del broker

    // Constructor
    public BrokerImpl() throws RemoteException {
        super(); // Llamada al constructor de UnicastRemoteObject
    }

    /**
     * Los servidores llaman a este método para notificar al Broker su disponibilidad.
     */
    @Override
    public void registrar_servidor(String nombre_servidor, String host_remoto_IP_puerto) throws RemoteException {
        if (servidoresRegistrados == null) {
            servidoresRegistrados = new HashMap<>();
        }
        servidoresRegistrados.put(nombre_servidor, host_remoto_IP_puerto);
        System.out.println("[Broker] Servidor registrado: " + nombre_servidor + " en " + host_remoto_IP_puerto);
    }

    /**
     * Mapeo estático entre nombre de servicio y nombre de servidor que lo ofrece.
     * En el nivel básico este mapeo está hardcodeado.
     * Para añadir nuevos servicios habría que recompilar el Broker.
     */
    private String obtenerServidorDeServicio(String nom_servicio) {
        switch (nom_servicio) {
            case "enviar_mensaje":
            case "obtener_mensajes":
            case "eliminar_mensaje":
            case "contar_mensajes":
                return "ServidorMensajes8698";
            case "registrar_usuario":
            case "obtener_usuarios":
            case "contar_usuarios":
                return "ServidorUsuarios8698";
            default:
                return null;
        }
    }

    /**
     * Los clientes llaman a este método para solicitar la ejecución de un servicio.
     * El Broker localiza el servidor correspondiente y realiza la invocación.
     */
    @Override
    public Respuesta ejecutar_servicio(String nom_servicio, List<Object> parametros_servicio) throws RemoteException {
        System.out.println("[Broker] Petición de servicio: " + nom_servicio
                + " con parámetros: " + parametros_servicio);

        // 1. Determinar qué servidor ofrece este servicio
        String nombreServidor = obtenerServidorDeServicio(nom_servicio);
        if (nombreServidor == null) {
            System.out.println("[Broker] Servicio desconocido: " + nom_servicio);
            return new Respuesta("Servicio desconocido: " + nom_servicio);
        }

        // 2. Obtener la dirección del servidor
        String direccionServidor = servidoresRegistrados.get(nombreServidor);
        if (direccionServidor == null) {
            System.out.println("[Broker] Servidor no registrado: " + nombreServidor);
            return new Respuesta("Servidor no registrado: " + nombreServidor);
        }

        // 3. Invocar el servicio en el servidor correspondiente
        try {
            Servidor servidor = (Servidor) Naming.lookup(
                    "//" + direccionServidor + "/" + nombreServidor);
            Respuesta r = servidor.ejecutarServicio(nom_servicio, parametros_servicio);
            System.out.println("[Broker] Respuesta obtenida: " + r);
            return r;
        } catch (Exception e) {
            System.out.println("[Broker] Error al invocar servidor: " + e);
            return new Respuesta("Error al invocar servidor: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            BrokerImpl broker = new BrokerImpl();
            System.out.println("[Broker] Objeto creado.");

            Naming.rebind("//" + hostBroker + ":" + puertoBroker + "/Broker800",
                    broker);
            System.out.println("[Broker] Registrado en RMI como 'Broker800'. "
                    + "Esperando peticiones...");

        } catch (Exception e) {
            System.out.println("[Broker] Error: " + e);
            e.printStackTrace();
        }
    }

}
