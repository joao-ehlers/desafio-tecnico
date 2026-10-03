FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
 
COPY pom.xml .
RUN mvn -B dependency:go-offline
 
COPY src ./src
RUN mvn -B clean package -DskipTests
 
FROM eclipse-temurin:21-jre
WORKDIR /app
 
RUN useradd --system --no-create-home appuser
USER appuser
 
COPY --from=build /app/target/*.jar app.jar
 
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
 
