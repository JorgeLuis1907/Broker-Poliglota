package calculadora;

// ╔══════════════════════════════════════════════════════════════════╗
// ║  ServidorSuma.java — Microservicio de SUMA                      ║
// ║  Puerto: 5001                                                    ║
// ║  Solo sabe hacer una cosa: sumar. Simple, enfocado, testeable.   ║
// ╚══════════════════════════════════════════════════════════════════╝

import calculadora.grpc.SolicitudMath;
import calculadora.grpc.RespuestaMath;
import calculadora.grpc.ServicioCalculadoraGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class ServidorSuma {

    private static final int PUERTO = 5001;

    public static void main(String[] args) throws IOException, InterruptedException {

        Server servidor = ServerBuilder
                .forPort(PUERTO)
                .addService(new OperacionSuma())
                .build()
                .start();

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║   MICROSERVICIO SUMA - LISTO     ║");
        System.out.println("║   Puerto: " + PUERTO + "  Lenguaje: Java   ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("  Esperando solicitudes del Broker...\n");

        servidor.awaitTermination();
    }

    // ── Implementación del servicio gRPC para SUMA ───────────────────
    static class OperacionSuma
            extends ServicioCalculadoraGrpc.ServicioCalculadoraImplBase {

        @Override
        public void calcular(SolicitudMath solicitud,
                             StreamObserver<RespuestaMath> observadorRespuesta) {

            double numero1   = solicitud.getNumero1();
            double numero2   = solicitud.getNumero2();
            double resultado = numero1 + numero2;

            System.out.println("  [SUMA] Calculando: " + numero1 + " + " + numero2 + " = " + resultado);

            // Construcción de la respuesta exitosa
            RespuestaMath respuesta = RespuestaMath.newBuilder()
                    .setResultado(resultado)
                    .setEsExitoso(true)
                    .setMensajeError("")    // Vacío porque no hay error
                    .build();

            observadorRespuesta.onNext(respuesta);
            observadorRespuesta.onCompleted();
        }
    }
}
