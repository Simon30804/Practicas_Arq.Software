import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/*
* Clase para representar la lista de servicios disponibles en el Broker. Esta clase se utiliza para enviar al cliente la información de los servicios registrados en el Broker.
 */
public class Servicios implements Serializable {
    /* 
    * Clase interna para representar la información de cada servicio registrado en el Broker. Contiene el nombre del servidor que ofrece el servicio, el nombre del servicio, la lista de parámetros necesarios para su ejecución y el tipo de retorno del servicio.
    */
    public static class Servicio implements Serializable {
        private String nombreServidor;
        private String nombreServicio;
        private List<Object> listaParametros;
        private String tipoRetorno;

        public Servicio(String nombreServidor, String nombreServicio, List<Object> listaParametros, String tipoRetorno) {
            this.nombreServidor = nombreServidor;
            this.nombreServicio = nombreServicio;
            this.listaParametros = listaParametros;
            this.tipoRetorno = tipoRetorno;
        }

        public String getNombreServicio() {
            return nombreServicio;
        }

        public String getNombreServidor() {
            return nombreServidor;
        }

        public List<Object> getListaParametros() {
            return listaParametros;
        }
        public String getTipoRetorno() {
            return tipoRetorno;
        }

        @Override
        public String toString() {
            return "Servicio{" +
                    "nombreServidor='" + nombreServidor + '\'' +
                    ", nombreServicio='" + nombreServicio + '\'' +
                    ", listaParametros=" + listaParametros +
                    ", tipoRetorno='" + tipoRetorno + '\'' +
                    '}';
        }
    }

    private List<Servicio> servicios;

    public Servicios() {
        this.servicios = new ArrayList<>();
    }

    public void anadirServicio(Servicio servicio) {
        servicios.add(servicio);
    }

    public List<Servicio> getServicios() {
        return servicios;
    }

    public int getNumeroServicios() {
        return servicios.size();
    }

    @Override
    public String toString() {
        return "Servicios{" +
                "servicios=" + servicios +
                '}';
    }    
}
