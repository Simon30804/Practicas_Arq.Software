
import java.io.Serializable;

/**
 * Clase serializable que representa la respuesta de un servicio ejecutado en el Broker.
 * Contiene el resultado de la ejecución, un indicador de éxito o error, y un mensaje descriptivo (útil en caso de error).
 */
public class Respuesta implements Serializable {
    private Object resultado;   // Resultado de la ejecución del servicio
    private boolean exito;      // true si la ejecución fue correcta, false si hubo error
    private String mensaje;     // Mensaje descriptivo 

    // Constructor para respuesta exitosa
    public Respuesta(Object resultado) {
        this.resultado = resultado;
        this.exito = true;
        this.mensaje = "Ejecución exitosa";
    }

    // Constructor para respuesta con error
    public Respuesta(String mensajeError) {
        this.resultado = null;
        this.exito = false;
        this.mensaje = mensajeError;
    }

    // Getters
    public Object getResultado() {
        return resultado;
    }

    public boolean isExito() {
        return exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    @Override
    public String toString() {
        if (exito) {
            return "[OK] Resultado=" + resultado + ", mensaje='" + mensaje + "'}";
        } else {
            return "[ERROR] Mensaje='" + mensaje + "'}";
        }
    }
}
