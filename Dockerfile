FROM maven:3.6.3-openjdk-8 as builder
WORKDIR /tmp/
#COPY pom.xml /tmp/pom.xml
COPY . .
RUN mvn -B clean package

FROM openjdk:8-jre-slim as app
WORKDIR /app
COPY --from=builder --chown=1000:1000 /tmp/target/irctelebridge-1.0.6.1.jar .
USER 1000
CMD ["java", "-Dspring.profiles.active=teleirc" "-jar", "irctelebridge-1.0.6.1.jar"]

