FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/equity-composition-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xmx2g","-jar","app.jar"]
