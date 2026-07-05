#!/bin/bash
set -e

# Serve compiled classes via HTTP so the JavaSpace can load TaskEntry
# when deserializing tuples written by this client.
python3 -m http.server 8080 --directory /app &

sleep 1

exec java \
  -Djava.security.manager \
  -Djava.security.policy=security.policy \
  -Djava.rmi.server.hostname=produtor \
  -Djava.rmi.server.codebase="http://produtor:8080/classes/" \
  -cp classes:jsk-platform.jar:jsk-lib.jar:outrigger-dl.jar:reggie-dl.jar \
  Produtor
