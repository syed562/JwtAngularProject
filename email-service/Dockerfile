
FROM eclipse-temurin:17-jdk-alpine


WORKDIR /app


COPY target/*.jar email-service.jar


EXPOSE 9005


ENV SPRING_APPLICATION_NAME=email-service \
    SERVER_PORT=9005 \
    SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
    SPRING_KAFKA_CONSUMER_GROUP_ID=email-group


ENTRYPOINT ["java", "-jar", "email-service.jar"]
