FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-Xmx512m"
COPY target/*.jar mobigo.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /mobigo.jar"]
