# --- Etapa 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Forzar User-Agent y deshabilitar verificación de agentes automatizados
ENV MAVEN_OPTS="-Dhttp.agent=Mozilla/5.0 -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"

# Generar un settings.xml con mirrors alternativos (Google CDN & Aliyun/Central)
RUN echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
                      https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
  <mirrors> \
    <mirror> \
      <id>google-maven-central</id> \
      <name>Google Maven Central Mirror</name> \
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url> \
      <mirrorOf>central</mirrorOf> \
    </mirror> \
    <mirror> \
      <id>aliyun-maven</id> \
      <name>Aliyun Central Mirror</name> \
      <url>https://maven.aliyun.com/repository/central</url> \
      <mirrorOf>central</mirrorOf> \
    </mirror> \
  </mirrors> \
</settings>' > /usr/share/maven/ref/settings.xml

# 1. Compilar e instalar la librería común compartida
COPY wd-lib-common ./wd-lib-common
RUN mvn -s /usr/share/maven/ref/settings.xml -f wd-lib-common/pom.xml clean install -DskipTests

# 2. Copiar e instalar el microservicio ms-enrollment
COPY ms-enrollment ./ms-enrollment
RUN mvn -s /usr/share/maven/ref/settings.xml -f ms-enrollment/pom.xml clean package -DskipTests

# --- Etapa 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/ms-enrollment/target/*.jar app.jar

EXPOSE 9093
ENTRYPOINT ["java", "-jar", "app.jar"]