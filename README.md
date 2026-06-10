# Country Information Integration Service

## Overview

This project demonstrates integration between REST and SOAP services using Spring Boot.
The application receives a country name via a REST API,
retrieves the ISO country code from a SOAP service, fetches detailed country information,
stores the information in MySQL,
and exposes CRUD operations for managing the stored data.

The solution follows enterprise integration patterns, production-ready deployment practices,
and cloud-native principles suitable for Kubernetes environments.

---

# Architecture

## High-Level Flow

```text
Client
  |
  v
REST API
  |
  v
Country Service
  |
  +----------------------+
  | SOAP Integration     |
  +----------------------+
  |
  v
CountryInfoService SOAP
  |
  +--> CountryISOCode
  |
  +--> FullCountryInfo
  |
  v
MySQL Database
  |
  v
CRUD APIs
```

### Components

| Component        | Responsibility                          |
|------------------|-----------------------------------------|
| REST Controller  | Receives requests and returns responses |
| Service Layer    | Business logic and orchestration        |
| SOAP Client      | Calls CountryInfo SOAP services         |
| Repository Layer | Database interactions                   |
| MySQL            | Persistent storage                      |
| Kubernetes       | Container orchestration                 |
| Prometheus       | Metrics collection                      |

---

# Technology Stack

* Java 21
* Spring Boot 3.x
* Spring Web
* Spring Data JPA
* MySQL 8
* SOAP Web Services
* Maven
* Docker
* Kubernetes
* Micrometer
* Resilience4j

---

# SOAP Service

WSDL:

http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL

SOAP Operations Used:

### CountryISOCode

Input:

```xml

<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
    <soapenv:Header/>
    <soapenv:Body>
        <web:CountryISOCode>
            <web:sCountryName>Kenya</web:sCountryName>
        </web:CountryISOCode>
    </soapenv:Body>
</soapenv:Envelope>
```

Response:

```xml

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <m:CountryISOCodeResponse xmlns:m="http://www.oorsprong.org/websamples.countryinfo">
            <m:CountryISOCodeResult>KE</m:CountryISOCodeResult>
        </m:CountryISOCodeResponse>
    </soap:Body>
</soap:Envelope>
```

### FullCountryInfo

Input:

```xml

<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
    <soapenv:Header/>
    <soapenv:Body>
        <web:FullCountryInfo>
            <web:sCountryISOCode>KE</web:sCountryISOCode>
        </web:FullCountryInfo>
    </soapenv:Body>
</soapenv:Envelope>
```

Response:

```xml

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <m:FullCountryInfoResponse xmlns:m="http://www.oorsprong.org/websamples.countryinfo">
            <m:FullCountryInfoResult>
                <m:sISOCode>KE</m:sISOCode>
                <m:sName>Kenya</m:sName>
                <m:sCapitalCity>Nairobi</m:sCapitalCity>
                <m:sPhoneCode>254</m:sPhoneCode>
                <m:sContinentCode>AF</m:sContinentCode>
                <m:sCurrencyISOCode>KES</m:sCurrencyISOCode>
                <m:sCountryFlag>http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg</m:sCountryFlag>
                <m:Languages>
                    <m:tLanguage>
                        <m:sISOCode>swa</m:sISOCode>
                        <m:sName>Swahili</m:sName>
                    </m:tLanguage>
                </m:Languages>
            </m:FullCountryInfoResult>
        </m:FullCountryInfoResponse>
    </soap:Body>
</soap:Envelope>
```

---

# Database Design

## CountryInfo

| Column          | Type    |
|-----------------|---------|
| id              | BIGINT  |
| isoCode         | VARCHAR |
| countryName     | VARCHAR |
| capitalCity     | VARCHAR |
| phoneCode       | VARCHAR |
| continentCode   | VARCHAR |
| currencyISOCode | VARCHAR |
| countryFlag     | VARCHAR |

## Language

| Column       | Type    |
|--------------|---------|
| id           | BIGINT  |
| languageName | VARCHAR |
| iso639_1     | VARCHAR |
| iso639_2     | VARCHAR |
| country_id   | BIGINT  |

Relationship:

```text
CountryInfo
    |
    | 1:N
    |
Language
```

---

# REST APIs

## Create Country

POST

```http
/api/v1/countries
```

Request

```json
{
  "name": "Kenya"
}
```

Response

```json
{
  "id": 2,
  "isoCode": "KE",
  "name": "Kenya",
  "capitalCity": null,
  "phoneCode": null,
  "continentCode": null,
  "languages": [
    {
      "id": 2,
      "isoCode": "swa",
      "name": "Swahili"
    }
  ]
}
```

