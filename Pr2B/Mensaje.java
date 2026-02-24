import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase serializable que representa un mensaje en el foro.
 */
public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;              // ID único del mensaje
    private String usuario;      // Usuario que envió el mensaje
    private String texto;        // Contenido del mensaje
    private long timestamp;      // Fecha de envío del mensaje

    public Mensaje(int id, String usuario, String texto) {
        this.id = id;
        this.usuario = usuario;
        this.texto = texto;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getTexto() { return texto; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return "[" + sdf.format(new Date(timestamp)) + "] " + usuario + ": " + texto;
    }
}