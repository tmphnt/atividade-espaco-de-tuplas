FROM eclipse-temurin:8-jdk AS build

WORKDIR /app

RUN apt-get update -qq && apt-get install -y -qq wget && rm -rf /var/lib/apt/lists/*

ARG RIVER_VERSION=2.2.3
ARG MAVEN=https://repo1.maven.org/maven2

RUN wget -q ${MAVEN}/net/jini/jsk-platform/${RIVER_VERSION}/jsk-platform-${RIVER_VERSION}.jar -O jsk-platform.jar && \
    wget -q ${MAVEN}/net/jini/jsk-lib/${RIVER_VERSION}/jsk-lib-${RIVER_VERSION}.jar -O jsk-lib.jar && \
    wget -q ${MAVEN}/org/apache/river/outrigger-dl/${RIVER_VERSION}/outrigger-dl-${RIVER_VERSION}.jar -O outrigger-dl.jar && \
    wget -q ${MAVEN}/org/apache/river/reggie-dl/${RIVER_VERSION}/reggie-dl-${RIVER_VERSION}.jar -O reggie-dl.jar

COPY src/TaskEntry.java  src/TaskEntry.java
COPY src/Consumidor.java src/Consumidor.java

RUN mkdir -p classes && \
    javac -cp jsk-platform.jar:jsk-lib.jar:outrigger-dl.jar:reggie-dl.jar \
          -d classes \
          src/TaskEntry.java \
          src/Consumidor.java

FROM eclipse-temurin:8-jre

WORKDIR /app

RUN apt-get update -qq && apt-get install -y -qq python3 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/jsk-platform.jar   .
COPY --from=build /app/jsk-lib.jar        .
COPY --from=build /app/outrigger-dl.jar   .
COPY --from=build /app/reggie-dl.jar      .
COPY --from=build /app/classes            classes/
COPY security.policy .
COPY config.properties .
COPY start.sh .
RUN chmod +x start.sh

CMD ["/bin/bash", "start.sh"]
