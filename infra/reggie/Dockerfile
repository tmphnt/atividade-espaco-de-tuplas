FROM eclipse-temurin:8-jdk

WORKDIR /river

RUN apt-get update -qq && apt-get install -y -qq wget ca-certificates python3 && rm -rf /var/lib/apt/lists/*

ARG RIVER_VERSION=2.2.3
ARG MAVEN=https://repo1.maven.org/maven2

RUN wget -q ${MAVEN}/org/apache/river/reggie/${RIVER_VERSION}/reggie-${RIVER_VERSION}.jar -O reggie.jar && \
    wget -q ${MAVEN}/org/apache/river/reggie-dl/${RIVER_VERSION}/reggie-dl-${RIVER_VERSION}.jar -O reggie-dl.jar && \
    wget -q ${MAVEN}/net/jini/jsk-platform/${RIVER_VERSION}/jsk-platform-${RIVER_VERSION}.jar -O jsk-platform.jar && \
    wget -q ${MAVEN}/net/jini/jsk-lib/${RIVER_VERSION}/jsk-lib-${RIVER_VERSION}.jar -O jsk-lib.jar && \
    wget -q ${MAVEN}/org/apache/river/start/${RIVER_VERSION}/start-${RIVER_VERSION}.jar -O start.jar

COPY reggie.config .
COPY reggie-inner.config .
COPY security.policy .
COPY start.sh .
RUN chmod +x start.sh

# Port 8080: HTTP server serving reggie-dl.jar to clients for RMI stub download
# Port 4160: default Jini discovery/reggie port
EXPOSE 8080 4160

CMD ["/bin/bash", "start.sh"]
