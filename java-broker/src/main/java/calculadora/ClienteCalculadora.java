package calculadora;

// ╔══════════════════════════════════════════════════════════════════╗
// ║  ClienteCalculadora.java — Interfaz de usuario por consola      ║
// ║                                                                  ║
// ║  FLUJO:                                                          ║
// ║  1. Muestra un menú de operaciones.                              ║
// ║  2. Pide los dos números al usuario.                             ║
// ║  3. Empaqueta todo en una SolicitudMath.                         ║
// ║  4. La envía al BROKER en el puerto 5000.                        ║
// ║  5. Muestra el resultado o el error de forma clara.              ║
// ╚══════════════════════════════════════════════════════════════════╝

import calculadora.grpc.SolicitudMath;
import calculadora.grpc.RespuestaMath;
import calculadora.grpc.ServicioCalculadoraGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ClienteCalculadora {

    // El cliente SOLO conoce al Broker, no a los microservicios directamente
    private static final String HOST_BROKER  = "localhost";
    private static final int    PUERTO_BROKER = 5000;

    public static void main(String[] args) throws InterruptedException {

        // ── Abrimos canal de comunicación con el Broker ───────────────
        ManagedChannel canalBroker = ManagedChannelBuilder
                .forAddress(HOST_BROKER, PUERTO_BROKER)
                .usePlaintext()
                .build();

        // ── Stub bloqueante: espera la respuesta antes de continuar ───
        ServicioCalculadoraGrpc.ServicioCalculadoraBlockingStub stubBroker =
                ServicioCalculadoraGrpc.newBlockingStub(canalBroker)
                .withDeadlineAfter(10, TimeUnit.SECONDS);

        Scanner lectorTeclado = new Scanner(System.in);
        boolean continuarEjecutando = true;

        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║       CALCULADORA DISTRIBUIDA con gRPC        ║");
        System.out.println("║   Cliente → Broker → Microservicio correcto   ║");
        System.out.println("╚═══════════════════════════════════════════════╝");

        while (continuarEjecutando) {

            // ── Menú principal ─────────────────────────────────────────
            System.out.println("\n┌─────────────────────────────────────┐");
            System.out.println("│         SELECCIONE OPERACIÓN         │");
            System.out.println("├─────────────────────────────────────┤");
            System.out.println("│  1. SUMA           (puerto 5001)     │");
            System.out.println("│  2. RESTA          (puerto 5002)     │");
            System.out.println("│  3. MULTIPLICACION (puerto 5003)     │");
            System.out.println("│  4. DIVISION       (puerto 5004)     │");
            System.out.println("│  5. SALIR                            │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.print("  Opción → ");

            String opcionIngresada = lectorTeclado.nextLine().trim();

            // ── Mapear opción del menú a nombre de operación ──────────
            String operacionSeleccionada;
            switch (opcionIngresada) {
                case "1": operacionSeleccionada = "SUMA";           break;
                case "2": operacionSeleccionada = "RESTA";          break;
                case "3": operacionSeleccionada = "MULTIPLICACION"; break;
                case "4": operacionSeleccionada = "DIVISION";       break;
                case "5":
                    System.out.println("\n  ¡Hasta luego! Cerrando conexión con el Broker...");
                    continuarEjecutando = false;
                    continue;
                default:
                    System.out.println("  ✗ Opción no válida. Elija entre 1 y 5.");
                    continue;
            }

            // ── Solicitar los dos números ──────────────────────────────
            double numero1, numero2;
            try {
                System.out.print("  Ingrese el primer número  → ");
                numero1 = Double.parseDouble(lectorTeclado.nextLine().trim());

                System.out.print("  Ingrese el segundo número → ");
                numero2 = Double.parseDouble(lectorTeclado.nextLine().trim());
            } catch (NumberFormatException excepcionFormato) {
                System.out.println("  ✗ Error: debe ingresar números válidos (ej: 10, 3.5).");
                continue;
            }

            // ── Construir la solicitud gRPC ────────────────────────────
            SolicitudMath solicitud = SolicitudMath.newBuilder()
                    .setNumero1(numero1)
                    .setNumero2(numero2)
                    .setOperacion(operacionSeleccionada)
                    .build();

            System.out.println("\n  ─── Enviando al Broker (puerto " + PUERTO_BROKER + ") ───");
            System.out.println("  Solicitud: " + numero1 + " " + simboloOperacion(operacionSeleccionada) + " " + numero2);

            // ── Enviar al Broker y recibir respuesta ───────────────────
            try {
                RespuestaMath respuesta = stubBroker.calcular(solicitud);

                // ── Mostrar resultado ──────────────────────────────────
                System.out.println("\n  ┌──────────────── RESPUESTA DEL SISTEMA ─────────────────┐");
                if (respuesta.getEsExitoso()) {
                    System.out.println("  │  Estado  : ✓ ÉXITO                                     │");
                    System.out.printf( "  │  Resultado: %-42.6f│%n", respuesta.getResultado());
                } else {
                    System.out.println("  │  Estado  : ✗ ERROR                                      │");
                    System.out.println("  │  Detalle : " + respuesta.getMensajeError());
                }
                System.out.println("  └─────────────────────────────────────────────────────────┘");

            } catch (Exception excepcionBroker) {
                // El Broker mismo no está disponible
                System.out.println("\n  ✗ ERROR CRÍTICO: No se puede conectar al Broker.");
                System.out.println("    Asegúrese de que BrokerServer esté corriendo en el puerto " + PUERTO_BROKER);
                System.out.println("    Detalle: " + excepcionBroker.getMessage());
            }
        }

        // ── Liberar el canal antes de salir ───────────────────────────
        canalBroker.shutdown();
        System.out.println("\n  Canal cerrado. Aplicación terminada correctamente.");
    }

    // ── Helper: símbolo visual de la operación ────────────────────────
    private static String simboloOperacion(String operacion) {
        switch (operacion) {
            case "SUMA":           return "+";
            case "RESTA":          return "-";
            case "MULTIPLICACION": return "×";
            case "DIVISION":       return "÷";
            default:               return "?";
        }
    }
}
