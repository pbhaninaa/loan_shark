# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom and download dependencies (cached unless pom changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

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

# Use PORT so Cloud Run and other hosts can override
CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
