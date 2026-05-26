package calculadora;

// ╔══════════════════════════════════════════════════════════════════╗
// ║  ServidorResta.java — Microservicio de RESTA                    ║
// ║  Puerto: 5002                                                    ║
// ╚══════════════════════════════════════════════════════════════════╝

import calculadora.grpc.SolicitudMath;
import calculadora.grpc.RespuestaMath;
import calculadora.grpc.ServicioCalculadoraGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class ServidorResta {

    private static final int PUERTO = 5002;

    public static void main(String[] args) throws IOException, InterruptedException {

        Server servidor = ServerBuilder
                .forPort(PUERTO)
                .addService(new OperacionResta())
                .build()
                .start();

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║   MICROSERVICIO RESTA - LISTO    ║");
        System.out.println("║   Puerto: " + PUERTO + "  Lenguaje: Java   ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("  Esperando solicitudes del Broker...\n");

        servidor.awaitTermination();
    }

    // ── Implementación del servicio gRPC para RESTA ──────────────────
    static class OperacionResta
            extends ServicioCalculadoraGrpc.ServicioCalculadoraImplBase {

        @Override
        public void calcular(SolicitudMath solicitud,
                             StreamObserver<RespuestaMath> observadorRespuesta) {

            double numero1   = solicitud.getNumero1();
            double numero2   = solicitud.getNumero2();
            double resultado = numero1 - numero2;

            System.out.println("  [RESTA] Calculando: " + numero1 + " - " + numero2 + " = " + resultado);

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
