#!/bin/bash
set -e

# Serve reggie-dl.jar via HTTP on port 8080 so RMI clients can download proxy stubs.
# River requires this URL to be reachable before ServiceStarter will proceed.
python3 -m http.server 8080 --directory /river &
HTTP_PID=$!

# Give the HTTP server a moment to bind
sleep 1

echo "[REGGIE] HTTP codebase server iniciado na porta 8080."

exec java \
  -Djava.security.manager \
  -Djava.security.policy=security.policy \
  -Djava.rmi.server.hostname=reggie \
  -cp start.jar:jsk-platform.jar:jsk-lib.jar:reggie.jar:reggie-dl.jar \
  com.sun.jini.start.ServiceStarter \
  reggie.config
