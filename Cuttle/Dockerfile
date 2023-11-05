# Use a Maven image for building the application
FROM maven:3.9.1 AS build

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . .

# Build the application (skip tests)
RUN mvn clean package -DskipTests

# Use Amazon Corretto as the base image
FROM openjdk:17-alpine

# Expose the port that the Spring Boot application will run on
EXPOSE 8080

# Set the JAR file argument
ARG JAR_FILE=target/Cuttle-0.0.1-SNAPSHOT.jar

# Copy the JAR file from the Maven build stage to the final image
COPY --from=build /app/${JAR_FILE} Cuttle-0.0.1-SNAPSHOT.jar

# Set the entry point for the application
ENTRYPOINT ["java", "-Dspring.profiles.active=container", "-jar", "Cuttle-0.0.1-SNAPSHOT.jar"]
