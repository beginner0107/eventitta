# syntax=docker/dockerfile:1
FROM gradle:8.13-jdk17 AS build
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
RUN chmod +x ./gradlew

COPY src/ src/
RUN ./gradlew clean bootJar --no-daemon --warning-mode=none

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]
