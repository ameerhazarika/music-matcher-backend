FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy Maven wrapper & pom first to leverage caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Give execute permission to mvnw
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the app
RUN ./mvnw clean package -DskipTests

# Run the jar
ENTRYPOINT ["java", "-jar", "target/*.jar"]
