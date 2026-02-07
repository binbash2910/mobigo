# ===== Build stage =====
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -Pprod clean package -DskipTests

# ===== Run stage =====
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-Xmx512m"
WORKDIR /app
COPY --from=build /app/target/*.jar mobigo.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/mobigo.jar"]
