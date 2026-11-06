FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build --chown=10001:0 /workspace/target/quarkus-app/ /app/
USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/tmp"
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]