Prerequisites
Java Development Kit (JDK) 21 installed.

Apache Maven 3.9+ build tool.

Active MySQL server listening on port 3306 with credentials matching the configuration block above (or overridden via environment variables).


Running Locally
Clone the project and navigate to the root directory.

Build the executable deployment package:
mvn clean package -DskipTests

Boot the application engine:
java -jar target/country-integration-service-0.0.1-SNAPSHOT.jar

Confirm health status by polling the local metrics system:
curl -X GET http://localhost:8080/actuator/health
