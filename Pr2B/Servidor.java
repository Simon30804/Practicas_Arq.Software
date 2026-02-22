import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/** 
 * Interfaz remota común  del Servidor que implementan todos los serviodres.
 * El Broker invoca el método ejecutarServicio() para solicitar la ejecución de un servicio remoto, 
 * sin tener que conocer los detalles de implementación del los serivdores. 
 * El servidor implementa la lógica para ejecutar el servicio solicitado y devuelve una Respuesta con el resultado.
 */
public interface Servidor extends Remote {
    // Métodos de la interfaz del servidor
   /**
    * Ejecuta un servicio remoto solicitado por el Broker.
    * @param nom_servicio Nombre del servicio a ejecutar
    * @param parametros Lista de parámetros necesarios para el servicio
    * @return Respuesta con el resultado de la ejecución del servicio
    * @throws RemoteException
    */
    Respuesta ejecutarServicio(String nom_servicio, List<Object> parametros) throws RemoteException;
}
