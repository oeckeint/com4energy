# Imagen base de Java
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Instalar MySQL Client (incluye mysqldump)
RUN apt-get update && apt-get install -y mysql-client-8.0

# Establecer el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el JAR generado por Maven/Gradle al contenedor
COPY target/ghost-flows-1.0.0.jar /app/app.jar

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app/app.jar"]