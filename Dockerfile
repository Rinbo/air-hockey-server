## Stage 1: Build JAR
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:resolve -DskipTests
COPY src src
RUN ./mvnw package -DskipTests

## Stage 2: Minimal runtime with ZGC
FROM eclipse-temurin:25-jre
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/*.jar /app/app.jar
USER app
ENTRYPOINT ["java", "-XX:+UseZGC", "-Xms512m", "-Xmx512m", "-jar", "/app/app.jar"]
