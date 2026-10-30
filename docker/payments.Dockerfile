FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -B -pl payments-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/payments-service/target/payments-service-1.0.0.jar app.jar
USER 1000
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
