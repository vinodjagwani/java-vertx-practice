# Dockerfile for Airline Booking System
# Candidate: Complete this Dockerfile to containerize the Vert.x application

FROM maven:3.9.6-eclipse-temurin-21

WORKDIR /app

COPY . .

RUN mvn compile -DskipTests

EXPOSE 8080

# Run with same command you use locally
ENTRYPOINT ["mvn", "compile", "exec:java", "-Dprofile=dev"]

