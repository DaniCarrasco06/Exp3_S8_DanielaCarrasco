# Imagen de auth-server (Semana 8 - Exp3). Contexto de build: la RAIZ del proyecto.
#   docker build -f auth-server/Dockerfile -t bank/auth-server .
# ---------- Etapa 1: compilar (solo este modulo y sus dependencias internas) ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -q -pl auth-server -am package -DskipTests

# ---------- Etapa 2: imagen final liviana (solo JRE) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /build/auth-server/target/auth-server.jar app.jar
USER appuser
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
