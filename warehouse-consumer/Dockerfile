# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17.0.1-jdk-slim
RUN groupadd -g 1000 app && useradd -u 1000 -g app -s /bin/sh -M app
WORKDIR /app
COPY --from=builder /app/target/warehouse-consumer-1.0-SNAPSHOT.jar app.jar
RUN chown -R app:app /app
USER app

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
