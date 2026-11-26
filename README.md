# DeepSource — Aplicación Android de gestión de proyectos y registro de horas (V15)

**Proyecto académico | Duoc UC · Ingeniería en Informática · DSY1105 Desarrollo de Aplicaciones Móviles**  
**Versión documentada:** V15 (noviembre de 2026)  
**Repositorio GitHub:** `[https://github.com/CristobalGonzalez1/DeepSource-Respaldo.git]` · 

## 1. Descripción

DeepSource es una aplicación Android orientada a gestionar **proyectos, tareas y tiempo trabajado**. Permite iniciar y pausar un cronómetro, registrar horas manualmente, consultar un historial por proyecto, adjuntar evidencias y usar un widget en la pantalla de inicio. Desde la V15 incorpora sincronización de proyectos, tareas y registros de horas con **dos microservicios REST Spring Boot**, conservando una copia local para la demostración sin conexión.

El proyecto se desarrolló progresivamente desde las evaluaciones previas. Este documento reúne la información pertinente hasta la **V15** y reemplaza los README dispersos de versiones antiguas. Las funciones indicadas como *pendientes* no deben presentarse como totalmente operativas.

## 2. Tecnologías

| Área | Tecnología | Uso |
|---|---|---|
| Aplicación móvil | Kotlin, Android SDK 35, Jetpack Compose | Interfaz nativa Android; API mínima 26 |
| Arquitectura | MVVM, ViewModel, StateFlow, corrutinas | Separación de UI, estado y datos; operaciones asíncronas |
| Datos locales | SharedPreferences y archivos privados | Sesión demo, proyectos, tareas, horas y evidencias |
| Cliente HTTP | Retrofit + Gson | Peticiones REST y serialización JSON |
| Backend | Java, Spring Boot, Spring Data JPA | Dos servicios independientes de proyectos y horas |
| Base de datos backend | H2 persistente en archivos | Datos locales de demostración por microservicio |
| Pruebas | JUnit, pruebas de Spring/MockMvc, JaCoCo | Validaciones y cobertura; MockK disponible como dependencia |
| Colaboración | Git/GitHub; Trello | Control de versiones y planificación del equipo |

## 3. Funciones incluidas en V15

- **Proyectos y tareas:** creación, selección y gestión de tareas asociadas a cada proyecto, con duración estimada por tarea y catálogo conectado al servicio de proyectos.
- **Cronómetro:** iniciar, pausar, reanudar y finalizar actividades; avisos de tiempo estimado y opción de extender el límite en **30 minutos**.
- **Registro de horas:** ingreso manual con proyecto, tarea, fecha, horas/minutos y comentario opcional; también registro al terminar un cronómetro y mediante el widget.
- **Historial:** consulta de registros, organizados por proyecto, y recuperación de registros remotos reconocidos al iniciar.
- **Evidencias:** fotografía con cámara o documento mediante selector de Android, asociado al registro y almacenado de forma **local** (no se sube al servidor).
- **Widget Android:** acceso rápido a cronómetro, proyecto/tarea y registro manual; adaptable al tamaño disponible.
- **Avisos y alarmas:** notificaciones configurables, vibración/sonido y recordatorios sujetos a permisos y restricciones de Android.
- **Acceso y roles:** cuentas demo normal y administrador, perfil, sesión local persistente y protección de rutas administrativas dentro de la app. **No equivale a autenticación/autorización segura de servidor.**
- **Sincronización:** catálogo de proyectos/tareas con `projects-service`; registro de horas con `hours-service` desde formulario, finalización y widget. Ante fallo de red, se mantiene el dato local y se intenta sincronizar nuevamente.

## 4. Evolución del proyecto (V3 → V15)

