//TODO: imports necesarios
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Naming;

public class Cliente {
    public static void main(String[] args){
        try{
            //Paso 1-Obtener una referencia al objeto servidor creado anteriormente
            //Nombre del host servidor o su IP. Es dónde se buscará al objeto remoto
            String hostname = "localhost"; //se puede usar "IP: puerto"
            Collection server = (Collection) Naming.lookup("//"+hostname + "/MyCollection");
            System.out.println("[Cliente] Conexión establecida con el servidor.");

            //Paso 2-Invocar remotamente los metodos del objeto servidor:
            //TODO: obtener el nombre de la colección y el número de libros
            String nombreColeccion = server.name_of_collection();
            int numeroLibros = server.number_of_books();
            System.out.println("[Cliente] Nombre de la colección: " + nombreColeccion);
            System.out.println("[Cliente] Número de libros: " + numeroLibros);
            
            //TODO: cambiar el nombre de la coleccion y ver que ha funcionado
            String nuevoNombre = "Colección de Libro del Zaragoza";
            server.name_of_collection(nuevoNombre);
            String nombreColeccionActualizado = server.name_of_collection();
            System.out.println("[Cliente] Nombre de la colección actualizado: " + nombreColeccionActualizado);

        }
        catch (Exception ex){
            System.out.println("[Cliente] Error: " + ex);
            System.out.println(ex);
        }
    }
}
