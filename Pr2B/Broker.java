import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List; 

/**
 * Interfaz remota del Broker.
 * Define la API que ofrece el Broker tanto a los servidores (registro)
 * como a los clientes (ejecucción de servicios).
 */
public interface Broker extends Remote {
    /**
     * Permite a un servidor registrarse en el Broker indicando su nombre
     * y su dirección (IP:puerto) donde está escuchando en RMI.
     */
    void registrar_servidor(String nombre_servidor, String host_remoto_IP_puerto)
            throws RemoteException;

    /**
     * Permite a un cliente solicitar la ejecución de un servicio remoto.
     * El Broker localiza el servidor correspondiente y realiza la invocación.
     *
     * @param nom_servicio       Nombre del servicio a ejecutar
     * @param parametros_servicio Lista de parámetros necesarios para el servicio
     * @return Objeto Respuesta con el resultado de la ejecución
     */
    Respuesta ejecutar_servicio(String nom_servicio, List<Object> parametros_servicio)
            throws RemoteException;
}
