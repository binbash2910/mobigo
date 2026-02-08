# ===== BUILD =====
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw clean package \
  -Pprod \
  -P!docker-compose \
  -DskipTests \
  -Dskip.npm \
  -Dskip.yarn \
  -Dskip.frontend

# ===== RUN =====
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-Xmx512m"
COPY --from=build /app/target/mobigo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
