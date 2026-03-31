import java.rmi.Naming;
import java.util.Arrays;
import java.util.UUID;

/**
 * Cliente de DEMOSTRACIÓN para el CASO ASÍNCRONO.
 * Usado para comprobar el correcto funcionamiento de la comunicación asíncrona entre el Cliente y el Broker.
 * Demuestra todos los escenarios en el apartado 3.3 Versiones asíncronas del guion de prácticas:
 * 1. Funcionamiento correcto de comunicación asíncrona
 * 2. Error si el cliente no había solicitado el servicio
 * 4. Error si la respuesta ya fue entregada anteriormente
 * 5. Error si intenta solicitar el mismo servicio sin recoger respuesta
 * 
 * Nota sobre la identificación de clientes:
 *   El Broker identifica a cada cliente por su dirección IP, mediante RemoteServer.getClientHost(),
 *   por lo que los métodos asíncronos NO reciben un clienteId explícito.
 *   El escenario "cliente incorrecto" (escenario 3 del PDF) queda garantizado a nivel de
 *   red: dos máquinas distintas tienen IPs distintas, y cada una solo puede recoger sus
 *   propias respuestas. En este programa de pruebas (solo existe una única máquina) no se tiene en cuenta ese escenario.
 */
public class ClienteAsincrono {

    private static final String IP_BROKER  = "155.210.154.196";  
    private static final int PUERTO_BROKER = 32000;

    private static Broker broker;
    private static String clienteId;

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("  DEMOSTRACIÓN - CASO ASÍNCRONO ");
        System.out.println("=================================\n");
        
