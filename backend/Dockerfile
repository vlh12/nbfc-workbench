FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend backend
ARG SERVICE
RUN mvn -B -pl backend/${SERVICE} -am package -DskipTests
RUN cp backend/${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar /app.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
USER 10001
ENTRYPOINT ["java","-jar","app.jar"]

