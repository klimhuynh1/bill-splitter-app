# Use an appropriate base image, for example, OpenJDK
FROM openjdk:21

# Set the working directory
WORKDIR /app

# Copy your JAR file into the container
COPY target/bill-splitter-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app

# Specify the command to run your application
CMD ["java", "-jar", "bill-splitter-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar"]