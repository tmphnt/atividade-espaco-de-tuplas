#!/bin/bash
set -e

python3 -m http.server 8080 --directory /app &

sleep 1

exec java \
  -Djava.security.manager \
  -Djava.security.policy=security.policy \
  -Djava.rmi.server.hostname=consumidor \
  -Djava.rmi.server.codebase="http://consumidor:8080/classes/" \
  -cp classes:jsk-platform.jar:jsk-lib.jar:outrigger-dl.jar:reggie-dl.jar \
  Consumidor
