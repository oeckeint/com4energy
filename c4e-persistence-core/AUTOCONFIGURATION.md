# Auto-Configuración de c4e-persistence-core

## Descripción General

`c4e-persistence-core` está configurado como una librería Spring Boot **auto-configurable**. Esto significa que cualquier microservicio que agregue esta dependencia Maven obtiene automáticamente:

- Descubrimiento de entidades JPA
- Registro de repositorios Spring Data
- Configuración de JPA/Hibernate

**Sin necesidad de**:
- `@EntityScan(...)`
- `@EnableJpaRepositories(...)`

## Cómo Funciona

### 1. Archivo de Configuración de Spring Boot

**Ubicación**: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.com4energy.persistence.autoconfigure.PersistenceCoreAutoConfiguration
```

Este archivo es leído automáticamente por Spring Boot durante el startup. Spring Boot ejecuta toda clase listada en este archivo si está disponible en el classpath.

### 2. Clase de Auto-Configuración

**Ubicación**: `src/main/java/com/com4energy/domain/autoconfigure/PersistenceCoreAutoConfiguration.java`

```java
@AutoConfiguration(before = HibernateJpaAutoConfiguration.class)
@AutoConfigurationPackage(basePackages = "com.com4energy.persistence")
@ComponentScan(basePackages = "com.com4energy.persistence")
@EnableJpaRepositories(basePackages = "com.com4energy.persistence")
public class PersistenceCoreAutoConfiguration {
}
```

**Anotaciones clave**:
- `@AutoConfiguration`: Marca esta clase como una configuración automática de Spring Boot
- `@AutoConfigurationPackage`: Registra el paquete base para entity scanning
- `@ComponentScan`: Escanea componentes bajo `com.com4energy.persistence`
- `@EnableJpaRepositories`: Activa el descubrimiento de repositorios Spring Data

### 3. Entidades JPA

Todas las entidades están bajo `com.com4energy.persistence`:
- `com.com4energy.persistence.medidas.medidah.MedidaHEntity`
- `com.com4energy.persistence.medidas.medidaqh.MedidaQHEntity`
- `com.com4energy.persistence.medidas.medidacch.MedidaCCHEntity`

### 4. Repositorios Spring Data

Todos los repositorios están bajo `com.com4energy.persistence`:
- `com.com4energy.persistence.medidas.medidah.MedidaHRepository`
- `com.com4energy.persistence.medidas.medidaqh.MedidaQHRepository`
- `com.com4energy.persistence.medidas.medidacch.MedidaCCHRepository`

## Uso en Servicios Consumidores

### Paso 1: Agregar Dependencia Maven

```xml
<dependency>
    <groupId>com.com4energy</groupId>
    <artifactId>c4e-persistence-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Paso 2: Inyectar Repositorios

Directamente en cualquier componente Spring (sin necesidad de configuración adicional):

```java
@Component
@RequiredArgsConstructor
public class MeasureService {
    private final MedidaHRepository medidaHRepository;
    private final MedidaQHRepository medidaQHRepository;
    private final MedidaCCHRepository medidaCCHRepository;
    
    // ... métodos
}
```

**NO es necesario**:
```java
@SpringBootApplication
@EntityScan(basePackages = "com.com4energy.persistence")  // ← NO NEEDED
@EnableJpaRepositories(basePackages = "com.com4energy.persistence")  // ← NO NEEDED
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Validación

### Test de Auto-Configuración

El módulo `c4e-persistence-core` incluye una prueba de integración:

```bash
mvn test -pl c4e-persistence-core -Dtest=PersistenceCoreAutoConfigurationTest
```

Output esperado:
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

La prueba verifica que:
1. Spring Boot carga la auto-configuración correctamente
2. Los 3 repositorios están disponibles como beans
3. No hay errores en el contexto

### Validación Manual

En cualquier servicio consumidor, al arrancar, deberías ver logs similares a:

```
INFO o.s.d.r.c.RepositoryConfigurationDelegate -- Bootstrapping Spring Data JPA repositories in DEFAULT mode.
INFO o.s.d.r.c.RepositoryConfigurationDelegate -- Finished Spring Data repository scanning in 4 ms. Found 3 JPA repository interfaces.
```

## Archivos Involucrados

```
c4e-persistence-core/
├── src/
│   ├── main/
│   │   ├── java/com/com4energy/domain/
│   │   │   ├── autoconfigure/
│   │   │   │   └── PersistenceCoreAutoConfiguration.java ← Configuración
│   │   │   └── model/
│   │   │       └── medidas/
│   │   │           ├── medidah/
│   │   │           ├── medidaqh/
│   │   │           └── medidacch/
│   │   └── resources/
│   │       └── META-INF/spring/
│   │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports ← Registro
│   └── test/
│       └── java/com/com4energy/domain/autoconfigure/
│           └── PersistenceCoreAutoConfigurationTest.java ← Prueba
└── pom.xml ← Dependencias necesarias
```

## Cambios Recientes

### Cambio 1: Mejorar `PersistenceCoreAutoConfiguration`

Se añadieron `@ComponentScan` y `@EnableJpaRepositories` explícitamente para garantizar que:
- Las entidades se descubran correctamente
- Los repositorios se registren como beans

### Cambio 2: Validar `AutoConfiguration.imports`

Se verificó que el archivo está en la ubicación correcta y contiene la clase de configuración.

## Notas

- **Optional Dependency**: `spring-boot-autoconfigure` está marcado como `<optional>true</optional>` en el pom.xml. Esto permite que servicios que no usen Spring Boot puedan seguir usando el módulo sin traer todas las dependencias de Spring Boot.

- **Ordering**: Se usa `@AutoConfiguration(before = HibernateJpaAutoConfiguration.class)` para asegurar que la configuración de domain-model se ejecute antes que Hibernate, permitiendo que Hibernate descubra las entidades correctamente.

- **Multiple Services**: Diversos servicios pueden consumir esta librería y todos obtendrán automáticamente los mismos repositorios y entidades, sin duplicación de código.

## Troubleshooting

### Problema: "No beans of 'MedidaHRepository' type found"

**Causa**: La librería no fue recompilada e instalada después de cambios.

**Solución**:
```bash
cd c4e-persistence-core
mvn clean install
```

### Problema: Entidades no se detectan

**Causa**: Mala configuración de paquetes.

**Verificación**:
```bash
# Asegurarse de que las entidades están bajo com.com4energy.persistence
find src/main/java -name "*Entity.java" -exec grep -l "package com.com4energy.persistence" {} \;
```

### Problema: Repositorios no se registran

**Causa**: Los repositorios no heredan de JPA Repository o están en paquete incorrecto.

**Verificación**:
```bash
# Asegurarse de que los repositorios están bajo com.com4energy.persistence
find src/main/java -name "*Repository.java" -exec grep -l "package com.com4energy.persistence" {} \;
```

## Referencias

- [Spring Boot Auto-configuration Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Spring Data JPA Repository](https://spring.io/projects/spring-data-jpa)

