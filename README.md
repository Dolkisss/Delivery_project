
# 🚚 Spring Delivery Platform

Microservices backend system that simulates a **delivery order platform**.  
The project demonstrates modern backend architecture using **Spring Boot**, **Apache Kafka**, **Docker**, and **PostgreSQL**.

The system is designed as a **distributed event‑driven application** where independent services communicate through Kafka events while sharing API contracts through a common library.

---
# 🧠 Key Concepts Demonstrated

This project demonstrates several important backend engineering practices:

### Microservices Architecture
Each service is isolated and owns its business domain.

### Event Driven Design
Services communicate through **Kafka events instead of direct synchronous calls**.

### Shared API Contracts
The `common-libs` module prevents DTO duplication between services.

### Containerized Infrastructure
PostgreSQL and Kafka are started using Docker containers.

### Multi‑Module Gradle Project
The project is structured using **Gradle Kotlin DSL** with multiple modules.

---

# 🧱 Architecture

The project is implemented as a **Gradle multi‑module microservices system**.

Main modules:

- `order-service`
- `payment-service`
- `delivery-service`
- `common-libs`

Each service is an **independent Spring Boot application** responsible for a specific business domain.

Communication between services is implemented using **Apache Kafka events**, allowing services to remain loosely coupled.

High level flow:

Client → Order Service → Payment Service → Delivery Service

Kafka is used to propagate important domain events between services.

---

# 📦 Project Structure

```
Spring_Delivery
│
├── order-service
├── payment-service
├── delivery-service
├── common-libs
│
├── docker-compose.yaml
├── build.gradle.kts
└── settings.gradle.kts
```

---

# ⚙️ Tech Stack

## Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate

## Messaging

- Apache Kafka

Used for asynchronous communication between services.

## Database

- PostgreSQL

## Infrastructure

- Docker
- Docker Compose

## Build System

- Gradle (Kotlin DSL)

The project is implemented as a **multi‑module Gradle build**.

---

# 🧩 Services

## Order Service

Responsible for managing customer orders.

Main responsibilities:

- Creating orders
- Storing order information in database
- Managing order items
- Publishing domain events after important state changes

Example DTOs:

- `CreateOrderRequestDto`
- `OrderDto`
- `OrderItemDto`

---

## Payment Service

Handles payment processing for orders.

Responsibilities:

- Creating payments for orders
- Managing payment status
- Publishing **OrderPaidEvent** after successful payment

This event is later consumed by other services in the system.

---

## Delivery Service

Responsible for the delivery process.

Responsibilities:

- Processing delivery assignments
- Listening to payment/order events
- Publishing **DeliveryAssignedEvent** when delivery is assigned

This service simulates the logistics side of the platform.

---

## Common Libs

Shared module containing **API contracts and event models** used by all services.

Includes:

- HTTP DTOs
- Kafka event models
- shared enums and contracts

Examples:

- `OrderPaidEvent`
- `DeliveryAssignedEvent`
- `OrderStatus`
- `PaymentStatus`

Using a shared module ensures **consistent data contracts across microservices**.

---

# 📡 Event Driven Communication

The system uses **Kafka events** to coordinate actions between services.

Example event flow:

1. Client creates order via **Order Service**
2. Payment is created in **Payment Service**
3. After successful payment → **OrderPaidEvent** is published
4. **Delivery Service** consumes the event
5. Delivery is assigned → **DeliveryAssignedEvent** is published

This pattern allows services to remain **loosely coupled and independently scalable**.

---

# 🐳 Infrastructure

The repository contains a **Docker Compose configuration** that starts required infrastructure services.

Containers:

- **PostgreSQL**
- **Apache Kafka**

Example:

```
docker-compose up -d
```

PostgreSQL runs on:

```
localhost:5433
```

Kafka runs on:

```
localhost:9092
```

---

# ▶️ Running the Project

### 1 Clone repository

```
git clone https://github.com/Dolkisss/Delivery_project
```

### 2 Start infrastructure

```
docker-compose up -d
```

This will start:

- PostgreSQL
- Kafka

### 3 Build project

```
./gradlew build
```

### 4 Run services

Each service can be started independently:

```
./gradlew :order-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :delivery-service:bootRun
```