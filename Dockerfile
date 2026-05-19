# --- Fase 1: Descarga de dependencias y construcción ---
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copiamos solo el POM para aprovechar la caché de capas de Docker
COPY pom.xml .
# Descargamos las dependencias. Si el pom.xml no cambia, esta capa se cachea
RUN mvn dependency:go-offline -B

# Copiamos el código fuente (Arquitectura Hexagonal: domain, infrastructure, application...)
COPY src ./src
# Compilamos el proyecto omitiendo los tests para acelerar el build en desarrollo
RUN mvn clean package -DskipTests

# --- Fase 2: Imagen de ejecución (Ligera y segura) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiamos el jar generado desde la fase de build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Configuraciones de optimización para Spring en contenedores
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=dev", "app.jar"]