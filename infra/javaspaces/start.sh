#!/bin/bash
set -e

# Serve outrigger-dl.jar via HTTP so RMI clients can download proxy stubs.
python3 -m http.server 8080 --directory /river &

sleep 1

echo "[JAVASPACES] HTTP codebase server iniciado na porta 8080."

exec java \
  -Djava.security.manager \
  -Djava.security.policy=security.policy \
  -Djava.rmi.server.hostname=javaspaces \
  -cp start.jar:jsk-platform.jar:jsk-lib.jar:outrigger.jar:outrigger-dl.jar \
  com.sun.jini.start.ServiceStarter \
  outrigger.config
