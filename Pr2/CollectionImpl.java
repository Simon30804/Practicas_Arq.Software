import java.rmi.RemoteException; 
import java.rmi.server.UnicastRemoteObject; 
import java.rmi.Naming;

public class CollectionImpl extends UnicastRemoteObject 
implements Collection {
    // Private member variables 
    private int m_number_of_books; 
    private String m_name_of_collection;

    // Constructor
    public CollectionImpl() throws RemoteException {
        super(); // Llamada al constructor de UnicastRemoteObject
        //Añadido por nosotros: inicializar las variables privadas
        m_number_of_books = 30;
        m_name_of_collection = "Mi colección de libros";
    }

    // TODO: Implementar todos los metodos de la interface remota 
    public int number_of_books() throws RemoteException {
        System.out.println("[Servidor] number_of_books() invocado -> " + m_number_of_books);
        return m_number_of_books;
    }

    public String name_of_collection() throws RemoteException {
        System.out.println("[Servidor] name_of_collection() invocado -> " + m_name_of_collection);
        return m_name_of_collection;
    }

    public void name_of_collection(String _new_value) throws RemoteException {
        System.out.println("[Servidor] name_of_collection(" + _new_value + ") invocado");        
        m_name_of_collection = _new_value;
    }

    public static void main(String[] args) {
        // Nombre o IP del host donde reside el objeto servidor 
        String hostName = "10.1.66.64:3007"; // se puede usar " IPhostremoto:puerto" 
        // Por defecto, RMI usa el puerto 1099

        try {
            // Crear objeto remoto
            CollectionImpl obj = new CollectionImpl();
            System.out.println("[Servidor] Objeto Creado!");
            // Registrar el objeto remoto 
            Naming.rebind("//" + hostName + "/MyCollection", obj);
            System.out.println("[Servidor] Objeto registrado como 'MyCollection'. Esperando clientes...");

        }
        catch (Exception ex) {
            System.out.println("[Servidor] Error: " + ex);
            System.out.println(ex);
        }
    }
}
