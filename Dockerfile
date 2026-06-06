FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/FetchRate-0.4.jar app.jar
EXPOSE 8000
ENV SERVER_ADDRESS=0.0.0.0
VOLUME /app/data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["start_http_server"]
