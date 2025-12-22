@echo off
echo Starting Microservices (DEV)

echo Starting Eureka Server...
start "" /b java -jar service-registry\target\service-registry-0.0.1-SNAPSHOT.jar
timeout /t 15 > nul

echo Starting Config Server...
start "" /b java -jar ConfigServer\target\ConfigServer-0.0.1-SNAPSHOT.jar
timeout /t 15 > nul

echo Starting API Gateway...
start "" /b java -jar api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar
timeout /t 10 > nul

echo Starting Auth Service...
start "" /b java -jar auth-service\target\spring-security-own-0.0.1-SNAPSHOT.jar
timeout /t 10 > nul

echo Starting Flight Service...
start "" /b java -jar flight-service\target\flight-service-0.0.1-SNAPSHOT.jar
timeout /t 10 > nul

echo Starting Passenger Service...
start "" /b java -jar passenger-service\target\passenger-service-0.0.1-SNAPSHOT.jar
timeout /t 10 > nul

REM echo Starting Ticket Service...
start cmd /k java -jar ticket-service\target\ticket-service-0.0.1-SNAPSHOT.jar
REM timeout /t 10 > nul

REM echo Starting Email Service...
start cmd /k java -jar email-service\target\email-service-0.0.1-SNAPSHOT.jar