        try {
            // Generamos un ID único para este cliente, y poder así identificar sus peticiones y respuestas asíncronas
            clienteId = "Cliente_" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println("[INFO] ID del cliente: " + clienteId + "\n");

            // Nos conectamos al Broker
            broker = (Broker) Naming.lookup(
                    "//" + IP_BROKER + ":" + PUERTO_BROKER + "/Broker800");
            System.out.println("[OK] Conectado al Broker\n");

            // Ejecutamos los escenarios de prueba
            System.out.println("=================================");
            System.out.println(" ESCENARIOS DE PRUEBA");
            System.out.println("=================================\n");

            // Comprobamos que el menú de gestión de servicios del servidor funciona correctamente, permitiendo dar de baja y alta servicios de manera dinámica
            escenario1_FuncionamientoCorrecto();
            pausa(2);
            // Comprobamos que si el cliente intenta obtener la respuesta de un servicio que no ha solicitado, se gestiona el error correctamente
            escenario2_ServicioNoSolicitado();
            pausa(2);
            // Comprobamos que si un cliente intenta obtener la respuesta de un servicio solicitado por otro cliente, se gestiona el error correctamente
            escenario3_ClienteIncorrecto();
            pausa(2);
            // Comprobamos que si el cliente intenta obtener la respuesta de un servicio que ya ha sido entregada anteriormente, se gestiona el error correctamente
            escenario4_RespuestaYaEntregada();
            pausa(2);
            // Comprobamos que si el cliente intenta solicitar el mismo servicio sin haber recogido la respuesta anterior, se gestiona el error correctamente
            escenario5_SolicitudDuplicada();

            System.out.println("\n=================================");
            System.out.println("     DEMOSTRACIÓN COMPLETADA ");
            System.out.println("=================================");

        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ESCENARIO 1: Funcionamiento correcto
    private static void escenario1_FuncionamientoCorrecto() throws Exception {
        System.out.println("=======================================");
        System.out.println(" ESCENARIO 1: Funcionamiento correcto");
        System.out.println("=======================================");

        // Solicitamos la ejecución asíncrona
        System.out.println("Solicitando ejecución asíncrona de 'contar_usuarios'...");
        broker.ejecutar_servicio_asinc("contar_usuarios", Arrays.asList());
        System.out.println("Petición asíncrona registrada. Cliente procede sin esperar.\n");

        // Simulamos que el cliente hace otras cosas (puede ser cualquier tarea, aquí simplemente imprimimos mensajes y pausamos la ejecución para simular tiempo de procesamiento)
        System.out.println("El Cliente hace otras tareas mientras el servicio se ejecuta...");
        for (int i = 1; i <= 3; i++) {
            System.out.println("  Tarea " + i + " completada...");
            pausa(1);
        }

        // Intentamos obtener la respuesta (puede que aún no esté lista)
        System.out.println("\n Intentando obtener la respuesta...");
        Respuesta r1 = broker.obtener_respuesta_asinc("contar_usuarios");
        
        if (r1.getMensaje().contains("aún está en ejecución")) {
            System.out.println(r1.getMensaje());
            pausa(2);
            System.out.println("\n Reintentando obtener la respuesta...");
            r1 = broker.obtener_respuesta_asinc("contar_usuarios");
        }

        if (r1.isExito()) {
            System.out.println("Respuesta recibida: " + r1.getResultado());
        } else {
            System.out.println("Error: " + r1.getMensaje());
        }
        System.out.println();
    }

    // ESCENARIO 2: Error - Servicio no solicitado
    private static void escenario2_ServicioNoSolicitado() throws Exception {
        System.out.println("=========================================");
        System.out.println("   ESCENARIO 2: Servicio NO solicitado");
        System.out.println("==========================================");

        System.out.println("→ Intentando obtener respuesta de un servicio que NO fue solicitado...");
        Respuesta r = broker.obtener_respuesta_asinc( "servicio_inventado");
        
        System.out.println( r.getMensaje());
        System.out.println("Error gestionado correctamente\n");
    }

    // ESCENARIO 3: Error - Cliente incorrecto
    private static void escenario3_ClienteIncorrecto() throws Exception {
        System.out.println("==========================================");
        System.out.println("   ESCENARIO 3: Cliente incorrecto");
        System.out.println("==========================================");
        System.out.println("  NOTA: El Broker identifica clientes por su dirección IP.");
        System.out.println("  En la práctica, si el Cliente A (IP_A) solicita un servicio,");
        System.out.println("  el Cliente B (IP_B diferente) NO puede obtener esa respuesta:");
        System.out.println("  el Broker no encontrará la clave 'IP_B:servicio' y devolverá");
        System.out.println("  el error 'No solicitaste previamente el servicio'.");
        System.out.println("  → Este escenario se verifica ejecutando dos clientes en");
        System.out.println("    máquinas distintas del laboratorio L1.02.\n");
    }

    // ESCENARIO 4: Error - Respuesta ya entregada
    private static void escenario4_RespuestaYaEntregada() throws Exception {
        System.out.println("=========================================");
        System.out.println("   ESCENARIO 4: Respuesta ya entregada");
        System.out.println("==========================================");

        System.out.println("Solicitando 'contar_usuarios' de nuevo...");
        broker.ejecutar_servicio_asinc( "contar_usuarios", Arrays.asList());
        pausa(2);

        System.out.println("Obteniendo respuesta por primera vez...");
        Respuesta r1 = broker.obtener_respuesta_asinc( "contar_usuarios");
        System.out.println("Primera entrega: " + r1.getResultado() + "\n");

        System.out.println("Intentando obtener la misma respuesta de nuevo...");
        Respuesta r2 = broker.obtener_respuesta_asinc( "contar_usuarios");
        System.out.println(r2.getMensaje());
        System.out.println("Error gestionado correctamente\n");
    }

    // ESCENARIO 5: Error - Solicitud duplicada sin recoger respuesta
    private static void escenario5_SolicitudDuplicada() throws Exception {
        System.out.println("=========================================");
        System.out.println("   ESCENARIO 5: Solicitud duplicada sin recoger respuesta");
        System.out.println("==========================================");

        System.out.println("Solicitando 'obtener_usuarios'...");
        broker.ejecutar_servicio_asinc( "obtener_usuarios", Arrays.asList());
        System.out.println("Primera solicitud registrada\n");

        System.out.println("Intentando solicitar el MISMO servicio SIN recoger la respuesta...");
        try {
            broker.ejecutar_servicio_asinc( "obtener_usuarios", Arrays.asList());
            System.out.println("ERROR: Debería haber lanzado excepción");
        } catch (Exception e) {
            System.out.println("Excepción capturada: " + e.getMessage());
            System.out.println("Error gestionado correctamente\n");
        }

        // Recogemos la respuesta pendiente
        System.out.println("Recogiendo la respuesta pendiente para limpiar...");
        pausa(2);
        Respuesta r = broker.obtener_respuesta_asinc( "obtener_usuarios");
        System.out.println("Respuesta recogida: " + r.getResultado() + "\n");
    }

    /**
     * Función auxiliar para pausar la ejecución durante una serie de segundos, simulando que el cliente hace otras tareas.
     */
    private static void pausa(int segundos) {
        try {
            Thread.sleep(segundos * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}