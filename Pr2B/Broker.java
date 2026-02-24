import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List; 

/**
 * Interfaz remota del Broker.
 * Define la API que ofrece el Broker tanto a los servidores (registro)
 * como a los clientes (ejecucción de servicios).
 */
public interface Broker extends Remote {
    /* API para los serviores */

    /**
     * Permite a un servidor registrarse en el Broker indicando su nombre
     * y su dirección (IP:puerto) donde está escuchando en RMI.
     */
    void registrar_servidor(String nombre_servidor, String host_remoto_IP_puerto)
            throws RemoteException;

     /**
     * Da de baja un servicio de forma dinámica.
     * 
     * @param nombre_servidor Nombre del servidor que ofrece el servicio
     * @param nom_servicio Nombre del servicio a eliminar
     */
    void alta_servicio(String nombre_servidor, String nom_servicio, List<Object> lista_param, String tipo_retorno)
            throws RemoteException;   
        
    void baja_servicio(String nombre_servidor, String nom_servicio)
            throws RemoteException;
        
    /* API para los Clientes */

     /**
     * Lista todos los servicios actualmente registrados en el broker.
     * 
     * @return Objeto Servicios con la lista completa de servicios disponibles
     */
    Servicios lista_servicios() throws RemoteException;

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
