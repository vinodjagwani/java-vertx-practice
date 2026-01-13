## Project Structure

```
src/
├── main/
│   ├── java/com/airline/booking/demo
│   │   ├── Launcher.java                           # JVM entry
│   │   ├── MainVerticle.java                       # Starts HTTP server + routes
│   │   ├── bootstrap/
│   │   │   ├── AppModule.java                      # Guice bindings
│   │   │   └── DatabaseBootstrap.java              # DB init / migration
│   │   ├── config/
│   │   │   ├── ConfigProvider.java
│   │   │   ├── DbPoolProvider.java
│   │   │   └── JsonConfig.java
│   │   ├── common/
│   │   │   ├── db/
│   │   │   ├── logging/
│   │   │   ├── utils/
│   │   │   └── validation/
│   │   ├── exception/
│   │   │   ├── dto/
│   │   │   │   ├── BusinessServiceException.java
│   │   │   │   └── ErrorCodeEnum.java
│   │   │   ├── PgErrorMapper.java
│   │   │   ├── GlobalErrorHandler.java
│   │   │   └── ErrorPrinter.java
│   │   ├── feature/                                  # Feature modules
│   │   │   ├── airlines/
│   │   │   │   ├── AirlineFeature.java               # Registers handler + routes
│   │   │   │   ├── dto/                              # DTO input/output
│   │   │   │   ├── handler/                          # HTTP handlers
│   │   │   │   │   └── AirlineHandler.java
│   │   │   │   ├── mapper/                           # Map entity ↔ dto
│   │   │   │   │   └── AirlineMapper.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/                       # Domain entities
│   │   │   │   │   │   └── Airline.java
│   │   │   │   │   ├── AirlineRepository.java
│   │   │   │   │   └── impl/
│   │   │   │   │       └── AirlineRepositoryImpl.java
│   │   │   │   └── service/                          # Business logic
│   │   │   │       └── AirlineService.java
│   │   │   ├── flights/
│   │   │   │   ├── FlightFeature.java
│   │   │   │   ├── dto/
│   │   │   │   ├── handler/
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── Flight.java
│   │   │   │   │   ├── FlightRepository.java
│   │   │   │   │   └── impl/FlightRepositoryImpl.java
│   │   │   │   └── service/
│   │   │   │       └── FlightService.java
│   │   │   ├── passengers/
│   │   │   │   ├── PassengerFeature.java
│   │   │   │   ├── dto/
│   │   │   │   ├── handler/
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── Passenger.java
│   │   │   │   │   ├── PassengerRepository.java
│   │   │   │   │   └── impl/PassengerRepositoryImpl.java
│   │   │   │   └── service/
│   │   │   │       └── PassengerService.java
│   │   │   └── bookings/
│   │   │       ├── BookingFeature.java
│   │   │       ├── dto/
│   │   │       ├── handler/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       │   ├── entity/
│   │   │       │   │   └── Booking.java
│   │   │       │   ├── BookingRepository.java
│   │   │       │   └── impl/BookingRepositoryImpl.java
│   │   │       └── service/
│   │   │           └── BookingService.java
│   └── resources/
│       ├── application.json                      # base config
│       ├── application-dev.json                  # dev profile
│       ├── application-qa.json                   # qa profile
│       ├── application-prod.json                 # prod profile
│       ├── db/
│       │   ├── schema.sql
│       │   └── data.sql
│       └── logback.xml
├── test/
│   └── java/com/airline/booking/demo/
│       ├── feature/airlines/repository/AirlineRepositoryImplSpec.groovy
│       ├── feature/flights/repository/FlightRepositoryImplSpec.groovy
│       └── feature/passengers/repository/PassengerRepositoryImplSpec.groovy
├── Dockerfile
├── .dockerignore
└── pom.xml
```

# 📁 Project Structure Overview (Vert.x + Spock + Guice)

This document describes the package/layout structure and architectural roles inside the project.

---

## 🚀 High-Level Overview

The application is a **Vert.x 5 reactive microservice** structured into **features**, each containing:

- `handler/` → HTTP Route Handlers (Controller-like)
- `service/` → Business Logic
- `repository/` → Data Persistence
- `dto/` → Request/Response Models
- `entity/` → Domain Entities (live inside repository)
- `mapper/` → DTO ↔ Entity Mappers
- `Feature.java` → Registers routes & dependencies

Guice (`bootstrap/`) wires dependencies. Vert.x `MainVerticle` starts HTTP server & routes.

---

# 🏗 Architecture & Project Structure

This project is a **Vert.x 5 reactive microservice** using:

- **Vert.x Web** for HTTP routing
- **Vert.x Reactive PostgreSQL Client**
- **JSR-380 Bean Validation** (Hibernate Validator)
- **Guice** for dependency injection
- **Spock** for testing

Features are organized in **vertical slices** (Airlines, Flights, Passengers, Bookings).

---

## 🧠 Request Flow (Reactive + Validation)

1. **HTTP Request → Handler**
2. **DTO parsed via Jackson**
3. **JSR Bean Validation triggered**
4. **DTO mapped → Entity**
5. **Service executes business logic**
6. **Repository runs reactive SQL** using:


7. **Response returned as JSON**

---

## 🧩 JSR-380 Bean Validation Usage

DTO example:

```java
public record AirlineRequest(@NotBlank(message = "cant' be null or empty") String code,
                             @NotBlank(message = "cant' be null or empty") String name,
                             @NotBlank(message = "cant' be null or empty") String country) {

}
```
---
## 🌱 Profiles (dev / qa / prod)

1. **Config loaded from:**

2. **application-dev.json**

3. **application-qa.json**

4. **application-prod.json**

---

## ✅ Run application locally
```
mvn clean compile exec:java -Dprofile=dev
```

## ✔ Test application after running
```
http://127.0.0.1:8080/ready
http://127.0.0.1:8080/health
```