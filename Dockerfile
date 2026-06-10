FROM registry.access.redhat.com/ubi9/openjdk-21-runtime

ENV PORT=8080
ENV TZ=Africa/Nairobi

COPY target/*.jar /opt/application.jar

WORKDIR /opt

EXPOSE 8080

ENTRYPOINT ["java","-jar","/opt/application.jar"]