# Space Explorer

[![CI](https://github.com/Charles006/space-explorer/actions/workflows/android.yml/badge.svg)](https://github.com/Charles006/space-explorer/actions/workflows/android.yml)
[![codecov](https://codecov.io/gh/Charles006/space-explorer/branch/main/graph/badge.svg)](https://codecov.io/gh/Charles006/space-explorer)

Android app (Jetpack Compose) que lista las imagenes y videos de NASA APOD,
permite buscar por fecha, marcar favoritos y reproducir los videos dentro de
la app.

## Stack

- Kotlin 2.0, Compose BOM 2024.10, Material 3
- Hilt, Retrofit + Moshi, Room, Coil
- Coroutines / Flow
- Tests: JUnit4, Mockito-Kotlin, Truth, Turbine, Robolectric, MockWebServer

## Correr local

Requisitos: JDK 17, Android SDK API 35, minSdk 24.

```
git clone <repo>
cd 2026-05-23_space_explorer
```

Build y run:

```
./gradlew assembleDebug
./gradlew installDebug
```

El repo trae `local.properties` con `NASA_API_KEY=DEMO_KEY` para que clonar y
correr funcione sin setup. Para subir el rate limit, pega tu propia key
gratuita de https://api.nasa.gov sobre esa linea.

Si usas Gradle desde la terminal sin Android Studio abierto, exporta
`ANDROID_HOME` apuntando al SDK. Si abres el proyecto en Android Studio,
el IDE agrega `sdk.dir` al `local.properties` solo en el primer sync.

## Tests

```
./gradlew testDebugUnitTest
```

Los tests instrumentados de Compose corren en JVM via Robolectric, no
necesitan emulador.

## Arquitectura

Tres capas:

- `domain/` modelos inmutables y la interfaz `AstronomyRepository`. Sin
  dependencias de Android.
- `data/` implementacion Retrofit + Room. La unica capa que conoce errores
  HTTP; el mapper a `AstronomyError` esta en `data/network/ApiErrorMapper`.
- `ui/` Compose, ViewModels (StateFlow) y NavHost.

Los favoritos se persisten en Room. La columna `videoUrl` se agrego en la
v2 del schema (ver `MIGRATION_1_2` en `SpaceExplorerDatabase`).

Los videos NASA son embeds YouTube/Vimeo. Se renderizan en un `WebView`
con un iframe; `EmbeddedVideoPlayer` normaliza la URL a
`youtube-nocookie.com/embed/<id>` antes de inyectarla.

## Notas

- Paginacion custom de 10 dias por pagina hacia atras. NASA APOD no expone
  cursor, solo rango de fechas.
- `local.properties` y `.idea/` estan en `.gitignore`.
- CI: `.github/workflows/android.yml` corre tests + assembleDebug en cada push.
