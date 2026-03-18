# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom and download dependencies (cached unless pom changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B
# RUN mvn clean package -DskipTests
# Copy source and build
COPY src src
RUN mvn package -B -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Cloud Run sets PORT; default 8080 for local
ENV PORT=8080

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Limit heap so the container stays within Railway memory (e.g. 512MB). Override with JAVA_OPTS in Railway if needed.
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:MaxMetaspaceSize=96m"
# Use PORT so Cloud Run and other hosts can override
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
