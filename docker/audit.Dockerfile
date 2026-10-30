FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -B -pl audit-hub -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/audit-hub/target/audit-hub-1.0.0.jar app.jar
USER 1000
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
