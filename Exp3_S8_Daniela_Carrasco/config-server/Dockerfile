FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -q -pl config-server -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /build/config-server/target/config-server.jar app.jar
USER appuser
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
