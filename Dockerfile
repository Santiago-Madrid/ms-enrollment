# --- Etapa 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# 1. Copiar e instalar la librería común (wd-lib-common)
COPY wd-lib-common ./wd-lib-common
RUN mvn -f wd-lib-common/pom.xml clean install -DskipTests

# 2. Copiar poms primero para aprovechar la caché de Docker en la descarga de dependencias
COPY ms-enrollment/pom.xml ./ms-enrollment/
RUN mvn -f ms-enrollment/pom.xml dependency:go-offline -B

# 3. Copiar el código fuente de ms-enrollment y empaquetar
COPY ms-enrollment/src ./ms-enrollment/src
RUN mvn -f ms-enrollment/pom.xml clean package -DskipTests

# --- Etapa 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el artifact empaquetado desde la etapa de Build
COPY --from=builder /app/ms-enrollment/target/*.jar app.jar

EXPOSE 9093
ENTRYPOINT ["java", "-jar", "app.jar"]