#!/bin/bash

echo "--- Rebuilding Client Components ---"
# Rebuild common and client
set -e
mvn install -pl caro-client -am -DskipTests

echo "--- Starting Caro Client (Launcher) ---"
# Use exec:java with the new Launcher class
mvn exec:java -pl caro-client -Dexec.mainClass="com.caro.client.app.Launcher"

