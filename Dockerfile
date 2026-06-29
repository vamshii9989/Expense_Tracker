# ---------- Stage 1: Build the application ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first so Docker can cache the downloaded dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Stage 2: Lightweight runtime image ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the built jar from the build stage (keeps final image small)
COPY --from=build /app/target/expense-tracker.jar app.jar

# Default Mongo connection settings — override these at "docker run" time
ENV MONGO_URI=mongodb://mongo:27017
ENV MONGO_DB_NAME=expense_tracker_db

ENTRYPOINT ["java", "-jar", "app.jar"]