---

## Get All Countries

GET

```http
/api/v1/countries
```

---

## Get Country By ID

GET

```http
/api/v1/countries/{id}
```

---

## Update Country

PUT

```http
/api/v1/countries/{id}
```

---

## Delete Country

DELETE

```http
/api/v1/countries/{id}
```

---

# Running Locally

## Start MySQL

```bash
docker run -d \
--name mysql \
-e MYSQL_ROOT_PASSWORD=root \
-e MYSQL_DATABASE=countrydb \
-p 3306:3306 \
mysql:8
```

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/country-integration-service-0.0.1-SNAPSHOT.jar
```

Application URL:

```text
http://localhost:8080
```

---

# Error Handling

The application implements centralized exception handling using:

```java
@ControllerAdvice
```

Standard Response Format:

```json
{
  "timestamp": "2026-08-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Country not found",
  "path": "/api/v1/countries/100"
}
```

HTTP Status Codes:

| Code | Meaning               |
|------|-----------------------|
| 200  | Success               |
| 201  | Created               |
| 400  | Bad Request           |
| 404  | Not Found             |
| 409  | Conflict              |
| 500  | Internal Server Error |
| 503  | Service Unavailable   |

---

# Resilience & Fault Tolerance

## Timeouts

SOAP calls have connection and read timeouts configured.

## Retries

Resilience4j Retry automatically retries transient failures.

```java
@Retry(name = "countrySoap")
```

## Circuit Breaker

Prevents cascading failures when SOAP service is unavailable.

```java
@CircuitBreaker(name = "countrySoap")
```

## Fallbacks

Graceful fallback responses returned when SOAP endpoint is down.

---

# Logging

Structured JSON logging is implemented.

Example:

```json
{
  "timestamp": "2026-08-01T12:00:00",
  "level": "INFO",
  "traceId": "abc123",
  "message": "Country information successfully retrieved"
}
```

Logs include:

* Request IDs
* Correlation IDs
* Response Times
* SOAP Request/Response Tracking

---

# Monitoring

Spring Boot Actuator exposes:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

Metrics collected:

* Request count
* Response time
* Error rate
* Database connection pool usage
* JVM metrics
* SOAP integration metrics

---

# Containerization

## Dockerfile

```dockerfile
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime

ENV PORT=8080
ENV TZ=Africa/Nairobi

COPY target/*.jar /opt/application.jar

WORKDIR /opt

EXPOSE 8080

ENTRYPOINT ["java","-jar","/opt/application.jar"]
```

## Deployment

```bash
kubectl apply -f k8s/deployment.yaml
```

Verify:

```bash
kubectl get pods
```

---

# Scaling Strategy

The service is stateless and designed for horizontal scaling.

Example:

```bash
kubectl scale deployment country-service \
--replicas=5
```

Benefits:

* Increased throughput
* Improved availability
* Better fault tolerance

---

# Health Checks

Readiness Probe

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

Liveness Probe

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
```

---

# Kubernetes Troubleshooting Guide

## Check Pods

```bash
kubectl get pods
```

## Describe Pod

```bash
kubectl describe pod country-service
```

## View Logs

```bash
kubectl logs country-service
```

Follow Logs:

```bash
kubectl logs -f country-service
```

## Check Deployment

```bash
kubectl get deployment
kubectl describe deployment country-service
```

## Verify Service

```bash
kubectl get svc
```

## Verify Endpoints

```bash
kubectl get endpoints
```

## Execute Inside Pod

```bash
kubectl exec -it country-service -- sh
```

## Restart Deployment

```bash
kubectl rollout restart deployment country-service
```

## Rollback Deployment

```bash
kubectl rollout undo deployment country-service
```

---

# Production Readiness Considerations

### Security

* Secrets stored in Kubernetes Secrets
* Non-root containers
* Minimal UBI 9 runtime image
* HTTPS termination via ingress

### Scalability

* Stateless services
* Horizontal Pod Autoscaling
* Load-balanced services

### Reliability

* Circuit breakers
* Retries
* Health checks
* Graceful shutdown

### Observability

* Structured logging
* Metrics
* Health endpoints

---

# Testing

## Create Country

```bash
curl --location 'http://localhost:8080/api/v1/countries' \
--header 'Content-Type: application/json' \
--data '{
"name":"kenya"
}'
```

## Fetch All Countries

```bash
curl http://localhost:8080/api/v1/countries
```

## Fetch Country By ID

```bash
curl http://localhost:8080/api/v1/countries/1
```

---

# GitHub Repository

```text
https://github.com/polycarpmogaka25/ncba-loop.git
```
