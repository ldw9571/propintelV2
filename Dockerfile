# 백엔드 이미지 (Gradle 8 + JDK 17). 레포의 gradle wrapper(9.x) 대신 이미지의 Gradle 8 을 사용한다.
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
