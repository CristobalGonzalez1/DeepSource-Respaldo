# API propia de DeepSource

Dos aplicaciones Spring Boot independientes con bases de datos H2 persistentes (archivos `data/` por servicio).

## Requisitos
Java 17+ y Maven 3.9+.

## Iniciar (en terminales distintas)
```
cd backend/projects-service && mvn spring-boot:run
cd backend/hours-service && mvn spring-boot:run
```
`projects-service`: http://localhost:8081/api/projects y `/api/tasks`.
`hours-service`: http://localhost:8082/api/hours.

Cada endpoint soporta GET lista, GET /{id}, POST, PUT /{id}, DELETE /{id}.

## Pruebas
Desde cada servicio: `mvn clean verify`. Tests MockMvc ejercitan el CRUD y generan reportes JaCoCo en `target/site/jacoco/index.html`. No equivalen a cobertura de tests Android >=80%.

## Observaciones
Sin autenticación de servidor, ni validación de propiedad de tareas; **solo para demo local**. La app intenta sincronizar proyectos y tareas al iniciarse y cuando se modifica su catálogo. Si el servidor no responde, utiliza la copia local. Para uso productivo se requiere JWT/OAuth, roles autorizados server-side y referencias relacionales entre entidades.
