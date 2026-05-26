#!/bin/bash
# =============================================================
# ejecutar.sh — Script de inicio para Linux / Mac / WSL
# Levanta todos los microservicios en terminales separadas.
# =============================================================

# ── Colores para la consola ───────────────────────────────────
VERDE='\033[0;32m'
AMARILLO='\033[1;33m'
ROJO='\033[0;31m'
CYAN='\033[0;36m'
RESET='\033[0m'

JAR="java-broker/target/grpc-calculadora.jar"
PYTHON_SERVIDOR="python-division/servidor_division.py"

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════════╗"
echo "║     CALCULADORA DISTRIBUIDA gRPC — Inicio del Sistema   ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo -e "${RESET}"

# ── Verificar que el JAR existe ───────────────────────────────
if [ ! -f "$JAR" ]; then
  echo -e "${ROJO}✗ JAR no encontrado. Ejecute primero:${RESET}"
  echo "    cd java-broker && mvn package -q"
  exit 1
fi

# ── Verificar que Python y grpc estén disponibles ─────────────
if ! python3 -c "import grpc" 2>/dev/null; then
  echo -e "${AMARILLO}⚠ Instalando dependencias Python...${RESET}"
  pip install grpcio grpcio-tools --break-system-packages -q
fi

echo -e "${VERDE}Levantando microservicios Java en background...${RESET}"

# ── Microservicios Java (en background) ───────────────────────
java -jar $JAR calculadora.ServidorSuma          > logs/suma.log 2>&1 &
echo -e "  ${VERDE}✓ ServidorSuma        iniciado (puerto 5001) — PID $!${RESET}"

java -jar $JAR calculadora.ServidorResta         > logs/resta.log 2>&1 &
echo -e "  ${VERDE}✓ ServidorResta       iniciado (puerto 5002) — PID $!${RESET}"

java -jar $JAR calculadora.ServidorMultiplica    > logs/multiplica.log 2>&1 &
echo -e "  ${VERDE}✓ ServidorMultiplica  iniciado (puerto 5003) — PID $!${RESET}"

sleep 1

# ── Microservicio Python División ─────────────────────────────
echo -e "${VERDE}Levantando microservicio Python...${RESET}"
python3 $PYTHON_SERVIDOR > logs/division.log 2>&1 &
echo -e "  ${VERDE}✓ servidor_division.py iniciado (puerto 5004) — PID $!${RESET}"

sleep 1

# ── Broker Central ────────────────────────────────────────────
echo -e "${VERDE}Levantando Broker Central...${RESET}"
java -jar $JAR calculadora.BrokerServer > logs/broker.log 2>&1 &
echo -e "  ${VERDE}✓ BrokerServer        iniciado (puerto 5000) — PID $!${RESET}"

sleep 2

# ── Lanzar el Cliente ─────────────────────────────────────────
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${RESET}"
echo -e "${CYAN}  Sistema listo. Iniciando cliente interactivo...${RESET}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${RESET}"
echo ""

java -jar $JAR calculadora.ClienteCalculadora