| Versión | Avance representativo |
|---|---|
| **V3** | Ampliación de la base Android/MVVM: formularios, cámara, recordatorio y widget. |
| **V4** | Mejoras de navegación inferior, selector visual y evidencias plegables. |
| **V5** | Catálogo de tareas por proyecto, perfiles demo, ajustes de notificaciones, alarmas y widget redimensionable. |
| **V6** | Perfil como pestaña, acceso administrativo restringido por rol en la app y permisos de notificaciones. |
| **V7** | Recuperación de la pantalla especial de alerta; aviso de tarea y extensión de 30 minutos. |
| **V8** | Acceso administrativo reforzado en la navegación y modos ampliados del widget. |
| **V9** | Persistencia del usuario y rol entre aperturas hasta cerrar sesión. |
| **V10** | Widget compacto con selector desplegable; registro manual con fecha y duración; formulario al terminar tarea. |
| **V11** | Primer avance de evaluación: dos microservicios Spring Boot, Retrofit y demostración de API externa en una pantalla separada. |
| **V12** | Proyectos y tareas sincronizados desde el flujo habitual; historial agrupado y duración estimada por tarea. |
| **V13** | Cámara y documentos en registro manual y finalización de cronómetro; evidencias vinculadas al registro. |
| **V14** | Reglas de negocio separadas, pruebas JUnit agrupadas (22 escenarios) y verificación JaCoCo acotada. |
| **V15** | Envío y recuperación de registros de horas desde `hours-service`, con `clientId` para evitar duplicados habituales. |

### Hitos recomendados para explicar en GitHub

**V5 → V12 → V14 → V15** ilustran respectivamente la base funcional, la integración del catálogo con API, las pruebas/cobertura y la sincronización de horas. **Cada commit debe representar aportes reales del integrante que lo firma; no se deben atribuir retrospectivamente cambios a otras personas.**

## 5. Arquitectura y recorrido de datos

```text
Pantallas Jetpack Compose / Widget
               |
         ViewModel + StateFlow
               |
     Repository (copia local)
               |
     Retrofit + Gson (HTTP/JSON)
            /       \
  projects-service  hours-service
      :8081             :8082
       |                  |
     JPA/H2             JPA/H2
```

**Ejemplo, guardar horas:** el usuario completa un formulario → se valida la duración → se genera un registro `TimeLog` → `LocalProjectRepository` lo guarda localmente → `HoursSyncMapper` adapta los datos → Retrofit ejecuta `POST /api/hours` → el controlador Spring Boot almacena el registro con JPA/H2. El `clientId` permite reconocer reintentos; el historial puede recuperar registros remotos de proyectos conocidos.

**Precisión:** guardar localmente **no garantiza** que el POST haya llegado al servidor. Es necesario verificarlo consultando la API.

## 6. Estructura del repositorio

```text
DeepSource_V15/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/cl/deepsource/horas/
│       │   ├── feature/home/          # Inicio y ViewModel
│       │   ├── feature/workflow/      # Cronómetro, formularios e historial
│       │   ├── feature/auth/          # Login, roles y perfil de usuario
│       │   ├── data/repository/       # Repositorio local/sincronización
│       │   ├── data/remote/           # Interfaces Retrofit y mapeos
│       │   ├── domain/logic/          # Reglas de negocio verificables
│       │   ├── core/nativefeatures/   # Cámara, evidencias y avisos
│       │   ├── widget/                # Widget Android
│       │   └── navigation/            # Rutas de Compose
│       └── test/                      # Pruebas unitarias JUnit
├── backend/
│   ├── projects-service/             # Spring Boot: proyectos y tareas
│   └── hours-service/                # Spring Boot: registros de horas
├── gradle/
├── app/build.gradle.kts
└── README.md
```

### Archivos clave

- `app/src/main/java/cl/deepsource/horas/domain/logic/TimerRules.kt` — cálculos y reglas del cronómetro.
- `.../domain/logic/RegistrationRules.kt` — reglas del registro e historial.
- `.../domain/logic/ProjectTaskRules.kt` — reglas de proyectos y tareas.
- `.../data/repository/LocalProjectRepository.kt` — almacenamiento y sincronización.
- `.../data/remote/RemoteApi.kt` — definiciones de proyectos, horas y API de feriados.
- `.../data/remote/HoursSyncMapper.kt` — adaptación de datos para el servicio de horas.
- `.../feature/workflow/WorkflowScreens.kt` — formulario manual, finalización e historial.
- `backend/hours-service/src/main/java/cl/deepsource/hours/HoursController.java` — endpoint de registros de horas.

## 7. Requisitos y puesta en marcha

