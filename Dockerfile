FROM maven:3.9.6-eclipse-temurin-21 AS build

ENV PORT 8080

ENV TZ=Africa/Nairobi

COPY target/*.jar /opt/application.jar

WORKDIR /opt

ENTRYPOINT ["java","-jar","application.jar"]