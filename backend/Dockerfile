# Build stage: compile and package on Amazon Corretto (the same JDK used in development).
# Tests are not run here: they need Docker (Testcontainers); run them before building the image with ./mvnw test.
FROM amazoncorretto:25-alpine AS build
WORKDIR /src
# Dependencies first, so they stay cached while only the sources change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/mercier-*.jar app.jar

# Corretto only publishes full-JDK images: build a runtime-only Java with jlink instead. It keeps every module
# (nothing can be missing at runtime) but drops debug symbols, man pages and headers. binutils provides the
# objcopy tool jlink uses to strip debug symbols.
RUN apk add --no-cache binutils \
    && jlink --module-path "$JAVA_HOME/jmods" --add-modules ALL-MODULE-PATH \
        --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /jre

# Runtime stage: plain Alpine with that runtime, as an unprivileged user.
FROM alpine:3.22
ENV JAVA_HOME=/opt/java
ENV PATH="$JAVA_HOME/bin:$PATH"
COPY --from=build /jre $JAVA_HOME
RUN addgroup -S mercier && adduser -S -G mercier mercier
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER mercier
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
