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
  -DskipTests \
  -Dskip.npm \
  -Dskip.yarn \
  -Dskip.frontend \
  -Dspring-boot.repackage.skip=false

# ===== RUN =====
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-Xmx512m"
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
