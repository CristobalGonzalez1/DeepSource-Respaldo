# DeepSource — Evaluación Parcial 2 (DSY1105)

Aplicación Android para administrar proyectos, tareas y horas de trabajo, realizada con **Kotlin, Jetpack Compose y MVVM**.

## Integrantes
- Integrante 1: **Cristobal Gonzalez**
- Integrante 2: **Valentina Saavedra**
- Integrante 3: **Felipe Galarce**
- Integrante 4: **Fernando Guajardo**
- Equipo: **EABMODEL**

## Funcionalidades
- Login de demostración, recuperación/solicitud de acceso simuladas e inicio con Google sujeto a configuración externa.
- Navegación entre autenticación, Home y perfil.
- Crear, elegir y eliminar proyectos; registrar tiempo manualmente; iniciar, pausar y terminar tareas.
- Formularios validados con `HomeFormValidator.kt`, desacoplados de Compose; repositorio y ViewModel también comprueban entradas.
- Persistencia local en `SharedPreferences`, conservada al cerrar y abrir la app.
- Animaciones Compose en mensajes de error, aparición de evidencia y cambios de tamaño.
- **Recurso nativo 1: cámara** con `TakePicturePreview`, asociando una evidencia fotográfica al proyecto actual, guardada privadamente en `filesDir/task_evidence`.
- **Recurso nativo 2: notificaciones** mediante `NotificationManager`, aviso cuando se confirma el inicio y término de una tarea. Solicitud de permiso en Android 13+.
- Widget de pantalla de inicio como capacidad adicional.

## Ejecutar
1. Abrir esta **carpeta raíz** en Android Studio; requiere JDK 17, SDK Android 35 y acceso a las dependencias de Gradle.
2. Sincronizar Gradle, ejecutar `app` en dispositivo o emulador Android 8.0+; para la cámara es preferible dispositivo real o emulador con cámara configurada.
3. Demo: `demo@deepsource.cl` / `123456`.
4. Crear un proyecto, iniciar una tarea, habilitar notificaciones y detenerla. Usar **Tomar fotografía** para capturar evidencia.
5. Cerrar/reabrir la aplicación y comprobar que conserva proyectos, registros y evidencia.
6. Inicio Google requiere propiedad `GOOGLE_WEB_CLIENT_ID` apropiada; **no** incluir claves privadas en Git.

## Arquitectura
- `feature/auth/`: pantallas de autenticación, ViewModels y repositorios.
- `feature/home/`: estado, ViewModel, interfaz y diálogos.
- `core/validation/HomeFormValidator.kt`: reglas independientes de la UI.
- `core/nativefeatures/`: cámara (almacenamiento interno) y notificaciones.
- `domain/`: entidades y contrato del repositorio.
- `data/repository/LocalProjectRepository.kt`: persistencia local.
- `navigation/`: rutas y navegación Compose.

## Pruebas manuales para mostrar al docente
- Campos vacíos, nombre de proyecto con 2 caracteres y nombre con más de 60 caracteres.
- Horas no numéricas, horas >23, minutos >59, duración cero y nota >300 caracteres.
- Crear/seleccionar/eliminar proyectos; iniciar/pausar/reanudar/finalizar tarea, verificar notificación.
- Conceder y denegar notificaciones en Android 13+; verificar que la app funciona sin permiso.
- Capturar fotografía, regresar a Home y comprobar miniatura y persistencia tras reiniciar.
- Comprobar funcionamiento offline de proyectos y registros.

## Colaboración y entrega (debe realizar el equipo)
Agregar enlace del repositorio público: **[pendiente]** y tablero Trello: **[pendiente]**. Los commits **deben ser auténticos**, con participación verificable de ambos estudiantes. No fabricar historial de Git ni evidencias de Trello. Entregar el ZIP de código en AVA, registrar entrega individual y **no efectuar commits después de la fecha límite previa a la defensa**, conforme a la pauta.

## Limitaciones conocidas
La autenticación estándar y recuperación/solicitud son demostrativas, sin backend real. La evidencia se asocia al **proyecto actual** (una foto más reciente por proyecto, reemplazable) y no se sincroniza. Las notificaciones dependen del permiso del sistema y pueden estar deshabilitadas por el usuario. El proyecto requiere compilación y prueba real en Android Studio antes de declarar cumplimiento total.
