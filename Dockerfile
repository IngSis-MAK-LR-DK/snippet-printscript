# syntax=docker/dockerfile:1

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
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
