# Calculadora Distribuida con gRPC
### Patrón Broker/Middleware · Java + Python · Políglota

---

## Índice
1. [Arquitectura del Sistema](#1-arquitectura-del-sistema)
2. [Estructura del Proyecto](#2-estructura-del-proyecto)
3. [Contrato gRPC (calculadora.proto)](#3-contrato-grpc)
4. [Prerrequisitos](#4-prerrequisitos)
5. [Compilación Java con Maven](#5-compilación-java-con-maven)
6. [Generación de Stubs Python](#6-generación-de-stubs-python)
7. [Ejecución Paso a Paso](#7-ejecución-paso-a-paso)
8. [Políticas de Manejo de Errores (QA)](#8-políticas-de-manejo-de-errores-qa)
9. [Referencia de Comandos por Sistema Operativo](#9-referencia-de-comandos)
10. [Flujo Completo de una Petición](#10-flujo-completo-de-una-petición)

---

## 1. Arquitectura del Sistema

```
  ┌─────────────────────────────────────────────────────────────────┐
  │                    SISTEMA DISTRIBUIDO gRPC                      │
  │                                                                   │
  │  ┌──────────────┐        ┌──────────────────────────────────┐    │
  │  │   CLIENTE    │  gRPC  │         BROKER (Puerto 5000)      │    │
  │  │   Java       │───────▶│         BrokerServer.java         │    │
  │  │  Puerto N/A  │◀───────│  Middleware / Enrutador Central   │    │
  │  └──────────────┘        └───────────┬──────────────────────┘    │
  │                                       │                            │
  │           ┌───────────────────────────┼────────────────────┐      │
  │           ▼                           ▼                     ▼      │
  │  ┌────────────────┐   ┌────────────────────┐  ┌──────────────────┐│
  │  │  ServidorSuma  │   │   ServidorResta     │  │ServidorMultiplica││
  │  │  Java · 5001   │   │   Java · 5002       │  │Java · 5003       ││
  │  └────────────────┘   └────────────────────┘  └──────────────────┘│
  │                                                                     │
  │                        ┌──────────────────────────────────────┐    │
  │                        │  servidor_division.py  ← PYTHON       │    │
  │                        │  Puerto 5004  (Microservicio políglota)│   │
  │                        └──────────────────────────────────────┘    │
  └─────────────────────────────────────────────────────────────────┘
```

**El cliente nunca conoce los puertos de los microservicios.**
Solo habla con el Broker en el puerto 5000. El Broker decide a dónde redirigir.

---

## 2. Estructura del Proyecto

```
grpc-calculadora/
│
├── README.md                        ← Este archivo
├── ejecutar.sh                      ← Inicio automático (Linux/Mac/WSL)
├── instalar_python.sh               ← Genera stubs Python (correr 1 vez)
│
├── java-broker/                     ← Proyecto Maven (Java)
│   ├── pom.xml                      ← Dependencias gRPC + plugin protoc
│   └── src/main/
│       ├── proto/
│       │   └── calculadora.proto    ← CONTRATO ÚNICO del sistema
│       └── java/calculadora/
│           ├── grpc/                ← Clases auto-generadas por Maven
│           │   ├── SolicitudMath.java
│           │   ├── RespuestaMath.java
│           │   └── ServicioCalculadoraGrpc.java
│           │
│           ├── BrokerServer.java    ← Núcleo del middleware (puerto 5000)
│           ├── ServidorSuma.java    ← Microservicio SUMA (puerto 5001)
│           ├── ServidorResta.java   ← Microservicio RESTA (puerto 5002)
│           ├── ServidorMultiplica.java ← Microservicio MULT (puerto 5003)
│           └── ClienteCalculadora.java ← Cliente de consola interactivo
│
└── python-division/
    ├── servidor_division.py         ← Microservicio DIVISION en Python
    ├── calculadora_pb2.py           ← Auto-generado: clases de mensajes
    └── calculadora_pb2_grpc.py      ← Auto-generado: stubs del servicio
```

---

## 3. Contrato gRPC

El archivo `calculadora.proto` es el **único contrato** que todos los servicios
del sistema comparten, sin importar si están en Java o Python.

```protobuf
// Petición del cliente al Broker
message SolicitudMath {
  double numero1   = 1;   // Primer operando
  double numero2   = 2;   // Segundo operando
  string operacion = 3;   // "SUMA" | "RESTA" | "MULTIPLICACION" | "DIVISION"
}

// Respuesta del Broker al cliente
message RespuestaMath {
  double resultado    = 1;  // Valor numérico del resultado
  bool   esExitoso   = 2;  // true = OK | false = hubo un error
  string mensajeError = 3;  // Vacío si exitoso, descriptivo si falló
}

service ServicioCalculadora {
  rpc Calcular (SolicitudMath) returns (RespuestaMath);
}
```

---

## 4. Prerrequisitos

| Herramienta | Versión mínima | Verificar con |
|---|---|---|
| Java JDK | 11 o superior | `java -version` |
| Maven | 3.6 o superior | `mvn -version` |
| Python | 3.8 o superior | `python3 --version` |
| pip | cualquiera | `pip3 --version` |

### Instalación rápida por sistema operativo

**Ubuntu / Debian / WSL:**
```bash
sudo apt update
sudo apt install -y default-jdk maven python3 python3-pip
```

**macOS (con Homebrew):**
```bash
brew install openjdk maven python3
```

**Windows (sin WSL):**
1. Descargar JDK desde https://adoptium.net
2. Descargar Maven desde https://maven.apache.org/download.cgi
3. Descargar Python desde https://python.org
4. Agregar los tres al PATH del sistema.

---

## 5. Compilación Java con Maven

```bash
# Desde la raíz del proyecto:
cd java-broker

# Limpia, genera stubs gRPC desde .proto, compila y empaqueta todo en un JAR
mvn clean package -q

# Resultado esperado:
# target/grpc-calculadora.jar   ← Fat JAR con todas las dependencias
```

**¿Qué hace Maven internamente?**
1. `mvn generate-sources` → El plugin `protobuf-maven-plugin` llama a `protoc`
   y genera automáticamente las clases Java desde `calculadora.proto`.
2. `mvn compile` → Compila esas clases generadas junto con nuestro código.
3. `mvn package` → El plugin `maven-shade` empaqueta TODO en un único JAR ejecutable.

---

## 6. Generación de Stubs Python

Este paso solo se necesita **una vez**. Genera los archivos Python
equivalentes a los que Maven genera para Java.

```bash
# Desde la raíz del proyecto:
chmod +x instalar_python.sh
./instalar_python.sh
```

O manualmente:
```bash
pip3 install grpcio grpcio-tools

python3 -m grpc_tools.protoc \
  -I java-broker/src/main/proto \
  --python_out=python-division \
  --grpc_python_out=python-division \
  java-broker/src/main/proto/calculadora.proto
```

Esto genera en `python-division/`:
- `calculadora_pb2.py` → Clases de los mensajes (SolicitudMath, RespuestaMath)
- `calculadora_pb2_grpc.py` → Stubs del servicio gRPC

---

## 7. Ejecución Paso a Paso

### Cada servicio necesita su propia terminal. Abrirlas en este orden:

#### Terminal 1 — Microservicio SUMA (Java)
```bash
cd java-broker
java -cp target/grpc-calculadora.jar calculadora.ServidorSuma
```
Salida esperada:
```
╔══════════════════════════════════╗
║   MICROSERVICIO SUMA - LISTO     ║
║   Puerto: 5001  Lenguaje: Java   ║
╚══════════════════════════════════╝
  Esperando solicitudes del Broker...
```

#### Terminal 2 — Microservicio RESTA (Java)
```bash
cd java-broker
java -cp target/grpc-calculadora.jar calculadora.ServidorResta
```

#### Terminal 3 — Microservicio MULTIPLICACION (Java)
```bash
cd java-broker
java -cp target/grpc-calculadora.jar calculadora.ServidorMultiplica
```

#### Terminal 4 — Microservicio DIVISION (Python)
```bash
cd python-division
python3 servidor_division.py
```
Salida esperada:
```
╔══════════════════════════════════════════╗
║   MICROSERVICIO DIVISION - LISTO         ║
║   Puerto: 5004   Lenguaje: Python        ║
╚══════════════════════════════════════════╝
  Esperando solicitudes del Broker Java...
```

#### Terminal 5 — Broker Central (Java)
```bash
cd java-broker
java -cp target/grpc-calculadora.jar calculadora.BrokerServer
```
Salida esperada:
```
╔══════════════════════════════════════════╗
║       BROKER CENTRAL - INICIADO          ║
╠══════════════════════════════════════════╣
║  Puerto    : 5000                        ║
║  Rutas     :                             ║
║    SUMA          → localhost:5001 (Java) ║
║    RESTA         → localhost:5002 (Java) ║
║    MULTIPLICACION→ localhost:5003 (Java) ║
║    DIVISION      → localhost:5004 (Python)║
╚══════════════════════════════════════════╝
  Esperando peticiones...
```

#### Terminal 6 — Cliente Interactivo (Java)
```bash
cd java-broker
java -cp target/grpc-calculadora.jar calculadora.ClienteCalculadora
```

---

## 8. Políticas de Manejo de Errores (QA)

### POLÍTICA 1: Fail-Fast (mismo lenguaje, sin viaje de red)

**Escenario:** El cliente pide `DIVISION` con `numero2 = 0`.

**Comportamiento:** El Broker valida localmente **antes** de abrir cualquier
conexión de red. Si detecta división entre cero, responde de inmediato.

**Prueba:**
```
Opción → 4  (DIVISION)
Primer número  → 10
Segundo número → 0
```

**Log del Broker:**
```
[BROKER] ⚠ FAIL-FAST: División entre cero detectada localmente.
```

**Respuesta al cliente:**
```
Estado  : ✗ ERROR
Detalle : Error: División entre cero inválida
```

**Ventaja documentada para QA:** Cero latencia de red. El error se corta
en el Broker sin desperdiciar recursos de red ni del microservicio Python.

---

### POLÍTICA 2: Tolerancia a Fallos / Caída de Nodo (lenguajes distintos)

**Escenario:** El cliente pide `DIVISION` con `numero2 != 0`, pero el
microservicio Python (`servidor_division.py`) está apagado o se cayó.

**Prueba:** No iniciar la Terminal 4 (Python) y solicitar una división.

**Log del Broker:**
```
[BROKER] → Redirigiendo a localhost:5004
[BROKER] ✗ Nodo caído - El microservicio de DIVISIÓN (Python) no se encuentra disponible
[BROKER]   Causa gRPC: UNAVAILABLE
```

**Respuesta al cliente:**
```
Estado  : ✗ ERROR
Detalle : El microservicio de DIVISIÓN (Python) no se encuentra disponible
```

**Ventaja documentada para QA:** El sistema **no colapsa**. El Broker captura
la excepción gRPC (`StatusRuntimeException`), la envuelve en una respuesta
limpia y el cliente recibe información útil en lugar de un stack trace.

---

### POLÍTICA 3: Operación Desconocida

**Escenario:** Se envía una operación que el sistema no reconoce.

**Respuesta:**
```
Error: Operación 'POTENCIA' no reconocida. Use: SUMA, RESTA, MULTIPLICACION, DIVISION
```

---

## 9. Referencia de Comandos

### Windows (PowerShell / CMD)

```powershell
# Compilar
cd java-broker
mvn clean package -q

# Ejecutar cada servicio (en ventanas separadas de PowerShell)
java -cp target\grpc-calculadora.jar calculadora.ServidorSuma
java -cp target\grpc-calculadora.jar calculadora.ServidorResta
java -cp target\grpc-calculadora.jar calculadora.ServidorMultiplica
java -cp target\grpc-calculadora.jar calculadora.BrokerServer
java -cp target\grpc-calculadora.jar calculadora.ClienteCalculadora

# Python (en otra ventana)
cd ..\python-division
python servidor_division.py
```

### Linux / macOS / WSL

```bash
# Compilar
cd java-broker && mvn clean package -q

# Ejecutar (en terminales separadas, añadir & para background)
java -cp target/grpc-calculadora.jar calculadora.ServidorSuma &
java -cp target/grpc-calculadora.jar calculadora.ServidorResta &
java -cp target/grpc-calculadora.jar calculadora.ServidorMultiplica &
python3 ../python-division/servidor_division.py &
java -cp target/grpc-calculadora.jar calculadora.BrokerServer &

# Cliente (en foreground para interactuar)
java -cp target/grpc-calculadora.jar calculadora.ClienteCalculadora
```

---

## 10. Flujo Completo de una Petición

```
Usuario ingresa: 15 ÷ 3
        │
        ▼
ClienteCalculadora.java
  Construye SolicitudMath{numero1=15, numero2=3, operacion="DIVISION"}
  Abre canal gRPC → localhost:5000
        │
        ▼
BrokerServer.java  (puerto 5000)
  Recibe la solicitud
  Valida: ¿numero2 == 0? → NO → continúa
  Decide: operacion="DIVISION" → puerto 5004
  Abre canal gRPC → localhost:5004
        │
        ▼ (si Python está activo)
servidor_division.py  (puerto 5004)
  Lee numero1=15, numero2=3
  Calcula: 15 / 3 = 5.0
  Devuelve RespuestaMath{resultado=5.0, esExitoso=true}
        │
        ▼ (regresa al Broker)
BrokerServer.java
  Recibe RespuestaMath del Python
  Reenvía la misma respuesta al cliente
        │
        ▼
ClienteCalculadora.java
  Muestra:
    Estado   : ✓ ÉXITO
    Resultado: 5.000000
```

---

## Mapa de Puertos

| Puerto | Servicio | Lenguaje | Archivo |
|--------|----------|----------|---------|
| 5000 | Broker Central | Java | BrokerServer.java |
| 5001 | Microservicio SUMA | Java | ServidorSuma.java |
| 5002 | Microservicio RESTA | Java | ServidorResta.java |
| 5003 | Microservicio MULTIPLICACION | Java | ServidorMultiplica.java |
| 5004 | Microservicio DIVISION | **Python** | servidor_division.py |
