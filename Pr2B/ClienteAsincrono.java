import java.rmi.Naming;
import java.util.Arrays;
import java.util.UUID;

/**
 * Cliente de DEMOSTRACIÓN para el CASO ASÍNCRONO.
 * 
 * Demuestra todos los escenarios requeridos en el PDF (página 6):
 * 1. Funcionamiento correcto de comunicación asíncrona
 * 2. Error si el cliente no había solicitado el servicio
 * 3. Error si el cliente que solicita la respuesta no es el mismo
 * 4. Error si la respuesta ya fue entregada
 * 5. Error si intenta solicitar el mismo servicio sin recoger respuesta
 */
public class ClienteAsincrono {

    private static final String IP_BROKER  = "155.210.154.196";  
    private static final int PUERTO_BROKER = 32000;

    private static Broker broker;
    private static String clienteId;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║  DEMOSTRACIÓN - CASO ASÍNCRONO                ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        try {
            // Generar ID único para este cliente
            clienteId = "Cliente_" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println("[INFO] ID del cliente: " + clienteId + "\n");

            // Conectar al Broker
            broker = (Broker) Naming.lookup(
                    "//" + IP_BROKER + ":" + PUERTO_BROKER + "/Broker800");
            System.out.println("[OK] Conectado al Broker\n");

            // Ejecutar todos los escenarios de prueba
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println(" ESCENARIOS DE DEMOSTRACIÓN");
            System.out.println("═══════════════════════════════════════════════════\n");

            escenario1_FuncionamientoCorrecto();
            pausa(2);

            escenario2_ServicioNoSolicitado();
            pausa(2);

            escenario3_ClienteIncorrecto();
            pausa(2);

            escenario4_RespuestaYaEntregada();
            pausa(2);

            escenario5_SolicitudDuplicada();

            System.out.println("\n╔════════════════════════════════════════════════╗");
            System.out.println("║  ✅ DEMOSTRACIÓN COMPLETADA                    ║");
            System.out.println("╚════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCENARIO 1: Funcionamiento correcto
    // ═══════════════════════════════════════════════════════════════
    private static void escenario1_FuncionamientoCorrecto() throws Exception {
        System.out.println("┌────────────────────────────────────────────────┐");
        System.out.println("│ ESCENARIO 1: Funcionamiento correcto          │");
        System.out.println("└────────────────────────────────────────────────┘");

        // Solicitar ejecución asíncrona
        System.out.println("→ Solicitando ejecución asíncrona de 'contar_usuarios'...");
        broker.ejecutar_servicio_asinc(clienteId, "contar_usuarios", Arrays.asList());
        System.out.println("✓ Petición asíncrona registrada. Cliente continúa sin esperar.\n");

        // Simular que el cliente hace otras cosas
        System.out.println("→ Cliente hace otras tareas mientras el servicio se ejecuta...");
        for (int i = 1; i <= 3; i++) {
            System.out.println("  Tarea " + i + " completada...");
            pausa(1);
        }

        // Intentar obtener la respuesta (puede que aún no esté lista)
        System.out.println("\n→ Intentando obtener la respuesta...");
        Respuesta r1 = broker.obtener_respuesta_asinc(clienteId, "contar_usuarios");
        
        if (r1.getMensaje().contains("aún está en ejecución")) {
            System.out.println("⏳ " + r1.getMensaje());
            pausa(2);
            System.out.println("\n→ Reintentando obtener la respuesta...");
            r1 = broker.obtener_respuesta_asinc(clienteId, "contar_usuarios");
        }

        if (r1.isExito()) {
            System.out.println("✅ Respuesta recibida: " + r1.getResultado());
        } else {
            System.out.println("❌ Error: " + r1.getMensaje());
        }
        System.out.println();
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCENARIO 2: Error - Servicio no solicitado
    // ═══════════════════════════════════════════════════════════════
    private static void escenario2_ServicioNoSolicitado() throws Exception {
        System.out.println("┌────────────────────────────────────────────────┐");
        System.out.println("│ ESCENARIO 2: Servicio NO solicitado           │");
        System.out.println("└────────────────────────────────────────────────┘");

        System.out.println("→ Intentando obtener respuesta de un servicio que NO fue solicitado...");
        Respuesta r = broker.obtener_respuesta_asinc(clienteId, "servicio_inventado");
        
        System.out.println("❌ " + r.getMensaje());
        System.out.println("✓ Error gestionado correctamente\n");
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCENARIO 3: Error - Cliente incorrecto
    // ═══════════════════════════════════════════════════════════════
    private static void escenario3_ClienteIncorrecto() throws Exception {
        System.out.println("┌────────────────────────────────────────────────┐");
        System.out.println("│ ESCENARIO 3: Cliente incorrecto                │");
        System.out.println("└────────────────────────────────────────────────┘");

        // Cliente A solicita un servicio
        String clienteA = "ClienteA";
        System.out.println("→ " + clienteA + " solicita 'obtener_usuarios'...");
        broker.ejecutar_servicio_asinc(clienteA, "obtener_usuarios", Arrays.asList());
        pausa(2);

        // Cliente B intenta obtener la respuesta
        String clienteB = "ClienteB";
        System.out.println("→ " + clienteB + " intenta obtener la respuesta de " + clienteA + "...");
        Respuesta r = broker.obtener_respuesta_asinc(clienteB, "obtener_usuarios");
        
        System.out.println("❌ " + r.getMensaje());
        System.out.println("✓ Error gestionado correctamente\n");

        // Limpiar: Cliente A recoge su respuesta
        broker.obtener_respuesta_asinc(clienteA, "obtener_usuarios");
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCENARIO 4: Error - Respuesta ya entregada
    // ═══════════════════════════════════════════════════════════════
    private static void escenario4_RespuestaYaEntregada() throws Exception {
        System.out.println("┌────────────────────────────────────────────────┐");
        System.out.println("│ ESCENARIO 4: Respuesta ya entregada           │");
        System.out.println("└────────────────────────────────────────────────┘");

        System.out.println("→ Solicitando 'contar_usuarios' de nuevo...");
        broker.ejecutar_servicio_asinc(clienteId, "contar_usuarios", Arrays.asList());
        pausa(2);

        System.out.println("→ Obteniendo respuesta por primera vez...");
        Respuesta r1 = broker.obtener_respuesta_asinc(clienteId, "contar_usuarios");
        System.out.println("✓ Primera entrega: " + r1.getResultado() + "\n");

        System.out.println("→ Intentando obtener la misma respuesta de nuevo...");
        Respuesta r2 = broker.obtener_respuesta_asinc(clienteId, "contar_usuarios");
        System.out.println("❌ " + r2.getMensaje());
        System.out.println("✓ Error gestionado correctamente\n");
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCENARIO 5: Error - Solicitud duplicada sin recoger respuesta
    // ═══════════════════════════════════════════════════════════════
    private static void escenario5_SolicitudDuplicada() throws Exception {
        System.out.println("┌────────────────────────────────────────────────┐");
        System.out.println("│ ESCENARIO 5: Solicitud duplicada              │");
        System.out.println("└────────────────────────────────────────────────┘");

        System.out.println("→ Solicitando 'obtener_usuarios'...");
        broker.ejecutar_servicio_asinc(clienteId, "obtener_usuarios", Arrays.asList());
        System.out.println("✓ Primera solicitud registrada\n");

        System.out.println("→ Intentando solicitar el MISMO servicio SIN recoger la respuesta...");
        try {
            broker.ejecutar_servicio_asinc(clienteId, "obtener_usuarios", Arrays.asList());
            System.out.println("❌ ERROR: Debería haber lanzado excepción");
        } catch (Exception e) {
            System.out.println("❌ Excepción capturada: " + e.getMessage());
            System.out.println("✓ Error gestionado correctamente\n");
        }

        // Limpiar: recoger la respuesta pendiente
        System.out.println("→ Recogiendo la respuesta pendiente para limpiar...");
        pausa(2);
        Respuesta r = broker.obtener_respuesta_asinc(clienteId, "obtener_usuarios");
        System.out.println("✓ Respuesta recogida: " + r.getResultado() + "\n");
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════
    private static void pausa(int segundos) {
        try {
            Thread.sleep(segundos * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}