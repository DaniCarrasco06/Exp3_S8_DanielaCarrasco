FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -q -pl bff-mobile -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /build/bff-mobile/target/bff-mobile.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
