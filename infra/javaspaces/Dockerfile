FROM eclipse-temurin:8-jdk

WORKDIR /river

RUN apt-get update -qq && apt-get install -y -qq wget ca-certificates python3 && rm -rf /var/lib/apt/lists/*

ARG RIVER_VERSION=2.2.3
ARG MAVEN=https://repo1.maven.org/maven2

RUN wget -q ${MAVEN}/org/apache/river/outrigger/${RIVER_VERSION}/outrigger-${RIVER_VERSION}.jar -O outrigger.jar && \
    wget -q ${MAVEN}/org/apache/river/outrigger-dl/${RIVER_VERSION}/outrigger-dl-${RIVER_VERSION}.jar -O outrigger-dl.jar && \
    wget -q ${MAVEN}/net/jini/jsk-platform/${RIVER_VERSION}/jsk-platform-${RIVER_VERSION}.jar -O jsk-platform.jar && \
    wget -q ${MAVEN}/net/jini/jsk-lib/${RIVER_VERSION}/jsk-lib-${RIVER_VERSION}.jar -O jsk-lib.jar && \
    wget -q ${MAVEN}/org/apache/river/start/${RIVER_VERSION}/start-${RIVER_VERSION}.jar -O start.jar

COPY outrigger.config .
COPY outrigger-inner.config .
COPY security.policy .
COPY start.sh .
RUN chmod +x start.sh

EXPOSE 8080

CMD ["/bin/bash", "start.sh"]
