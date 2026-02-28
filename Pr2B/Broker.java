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

    /**
     * Solicita la ejecución ASÍNCRONA de un servicio.
     * El cliente NO espera la respuesta, puede continuar haciendo otras cosas.
     * La respuesta se almacena en el broker hasta que el cliente la solicite.
     * 
     * RESTRICCIÓN: Un cliente no puede solicitar el mismo servicio dos veces
     * sin haber recogido la respuesta anterior.
     * 
     * @param clienteId Identificador único del cliente que hace la petición
     * @param nom_servicio Nombre del servicio a ejecutar
     * @param parametros_servicio Lista de parámetros necesarios
     */
    void ejecutar_servicio_asinc(String clienteId, String nom_servicio,
                                 List<Object> parametros_servicio)
            throws RemoteException;

    /**
     * Obtiene la respuesta de una ejecución asíncrona previa.
     * 
     * ERRORES que puede devolver:
     * - El cliente no había solicitado previamente el servicio
     * - El cliente que solicita la respuesta no es el mismo que hizo la petición
     * - La respuesta ya fue entregada anteriormente
     * - La respuesta aún no está disponible (servicio en ejecución)
     * 
     * @param clienteId Identificador del cliente que solicita la respuesta
     * @param nom_servicio Nombre del servicio del que se quiere obtener respuesta
     * @return Respuesta con el resultado o un mensaje de error
     */
    Respuesta obtener_respuesta_asinc(String clienteId, String nom_servicio)
            throws RemoteException;
            
}
