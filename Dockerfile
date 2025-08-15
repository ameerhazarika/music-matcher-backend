FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Maven wrapper & config first for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Ensure mvnw is executable
RUN chmod +x mvnw

# Download dependencies (speeds up future builds)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the JAR
RUN ./mvnw clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
