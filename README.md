🚀 Overview

This project is a scalable flight booking backend system built using Spring Boot microservices architecture.
All services are containerized using Docker and exposed through an API Gateway with JWT-based authentication.

🧩 Microservices Included
Service	Description
auth-service	User authentication, JWT, change password
flight-service	Flight creation, search, admin inventory
ticket-service	Ticket booking, cancellation
passenger-service	Passenger details
email-service	Booking confirmation emails
config-server	Centralized configuration
eureka-server	Service discovery
api-gateway	Routing, security, CORS
🔐 Security Design

JWT-based authentication

Role-based authorization:

ROLE_ADMIN → add/delete flights

ROLE_USER → book tickets

Security enforced at API Gateway

Cookies used for authentication (withCredentials)

🏗 Architecture Flow
Angular Frontend
       |
       v
   API Gateway (8765)
       |
------------------------------------------------
| auth | flight | ticket | passenger | email  |
------------------------------------------------
       |
   PostgreSQL / MySQL

🗂 Database Design

PostgreSQL

Auto-generated primary keys

Unique constraints enforced

Sequence synchronization handled

⚙️ Tech Stack (Backend)

Java 17

Spring Boot

Spring Security

Spring Cloud Gateway

Spring Data JPA

PostgreSQL

Docker & Docker Compose

JWT

▶️ How to Run (Docker)

docker-compose build
docker-compose up -d

Verify:

Eureka: http://localhost:8761

Gateway: http://localhost:8765

🧪 Important API Endpoints
Auth
POST /auth-service/api/auth/signin
POST /auth-service/api/auth/signup
POST /auth-service/api/auth/change-password

Flight (ADMIN only)
POST /flight-service/flight/register
DELETE /flight-service/flight/delete/{id}

Flight Search (Public)
GET /flight-service/flight/search

🧠 Key Backend Highlights

Centralized security at gateway

Stateless authentication

Clean service separation

Dockerized deployment

Production-style architecture

📌 Backend Repository Structure
backend/
├── api-gateway

├── auth-service

├── flight-service

├── ticket-service

├── passenger-service

├── email-service

├── config-server

├── eureka-server

└── docker-compose.yml
