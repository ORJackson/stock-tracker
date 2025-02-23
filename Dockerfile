# Use a Java 21 image
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy project files
COPY . .

# Set Java environment
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Make Gradle executable
RUN chmod +x ./gradlew

# Build the project
RUN ./gradlew clean build -x check -x test

# Run the app
CMD ["java", "-jar", "build/libs/*.jar"]
