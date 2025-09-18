FROM gradle:8.13-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon || true

COPY src/ src/
COPY gradlew .
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon --warning-mode=none

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]
