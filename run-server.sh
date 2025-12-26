#!/bin/bash

# Port defined in GameConstants
PORT=1099

echo "--- Checking if Port $PORT is in use ---"
# Find PID of process using the port
PID=$(lsof -t -i:$PORT)

if [ -n "$PID" ]; then
    echo "Killing existing server process (PID: $PID)..."
    kill -9 $PID
else
    echo "Port $PORT is free."
fi

echo "--- Rebuilding Project (Maven Install) ---"
# -DskipTests speeds up the build since we just want to run it
# set -e ensures we stop if the build fails
set -e
mvn clean install -DskipTests

echo "--- Starting Caro Server ---"
mvn exec:java -pl caro-server -Dexec.mainClass="com.caro.server.app.ServerApp"
