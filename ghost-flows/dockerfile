# Imagen base de Java
FROM openjdk:17-jdk-slim

# Instalar MySQL Client (incluye mysqldump)
RUN apt-get update && apt-get install -y default-mysql-client

# Establecer el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el JAR generado por Maven/Gradle al contenedor
COPY target/ghost-flows-1.0.0.jar /app/app.jar

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app/app.jar"]