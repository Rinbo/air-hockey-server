## Stage 1: Build native image
FROM container-registry.oracle.com/graalvm/native-image:25 AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:resolve -DskipTests
COPY src src
RUN ./mvnw -Pnative native:compile -DskipTests

## Stage 2: Minimal runtime
FROM debian:bookworm-slim
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/air-hockey-server /app/air-hockey-server
USER app
ENTRYPOINT ["/app/air-hockey-server"]
