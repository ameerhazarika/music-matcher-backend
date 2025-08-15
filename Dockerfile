FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy Maven wrapper & pom first to leverage Docker layer caching for dependencies
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the app
RUN ./mvnw clean package -DskipTests

# Run the jar
ENTRYPOINT ["java", "-jar", "target/*.jar"]
