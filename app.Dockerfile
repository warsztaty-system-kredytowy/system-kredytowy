FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]