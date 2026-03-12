# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/leader-follower-kv-1.0.0.jar app.jar

ENV ROLE=follower
ENV FOLLOWER_URLS=""
ENV WRITE_QUORUM_SIZE=5
ENV READ_QUORUM_SIZE=1
ENV SERVER_PORT=8080

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]