**Android:** Android Studio, SDK 35, JDK compatible con Gradle del proyecto (se usa objetivo JVM 17); emulador o teléfono con **Android 8.0 / API 26 o superior**.  
**Backend:** JDK 17 o superior y Maven 3.9 o superior.  
**Nota:** el paquete puede no incluir los ejecutables estándar `gradlew.bat` y `gradle-wrapper.jar`; si faltan, ejecutar las tareas desde **Run Anything** de Android Studio con la distribución Gradle configurada en el IDE.

1. Extraer el proyecto y abrir su **carpeta raíz** desde Android Studio → **Open**.
2. Sincronizar Gradle y seleccionar el dispositivo/emulador.
3. Iniciar **ambos** microservicios en dos terminales independientes desde la raíz del repositorio:

   **Terminal A:**
   ```powershell
   cd backend/projects-service
   mvn spring-boot:run
   ```
   **Terminal B:**
   ```powershell
   cd backend/hours-service
   mvn spring-boot:run
   ```

4. Verificar los endpoints desde Windows:
   - Proyectos: http://localhost:8081/api/projects
   - Tareas: http://localhost:8081/api/tasks
   - Horas: http://localhost:8082/api/hours
5. Ejecutar `app` en Android Studio. El emulador Android accede al PC mediante `10.0.2.2`, **no** mediante `localhost`.

### Configuración de red

La configuración predeterminada de `app/build.gradle.kts` es:

```properties
API_BASE_URL=http://10.0.2.2:8081/
HOURS_BASE_URL=http://10.0.2.2:8082/
```

Se pueden sobrescribir desde `gradle.properties` local o mediante propiedades `-P` de Gradle. En teléfono físico, usar la IP del PC accesible desde el teléfono o reenvío ADB configurado; abrir los puertos necesarios en el firewall **solo dentro del entorno de pruebas**. No exponer estos servicios sin seguridad a Internet.

### Acceso de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Normal | `demo@deepsource.cl` | `123456` |
| Administrador | `admin@deepsource.cl` | `admin123` |

Son **credenciales simuladas almacenadas en el prototipo**, no usuarios de una API de autenticación real. El inicio con Google requiere configuración externa y no reemplaza la verificación de identidad en backend.

## 8. Endpoints REST

| Servicio | Método | Ruta | Finalidad |
|---|---|---|---|
| Projects (8081) | GET / POST | `/api/projects` | Consultar / crear proyectos |
| Projects (8081) | GET / PUT / DELETE | `/api/projects/{id}` | Consultar / modificar / eliminar por ID |
| Projects (8081) | GET / POST | `/api/tasks` | Consultar / crear tareas |
| Projects (8081) | GET / PUT / DELETE | `/api/tasks/{id}` | Consultar / modificar / eliminar por ID |
| Hours (8082) | GET / POST | `/api/hours` | Consultar / crear registros |
| Hours (8082) | GET / PUT / DELETE | `/api/hours/{id}` | Consultar / modificar / eliminar por ID |

Ejemplo de consulta desde PowerShell:

```powershell
Invoke-RestMethod http://localhost:8082/api/hours | ConvertTo-Json -Depth 5
```

**Prueba real de sincronización:** registrar 1 h 15 min desde Android, confirmar su aparición en el historial y comprobar en `/api/hours` que el nuevo registro incluye una duración de **75 minutos**. Repetir después de finalizar el cronómetro y desde el widget. Si la respuesta está vacía, la sincronización todavía no ha sido demostrada.

## 9. API externa de feriados (Roison)

El proyecto declara un cliente Retrofit para `https://api.roison.cl/feriados.json`, mediante `HolidayApi` en `RemoteApi.kt`. Su objetivo es consultar los feriados chilenos.

**Estado en V15:** la API está **declarada/configurada**, pero en las pantallas del flujo principal **no se ha verificado una invocación funcional que muestre sus resultados**. Por lo tanto, **no debe presentarse como integración externa completada** hasta incorporar la llamada a la UI y probar su respuesta con conectividad real. La presencia de una interfaz Retrofit por sí sola no cumple la integración demostrable.

## 10. Pruebas unitarias y cobertura

En Android se incluyen pruebas JUnit de reglas de cronómetro, registro de horas, proyectos/tareas, formularios, permisos y mapeo de registros. La V14 consolidó pruebas de lógica en **22 escenarios agrupados**; V15 agregó pruebas para el mapeo de horas. En backend hay pruebas de controladores y reintentos (`HoursIdempotencyTest.java`).

