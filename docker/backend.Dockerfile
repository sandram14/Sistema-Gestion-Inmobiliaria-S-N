# =====================================================================
# Backend: API Java (Servlets + JPA) sobre Tomcat 10.1 con Java 21
# =====================================================================
# Etapa 1 compila el WAR con Maven; etapa 2 lo publica en Tomcat.
# Las pruebas se ejecutan antes, fuera de la imagen (mvn -f backend/pom.xml test),
# porque las de integración necesitan la base de datos.
# Construir (desde la raíz del repositorio): docker compose -f docker/docker-compose.yml build backend
# =====================================================================

# ---- Etapa 1: compilación ----
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY backend/pom.xml ./
# Descarga de dependencias en una capa propia: se reutiliza mientras pom.xml no cambie
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline || true
COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---- Etapa 2: ejecución ----
FROM tomcat:10.1-jdk21-temurin

# curl: solo para el control de salud del contenedor (/api/health).
# No se fija su versión: la disponible cambia con cada actualización de la imagen base.
# hadolint ignore=DL3008
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 # Sin las aplicaciones de ejemplo de Tomcat: solo la nuestra, en la raíz
 && rm -rf /usr/local/tomcat/webapps/* \
 && mkdir -p /data/uploads

COPY --from=compilacion /app/target/sn-inmobiliaria.war /usr/local/tomcat/webapps/ROOT.war

ENV TZ=America/Asuncion \
    APP_ENV=produccion \
    UPLOAD_DIR=/data/uploads

EXPOSE 8080
# El arranque (catalina.sh run) lo define la imagen oficial de Tomcat
