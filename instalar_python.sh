#!/bin/bash
# =============================================================
# instalar_python.sh — Genera los stubs gRPC de Python
# Debe ejecutarse UNA SOLA VEZ antes de usar servidor_division.py
# Compatible con Linux, Mac y WSL.
# =============================================================

echo "══════════════════════════════════════════════════"
echo "  Instalando dependencias Python para gRPC"
echo "══════════════════════════════════════════════════"

# ── 1. Instalar librerías gRPC para Python ────────────────────
echo ""
echo "→ Paso 1: Instalando grpcio y grpcio-tools..."
pip3 install grpcio grpcio-tools

# ── 2. Generar los stubs Python desde el .proto ───────────────
# Esto crea calculadora_pb2.py y calculadora_pb2_grpc.py
echo ""
echo "→ Paso 2: Generando código Python desde calculadora.proto..."

python3 -m grpc_tools.protoc \
  -I java-broker/src/main/proto \
  --python_out=python-division \
  --grpc_python_out=python-division \
  java-broker/src/main/proto/calculadora.proto

echo ""
echo "✓ Archivos generados en python-division/:"
ls -la python-division/calculadora_pb2*.py

echo ""
echo "══════════════════════════════════════════════════"
echo "  Instalación completa. Ya puede ejecutar:"
echo "    python3 python-division/servidor_division.py"
echo "══════════════════════════════════════════════════"
