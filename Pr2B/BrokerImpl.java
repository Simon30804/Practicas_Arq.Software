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
 * 
 * En el nivel avanzao, el broer mantiene un registro dinámico de qué servicios ofrece cada servidor, permitiendo
 * dar de alta y baja servicios sin necesidad de recompilar el Broker.
 */

public class BrokerImpl extends UnicastRemoteObject 
implements Broker {
    // Mapa para almacenar los servidores registrados (nombre_servidor -> host_remoto_IP_puerto)
    private Map<String, String> servidoresRegistrados = new HashMap<>(); // Mapa de servicios a servidores
    // Mapa para almacenar los servicios registrados (nombre_servicio -> InfoServicio)
    private Map<String, InfoServicio> serviciosRegistrados = new HashMap<>(); // Mapa de servicios a servidores

    private static class InfoServicio {
        String nombreServidor;
        String nombreServicio;
        List<Object> listaParametros;
        String tipoRetorno;

        public InfoServicio(String nombreServidor, String nombreServicio, List<Object> listaParametros, String tipoRetorno) {
            this.nombreServidor = nombreServidor;
            this.nombreServicio = nombreServicio;
            this.listaParametros = listaParametros;
            this.tipoRetorno = tipoRetorno;
        }
    }

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

    @Override 
    public void alta_servicio(String nombre_servidor, String nom_servicio, List<Object> lista_param, String tipo_retorno) throws RemoteException {
        // Verificar que el servidor esté registrado
        if (!servidoresRegistrados.containsKey(nombre_servidor)) {
            System.out.println("[Broker] ERROR: Intento de alta de servicio '"
                    + nom_servicio + "' de servidor no registrado: " + nombre_servidor);
            throw new RemoteException("Servidor no registrado: " + nombre_servidor);
        }

        InfoServicio info = new InfoServicio(nombre_servidor, nom_servicio, lista_param, tipo_retorno);
        serviciosRegistrados.put(nom_servicio, info);
        
        System.out.println("[Broker] Servicio registrado: " + nom_servicio
                + " del servidor " + nombre_servidor);
    }

    @Override
    public void baja_servicio(String nombre_servidor, String nom_servicio) throws RemoteException {
        InfoServicio info = serviciosRegistrados.get(nom_servicio);

        if (info == null) {
            System.out.println("[Broker] ERROR: Intento de baja de servicio no registrado: " + nom_servicio);
            throw new RemoteException("Servicio no registrado: " + nom_servicio);
        }

        if (!info.nombreServidor.equals(nombre_servidor)) {
            System.out.println("[Broker] ERROR: Intento de baja de servicio '"
                    + nom_servicio + "' por servidor no propietario: " + nombre_servidor);
            throw new RemoteException("Servidor no propietario del servicio: " + nombre_servidor);
        }

        serviciosRegistrados.remove(nom_servicio);
        System.out.println("[Broker] Servicio dado de baja: " + nom_servicio + " del servidor " + nombre_servidor);
    } 

    /* API para los clientes */

    /**
    * Lista todos los servicios actualmente registrados en el broker.
    * 
    * @return Objeto Servicios con la lista completa de servicios disponibles
    */
    @Override
    public Servicios lista_servicios() throws RemoteException {
        Servicios servicios = new Servicios();

        for(InfoServicio info : serviciosRegistrados.values()) {
            Servicios.Servicio servicio = new Servicios.Servicio(
                    info.nombreServidor,
                    info.nombreServicio,
                    info.listaParametros,
                    info.tipoRetorno
            );
            servicios.anadirServicio(servicio);
        }
        return servicios;
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
        InfoServicio servicioInfo = serviciosRegistrados.get(nom_servicio);
        String nombreServidor =  servicioInfo.nombreServidor;
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
