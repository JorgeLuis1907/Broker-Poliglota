"""
╔══════════════════════════════════════════════════════════════════════╗
║  servidor_division.py — Microservicio de DIVISION en Python         ║
║                                                                      ║
║  LENGUAJE: Python (políglota: diferente al resto del sistema)        ║
║  PUERTO  : 5004                                                      ║
║                                                                      ║
║  Lee el MISMO contrato calculadora.proto que usa Java,              ║
║  comprobando que gRPC es agnóstico al lenguaje.                      ║
╚══════════════════════════════════════════════════════════════════════╝
"""

import grpc
from concurrent import futures
import time

# Importamos el código generado por protoc (generado con grpc_tools)
# Estos archivos se generan ejecutando el comando en el README
import calculadora_pb2
import calculadora_pb2_grpc

# ── Configuración del microservicio ──────────────────────────────────
PUERTO            = 5004
TRABAJADORES_MAX  = 10       # Hilos concurrentes máximos
TIEMPO_ESPERA_SEG = 86400    # 24 horas antes de timeout automático


# ════════════════════════════════════════════════════════════════════
# IMPLEMENTACIÓN DEL SERVICIO gRPC
# Hereda de la clase generada automáticamente por protoc
# ════════════════════════════════════════════════════════════════════
class ServicioDivision(calculadora_pb2_grpc.ServicioCalculadoraServicer):
    """
    Implementa el contrato definido en calculadora.proto.
    Solo maneja la operación DIVISION.
    """

    def Calcular(self, solicitud, contexto):
        """
        Método RPC llamado por el Broker de Java cuando llega
        una solicitud de división.
        """
        numero1   = solicitud.numero1
        numero2   = solicitud.numero2
        operacion = solicitud.operacion.upper().strip()

        print(f"  [DIVISION-PY] Solicitud recibida:")
        print(f"    numero1  : {numero1}")
        print(f"    numero2  : {numero2}")
        print(f"    operacion: {operacion}")

        # ── Validación de seguridad propia del microservicio ──────────
        # El Broker ya validó el caso obvio (numero2==0 con entero),
        # pero el microservicio también valida para ser autónomo.
        if numero2 == 0:
            print("  [DIVISION-PY] ⚠ División entre cero rechazada.")
            return calculadora_pb2.RespuestaMath(
                resultado    = 0.0,
                esExitoso    = False,
                mensajeError = "Error: División entre cero inválida"
            )

        # ── Cálculo de la división ─────────────────────────────────────
        resultado = numero1 / numero2
        print(f"  [DIVISION-PY] ✓ Calculando: {numero1} ÷ {numero2} = {resultado}")

        return calculadora_pb2.RespuestaMath(
            resultado    = resultado,
            esExitoso    = True,
            mensajeError = ""   # Vacío porque fue exitoso
        )


# ════════════════════════════════════════════════════════════════════
# ARRANQUE DEL SERVIDOR gRPC
# ════════════════════════════════════════════════════════════════════
def iniciar_servidor():
    """
    Crea el servidor gRPC, registra el servicio y comienza a escuchar.
    """
    # Creamos el servidor con un pool de hilos
    servidor = grpc.server(
        futures.ThreadPoolExecutor(max_workers=TRABAJADORES_MAX)
    )

    # Registramos nuestra implementación en el servidor
    calculadora_pb2_grpc.add_ServicioCalculadoraServicer_to_server(
        ServicioDivision(), servidor
    )

    # Indicamos el puerto de escucha (sin TLS en desarrollo)
    servidor.add_insecure_port(f"[::]:{PUERTO}")
    servidor.start()

    print("╔══════════════════════════════════════════╗")
    print("║   MICROSERVICIO DIVISION - LISTO         ║")
    print(f"║   Puerto: {PUERTO}   Lenguaje: Python        ║")
    print("╚══════════════════════════════════════════╝")
    print("  Esperando solicitudes del Broker Java...\n")

    # ── Mantenemos el servidor activo indefinidamente ─────────────────
    try:
        servidor.wait_for_termination()
    except KeyboardInterrupt:
        print("\n  [DIVISION-PY] Señal de interrupción recibida.")
        print("  [DIVISION-PY] Apagando servidor de forma limpia...")
        servidor.stop(grace=3)   # 3 segundos para terminar peticiones en curso
        print("  [DIVISION-PY] Servidor apagado correctamente.")


# ── Punto de entrada ──────────────────────────────────────────────────
if __name__ == "__main__":
    iniciar_servidor()