Desde **Run Anything** (doble `Ctrl`) en Android Studio:

```text
gradle :app:testDebugUnitTest --rerun-tasks --no-build-cache
gradle :app:verifyBusinessLogic80 --rerun-tasks --no-build-cache
```

Informe HTML de reglas seleccionadas:

```text
app/build/reports/jacoco/businessLogicCoverageReport/html/index.html
```

En la ejecución compartida por el equipo del **26 de noviembre de 2026**, JaCoCo mostró **97% de instrucciones y 90% de ramas** en el *subconjunto de reglas* evaluado. El comando `verifyBusinessLogic80` finalizó con `BUILD SUCCESSFUL` sin caché.

**Alcance exacto de ese porcentaje:** `core/validation/**`, `domain/logic/**` y `feature/auth/domain/RolePermissions*`. **No representa cobertura del 97% de toda la aplicación**: quedan fuera de este informe ViewModels, repositorios, Retrofit y buena parte de Android. Para defender el criterio completo de la evaluación, acompañar el informe acotado con la lista de clases medidas, las pruebas y las verificaciones adicionales que correspondan.

Otras tareas útiles:

```text
gradle :app:businessLogicCoverageReport
gradle :app:createDebugUnitTestCoverageReport
```

El último comando genera un reporte más amplio, cuyo porcentaje puede ser muy distinto del informe acotado.

### Pruebas del backend

Dentro de **cada** servicio:

```powershell
mvn clean verify
```

Los resultados de sus pruebas e informes JaCoCo pertenecen al backend; no sustituyen las pruebas del proyecto Android.

## 11. Demostración práctica sugerida

1. Mostrar el login demo y abrir un proyecto con tareas.
2. Registrar **1 h 15 min** manualmente; verificar el historial.
3. Consultar `http://localhost:8082/api/hours` y comprobar el nuevo JSON.
4. Iniciar/pausar/finalizar cronómetro; volver a consultar el servicio.
5. Mostrar el widget y el guardado de un registro rápido.
6. Abrir `TimerRules.kt` y su prueba `TimerRulesTest.kt`; ejecutar JUnit.
7. Abrir JaCoCo, indicar **97% instrucciones / 90% ramas de reglas seleccionadas**, sin atribuir esos porcentajes a toda la app.

## 12. Limitaciones y mejoras pendientes

- **Autenticación y autorización de producción:** los perfiles y roles son locales/demostrativos; los endpoints no incluyen autenticación robusta ni autorización por propietario. No usar información sensible ni desplegar los servicios públicamente.
- **Roison:** cliente definido, pero falta mostrar y verificar sus datos en una funcionalidad cotidiana de DeepSource.
- **Sincronización:** implementación orientada a una demostración académica; no garantiza resolución de conflictos ni replicación completa multiusuario.
- **Adjuntos:** cámara/archivos quedan en el dispositivo y no se transfieren al backend.
- **Pruebas:** la cobertura de ViewModels, repositorios y comunicación de red no queda demostrada por el reporte acotado del 97%.
- **Entrega:** APK de release firmada, configuración de firma `.jks`, repositorio público, evidencias de Trello y aportes auténticos de integrantes deben revisarse según la pauta antes de afirmar que la evaluación está completamente cubierta.
- **Validación final:** verificar compilación Android, pruebas Maven, permisos, widget, notificaciones y sincronización real en la máquina usada para la defensa.

## 13. Equipo y créditos

| Integrante | Responsabilidad / aporte verificable | GitHub |
|---|---|---|
| `[Nombre 1]` | `[Aporte real]` | `[@usuario1]` |
| `[Nombre 2]` | `[Aporte real]` | `[@usuario2]` |
| `[Nombre 3]` | `[Aporte real]` | `[@usuario3]` |
| `[Nombre 4]` | `[Aporte real]` | `[@usuario4]` |

Completar con los nombres, usuarios y aportes reales. Cada integrante debe autorizar sus propios commits con el nombre/correo correcto y publicar solamente cambios que efectivamente realizó.

---

**Estado de referencia:** código V15 revisado y resultados JaCoCo aportados por el equipo. Este README describe funciones y pruebas verificables, **no certifica por sí solo que se haya completado toda la EV4**.
