# Vigentis 1.0 - imagen para publicar en internet (Render)

# Etapa 1: compilar el proyecto (incluye la web de React ya compilada en resources/static)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# Etapa 2: ejecutar solo con lo necesario
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar vigentis.jar
# Memoria ajustada al plan gratuito (512 MB)
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"
EXPOSE 8080
CMD ["java", "-jar", "vigentis.jar"]
