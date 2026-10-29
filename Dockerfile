FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/arc-transit-system-0.0.1-SNAPSHOT.jar /app/arc-transit.jar
RUN mkdir /app/data && chown -R 10001:10001 /app
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/arc-transit.jar"]
