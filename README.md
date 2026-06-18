# boiler-alpha

A multi-module Spring Boot project built with Maven.

## Modules

- `boiler-common`: shared constants and common result models
- `boiler-pojo`: DTO, VO, and entity classes
- `boiler-server`: web layer, service layer, MyBatis mapper, and application entry

## Tech Stack

- Java 17
- Spring Boot 3.4.0
- Maven
- MyBatis
- MySQL

## Run

```bash
./mvnw clean test
./mvnw -pl boiler-server spring-boot:run
```
