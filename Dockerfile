# Сборка через официальный образ Maven (не зависим от Unix-скрипта mvnw в репо).
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/auth-service-*.jar /app/app.jar
USER app
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
