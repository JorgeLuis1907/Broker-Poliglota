package calculadora;

// ╔══════════════════════════════════════════════════════════════════╗
// ║  ServidorMultiplica.java — Microservicio de MULTIPLICACION      ║
// ║  Puerto: 5003                                                    ║
// ╚══════════════════════════════════════════════════════════════════╝

import calculadora.grpc.SolicitudMath;
import calculadora.grpc.RespuestaMath;
import calculadora.grpc.ServicioCalculadoraGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class ServidorMultiplica {

    private static final int PUERTO = 5003;

    public static void main(String[] args) throws IOException, InterruptedException {

        Server servidor = ServerBuilder
                .forPort(PUERTO)
                .addService(new OperacionMultiplicacion())
                .build()
                .start();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  MICROSERVICIO MULTIPLICACION - LISTO    ║");
        System.out.println("║  Puerto: " + PUERTO + "   Lenguaje: Java          ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("  Esperando solicitudes del Broker...\n");

        servidor.awaitTermination();
    }

    // ── Implementación del servicio gRPC para MULTIPLICACION ─────────
    static class OperacionMultiplicacion
            extends ServicioCalculadoraGrpc.ServicioCalculadoraImplBase {

        @Override
        public void calcular(SolicitudMath solicitud,
                             StreamObserver<RespuestaMath> observadorRespuesta) {

            double numero1   = solicitud.getNumero1();
            double numero2   = solicitud.getNumero2();
            double resultado = numero1 * numero2;

            System.out.println("  [MULTIPLICACION] Calculando: " + numero1 + " × " + numero2 + " = " + resultado);

            RespuestaMath respuesta = RespuestaMath.newBuilder()
                    .setResultado(resultado)
                    .setEsExitoso(true)
                    .setMensajeError("")
                    .build();

            observadorRespuesta.onNext(respuesta);
            observadorRespuesta.onCompleted();
        }
    }
}
