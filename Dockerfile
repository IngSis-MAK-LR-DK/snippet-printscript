# syntax=docker/dockerfile:1

# --- Agente Java de New Relic ---
# Version fija y verificada por checksum (sha256 del zip publicado en Maven Central): si cambia el
# archivo, el build falla. Para actualizar, cambiar las dos cosas juntas.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS newrelic
ADD --checksum=sha256:5f32e34216b5f23b41973d098e61ae49ee4a05dd79394c0235b26e9203b57317 \
    https://repo1.maven.org/maven2/com/newrelic/agent/java/newrelic-java/9.5.0/newrelic-java-9.5.0.zip \
    /tmp/newrelic.zip
RUN cd /opt && jar xf /tmp/newrelic.zip newrelic/newrelic.jar newrelic/newrelic.yml

# --- Etapa 1: compilar el jar con Gradle y JDK 21 ---
# Siempre corre en la arquitectura de quien construye (BUILDPLATFORM): el jar es el mismo para
# amd64 y arm64, asi no hace falta emular nada al publicar una imagen multi-arquitectura.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
# El plugin de ci-kit se baja de GitHub Packages, que pide login. settings.gradle.kts lee
# GITHUB_ACTOR y GITHUB_TOKEN; entran como secretos de build y no quedan en la imagen.
RUN --mount=type=secret,id=gpr_user,env=GITHUB_ACTOR \
    --mount=type=secret,id=gpr_key,env=GITHUB_TOKEN \
    ./gradlew :app:bootJar -x check --no-daemon

# --- Etapa 2: imagen final, solo con el JRE y el jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/build/libs/app.jar app.jar
COPY --from=newrelic /opt/newrelic/newrelic.jar /opt/newrelic/newrelic.yml /app/newrelic/
# Apagado por defecto: sin cuenta de New Relic la imagen corre igual que antes. El stack lo prende
# con NEW_RELIC_AGENT_ENABLED=true y le da la license key y el nombre por ambiente.
# El log del agente va a la consola (docker logs) y no a un archivo dentro del container; el MDC
# (requestId, userId) viaja con cada log como atributo.
ENV NEW_RELIC_AGENT_ENABLED=false \
    NEW_RELIC_APP_NAME=snippet-printscript \
    NEW_RELIC_LOG_FILE_NAME=STDOUT \
    NEW_RELIC_APPLICATION_LOGGING_FORWARDING_CONTEXT_DATA_ENABLED=true
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]
