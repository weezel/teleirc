# TeleIRC Bridge

## Dependencies

* Java >= 1.8

* Maven

* Camel

## Setup
Create a Telegram bot and name it whatever you like.

* Privacy mode needs to be disabled

* Create a group and confirm that a first letter is a hash (#)

* Configure `application-teleirc.yml` to match the channels and groups you have

* Voilà

## Running

### Create a package

* mvn clean package

### Execute, and be happy

* java -Dspring.profiles.active=teleirc -jar target/irctelebridge-1.0.2.jar

