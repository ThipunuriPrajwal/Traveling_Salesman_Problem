#!/bin/bash
# ============================================================
#  Build & Run — Smart Delivery Route Optimizer (Linux/macOS)
#  Prerequisites:
#    - JDK 17+ in PATH
#    - mysql-connector-j-8.x.x.jar placed in lib/
#    - MySQL running with tsp_db created (run setup_database.sql)
# ============================================================

LIB="lib/mysql-connector-j-26.10.0.jar"
SRC="src/tsp/*.java"
OUT="out"

mkdir -p "$OUT"

echo "[1/2] Compiling..."
javac -cp "$LIB" -d "$OUT" $SRC
if [ $? -ne 0 ]; then
    echo "Compilation FAILED."
    exit 1
fi

echo "[2/2] Running..."
java -cp "$OUT:$LIB" tsp.TSPMainGUI
