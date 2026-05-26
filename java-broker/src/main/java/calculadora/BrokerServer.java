package calculadora;

import calculadora.grpc.SolicitudMath;
import calculadora.grpc.RespuestaMath;
import calculadora.grpc.ServicioCalculadoraGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BrokerServer {

    private static final int PUERTO_BROKER         = 5000;
    private static final int PUERTO_SUMA           = 5001;
    private static final int PUERTO_RESTA          = 5002;
    private static final int PUERTO_MULTIPLICACION = 5003;
    private static final int PUERTO_DIVISION       = 5004;
    private static final String HOST_LOCAL         = "localhost";

    // ── CANALES ESTÁTICOS REUTILIZABLES (Solución al agotamiento de red) ──
    private static ManagedChannel canalSuma;
    private static ManagedChannel canalResta;
    private static ManagedChannel canalMultiplica;
    private static ManagedChannel canalDivision;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("java.awt.headless", "true");

        // 1. Inicializamos los canales una sola vez al arrancar el Broker
        canalSuma = ManagedChannelBuilder.forAddress(HOST_LOCAL, PUERTO_SUMA).usePlaintext().build();
        canalResta = ManagedChannelBuilder.forAddress(HOST_LOCAL, PUERTO_RESTA).usePlaintext().build();
        canalMultiplica = ManagedChannelBuilder.forAddress(HOST_LOCAL, PUERTO_MULTIPLICACION).usePlaintext().build();
        canalDivision = ManagedChannelBuilder.forAddress(HOST_LOCAL, PUERTO_DIVISION).usePlaintext().build();

        // 2. Levantamos el servidor Broker
        Server servidorBroker = ServerBuilder
                .forPort(PUERTO_BROKER)
                .addService(new ManejadorDePeticiones())
                .build()
                .start();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       BROKER CENTRAL - INICIADO          ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Canales persistentes gRPC: ACTIVOS      ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("  Esperando peticiones continuas...\n");

        // Registrar hook para apagar los canales limpiamente si se cierra el Broker
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[BROKER] Cerrando canales gRPC...");
            canalSuma.shutdown();
            canalResta.shutdown();
            canalMultiplica.shutdown();
            canalDivision.shutdown();
        }));

        servidorBroker.awaitTermination();
    }

    static class ManejadorDePeticiones
            extends ServicioCalculadoraGrpc.ServicioCalculadoraImplBase {

        @Override
        public void calcular(SolicitudMath solicitud, StreamObserver<RespuestaMath> observadorRespuesta) {

            String operacion = solicitud.getOperacion().toUpperCase().trim();
            double numero1   = solicitud.getNumero1();
            double numero2   = solicitud.getNumero2();

            System.out.println("──────────────────────────────────────────");
            System.out.println("  [BROKER] Petición recibida: " + operacion + " (" + numero1 + ", " + numero2 + ")");

            RespuestaMath respuesta;

            // ── POLÍTICA 1: FAIL-FAST LOCALLY ──
            if (operacion.equals("DIVISION") && numero2 == 0) {
                System.out.println("  [BROKER] ⚠ FAIL-FAST: División entre cero.");
                respuesta = RespuestaMath.newBuilder()
                        .setEsExitoso(false)
                        .setMensajeError("Error: División entre cero inválida")
                        .setResultado(0)
                        .build();
                observadorRespuesta.onNext(respuesta);
                observadorRespuesta.onCompleted();
                return;
            }

            // Seleccionar el canal ya existente correspondiente
            ManagedChannel canalDestino = obtenerCanalDestino(operacion);

            if (canalDestino == null) {
                System.out.println("  [BROKER] ✗ Operación desconocida: " + operacion);
                respuesta = RespuestaMath.newBuilder()
                        .setEsExitoso(false)
                        .setMensajeError("Error: Operación '" + operacion + "' no reconocida.")
                        .setResultado(0)
                        .build();
                observadorRespuesta.onNext(respuesta);
                observadorRespuesta.onCompleted();
                return;
            }

            // ── POLÍTICA 2: TOLERANCIA A FALLOS USANDO CANAL PERSISTENTE ──
            try {
                // Usamos el canal persistente sin cerrarlo en el finally
                ServicioCalculadoraGrpc.ServicioCalculadoraBlockingStub stubCliente =
                        ServicioCalculadoraGrpc.newBlockingStub(canalDestino)
                        .withDeadlineAfter(3, TimeUnit.SECONDS);

                respuesta = stubCliente.calcular(solicitud);
                System.out.println("  [BROKER] ✓ Procesado con éxito.");

            } catch (StatusRuntimeException excepcionRed) {
                String nombreServicio = obtenerNombreServicio(operacion);
                String lenguaje       = operacion.equals("DIVISION") ? "Python" : "Java";
                String mensajeError   = "El microservicio de " + nombreServicio + " (" + lenguaje + ") no se encuentra disponible";

                System.out.println("  [BROKER] ✗ Nodo caído - " + mensajeError);

                respuesta = RespuestaMath.newBuilder()
                        .setEsExitoso(false)
                        .setMensajeError(mensajeError)
                        .setResultado(0)
                        .build();
            }

            observadorRespuesta.onNext(respuesta);
            observadorRespuesta.onCompleted();
        }

        private ManagedChannel obtenerCanalDestino(String operacion) {
            switch (operacion) {
                case "SUMA":           return canalSuma;
                case "RESTA":          return canalResta;
                case "MULTIPLICACION": return canalMultiplica;
                case "DIVISION":       return canalDivision;
                default:               return null;
            }
        }

        private String obtenerNombreServicio(String operacion) {
            switch (operacion) {
                case "SUMA":           return "SUMA";
                case "RESTA":          return "RESTA";
                case "MULTIPLICACION": return "MULTIPLICACIÓN";
                case "DIVISION":       return "DIVISIÓN";
                default:               return operacion;
            }
        }
    }
}