FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests \
    && mv target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]