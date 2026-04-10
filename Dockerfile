FROM gradle:8.13-jdk17 AS build
WORKDIR /app

COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY eventitta-api/build.gradle eventitta-api/build.gradle
COPY eventitta-app/build.gradle eventitta-app/build.gradle
COPY eventitta-domain/build.gradle eventitta-domain/build.gradle
COPY eventitta-infra/build.gradle eventitta-infra/build.gradle

RUN ./gradlew :eventitta-app:dependencies --no-daemon || true

COPY eventitta-api/src/ eventitta-api/src/
COPY eventitta-app/src/ eventitta-app/src/
COPY eventitta-domain/src/ eventitta-domain/src/
COPY eventitta-infra/src/ eventitta-infra/src/

RUN ./gradlew :eventitta-app:bootJar --no-daemon --warning-mode=none

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/eventitta-app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]
