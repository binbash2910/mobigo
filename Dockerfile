# ===== BUILD =====
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B dependency:go-offline

COPY package.json package-lock.json ./
COPY angular.json tsconfig.json tsconfig.app.json ngsw-config.json ./
COPY webpack webpack/
COPY src src

RUN ./mvnw clean package \
  -Pprod \
  -P!docker-compose \
  -DskipTests

# ===== RUN =====
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-Xmx300m -Xms128m -XX:MaxMetaspaceSize=128m"
COPY --from=build /app/target/mobigo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
