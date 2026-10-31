FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /work
COPY --from=build --chown=10001:0 /workspace/target/quarkus-app/ ./
USER 10001
EXPOSE 8082
ENV KAVIYO_HTTP_PORT=8082
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
