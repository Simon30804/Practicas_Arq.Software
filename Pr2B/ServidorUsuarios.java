import java.rmi.Naming;
import java.rmi.Remote; 
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List; 
import java.util.Set;

/*
* Este servidor se va a encargar de la gestion e usuarios.
*/
public class ServidorUsuarios extends UnicastRemoteObject
implements Servidor {

    // IP y puerto del Broker para el registro del servidor
    private static final String IP_BROKER = "localhost"; // Lo tengo que cambiar
    private static final int PUERTO_BROKER = 32000;

    // IP y puerto donde el servidor está escuchando en RMI
    private static final String IP_SERVIDOR = "localhost"; // Lo tengo que cambiar
    private static final int PUERTO = 32002;

    // Nombre único del server para su registro en el Broker
    private static final String NOMBRE_SERVIDOR = "ServidorUsuarios8698";

    // Almacenamiento de los usuarios
    private Set<String> usuarios = new HashSet<>();

    // Constructor del servidor
    public ServidorUsuarios() throws RemoteException {
        super();
        // Añadir algunos usuarios 
        usuarios.add("admin");
        usuarios.add("diego");
    }

    @Override
    public Respuesta ejecutarServicio(String nom_servicio, List<Object> parametros) throws RemoteException {
        System.out.println("[ServidorUsuarios] Ejecutando servicio: " + nom_servicio
                + " con parámetros: " + parametros);
        try {
            switch (nom_servicio) {
                case "registrar_usuario":
                    return registrarUsuario(parametros);
                case "obtener_usuarios":
                    return obtenerUsuarios(parametros);
                case "contar_usuarios":
                    return contarUsuarios(parametros);
                default:
                    return new Respuesta("Servicio no reconocido en ServidorUsuarios: " + nom_servicio);
            }
        } catch (Exception e) {
            return new Respuesta("Error al ejecutar el servicio: " + e.getMessage());
        }
    }

    /*
    * Servicio para registrar un nuevo usuario en el sistema. Recibe como parámetro el nombre del usuario a registrar.
    * Devuelve una respuesta indicando si el registro ha sido exitoso o si ha habido algún error.
    * El servidor verifica que el nombre de usuario no esté vacío y que no exista ya en el sistema antes de registrarlo.
    */
    private Respuesta registrarUsuario(List<Object> parametros){
        String nombreUsuario = (String) parametros.get(0);

        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            System.out.println("[ServidorUsuarios] Usuario '" + nombreUsuario + "' no válido");
            return new Respuesta("El nombre de usuario no puede estar vacío");
        }
        if (usuarios.contains(nombreUsuario)) {
            System.out.println("[ServidorUsuarios] Usuario '" + nombreUsuario + "' ya existe");            
            return new Respuesta("El nombre de usuario ya existe");
        }
        usuarios.add(nombreUsuario);
        System.out.println("[ServidorUsuarios] Usuario '" + nombreUsuario + "' registrado correctamente");
        String m = "Usuario '" + nombreUsuario + "' registrado correctamente";
        return new Respuesta((Object) m);
    }

    /*
    * Servicio para obtener la lista de usuarios registrados en el sistema. No recibe parámetros.
    * Devuelve una respuesta con la lista de nombres de usuario registrados.
    * El servidor devuelve la lista de usuarios como un Set<String> para evitar duplicados y facilitar la gestión de usuarios.
    */
    private Respuesta obtenerUsuarios(List<Object> parametros){
        List<String> listaUsuarios = new ArrayList<>(usuarios);
        System.out.println("[ServidorUsuarios] Devolviendo " + listaUsuarios.size() + " usuarios");
        return new Respuesta(listaUsuarios);
    }

    /*
    * Servicio para contar el número de usuarios registrados en el sistema. No recibe parámetros.
    * Devuelve una respuesta con el número total de usuarios registrados.
    * El servidor simplemente devuelve el tamaño del conjunto de usuarios para obtener el conteo.
    */
    private Respuesta contarUsuarios(List<Object> parametros){
        int numeroUsuarios = usuarios.size();
        System.out.println("[ServidorUsuarios] Número de usuarios registrados: " + numeroUsuarios);
        return new Respuesta(numeroUsuarios);
    }

    public static void main(String[] args) {
        try {
            // Creamos y registramos el Objeto en el rmiregistry para que el Broker pueda localizarlo
            ServidorUsuarios servidor = new ServidorUsuarios();
            String direccionRMI = IP_SERVIDOR + ":" + PUERTO;
            Naming.rebind("//" + direccionRMI + "/" + NOMBRE_SERVIDOR, servidor);
            System.out.println("[ServidorUsuarios] Servidor registrado en RMI con nombre: " + NOMBRE_SERVIDOR);

            // Nos conectamos al Broker para registrar el servicio que ofrecemos
            Broker broker = (Broker) Naming.lookup("//" + IP_BROKER + ":" + PUERTO_BROKER + "/Broker800");
            broker.registrar_servidor(NOMBRE_SERVIDOR, direccionRMI);
            System.out.println("[ServidorUsuarios] Servicio registrado en el Broker: " + NOMBRE_SERVIDOR);

            // Registramos los serivicos que ofrecenis de manera dinámica para que el Broker los tenga disponibles para los clientes
            broker.alta_servicio(NOMBRE_SERVIDOR, "registrar_usuario", List.of("String nombreUsuario"), "String");
            System.out.println("[ServidorUsuarios] Servicio registrado en el Broker: registrar_usuario");
            broker.alta_servicio(NOMBRE_SERVIDOR, "obtener_usuarios", List.of(), "List<String>");
            System.out.println("[ServidorUsuarios] Servicio registrado en el Broker: obtener_usuarios");
            broker.alta_servicio(NOMBRE_SERVIDOR, "contar_usuarios", List.of(), "int");
            System.out.println("[ServidorUsuarios] Servicio registrado en el Broker: contar_usuarios");

        } catch (Exception e) {
            System.err.println("[ServidorUsuarios] Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
