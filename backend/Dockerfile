# Build stage: compile and package. Tests are not run here: they need Docker (Testcontainers);
# run them before building the image with ./mvnw test.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
# Dependencies first, so they stay cached while only the sources change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/mercier-*.jar app.jar

# Runtime stage: JRE only, as an unprivileged user.
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S mercier && adduser -S -G mercier mercier
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER mercier
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
